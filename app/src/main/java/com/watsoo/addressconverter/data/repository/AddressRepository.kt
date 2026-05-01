package com.watsoo.addressconverter.data.repository

import android.util.Log

import com.watsoo.addressconverter.config.AppConfig
import com.watsoo.addressconverter.data.local.AddressDao
import com.watsoo.addressconverter.data.local.AddressEntity
import com.watsoo.addressconverter.data.local.AddressStatus
import com.watsoo.addressconverter.data.local.WorkerLogger
import com.watsoo.addressconverter.data.mapper.toEntity
import com.watsoo.addressconverter.data.mapper.toUploadItemDto
import com.watsoo.addressconverter.data.remote.AddressRemoteDataSource
import com.watsoo.addressconverter.data.remote.ApiResult
import com.watsoo.addressconverter.data.remote.UploadGeocodedBatchRequestDto
import com.watsoo.addressconverter.geocode.GeocoderClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import java.io.IOException
import java.util.UUID
import kotlin.math.min
import kotlin.math.pow
import kotlin.random.Random

class AddressRepository(
    private val addressDao: AddressDao,
    private val remoteDataSource: AddressRemoteDataSource,
    private val geocoderClient: GeocoderClient
) {

    private val geocodeSemaphore = Semaphore(AppConfig.GEOCODING_CONCURRENCY)

    // ──────────────────────────────────────────────
    //  Fetch
    // ──────────────────────────────────────────────

    suspend fun fetchAndSaveAddresses() = withContext(Dispatchers.IO) {
        WorkerLogger.info("Fetching server addresses started (Batch size: ${AppConfig.FETCH_BATCH_SIZE})…")
        
        var currentCursor: String? = null
        var hasMore = true
        var totalFetched = 0

        while (hasMore && isActive) {
            val result = remoteDataSource.fetchAddresses(AppConfig.FETCH_BATCH_SIZE, currentCursor)
            
            when (result) {
                is ApiResult.Success -> {
                    val dto = result.data
                    val entities = dto.addresses.map { it.toEntity() }
                    addressDao.insertAddresses(entities)
                    
                    totalFetched += entities.size
                    currentCursor = dto.nextCursor
                    hasMore = dto.hasMore && currentCursor != null
                    
                    if (entities.isNotEmpty()) {
                        WorkerLogger.info("Fetched page: ${entities.size} items. Total so far: $totalFetched")
                    }
                }
                is ApiResult.NetworkError -> {
                    WorkerLogger.warning("Fetch network error: ${result.exception.message}. Retrying later.")
                    hasMore = false
                }
                is ApiResult.HttpError -> {
                    WorkerLogger.error("Fetch HTTP error [${result.code}]: ${result.message}")
                    hasMore = false
                }
                is ApiResult.RateLimited -> {
                    val wait = result.retryAfterSeconds ?: 10
                    WorkerLogger.warning("Fetch rate limited. Waiting ${wait}s…")
                    delay(wait * 1000L)
                }
                is ApiResult.Unauthorized -> {
                    WorkerLogger.error("Fetch failed: Unauthorized (401). Authentication required.")
                    hasMore = false
                }
                is ApiResult.UnknownError -> {
                    WorkerLogger.error("Fetch failed: Unknown error.")
                    hasMore = false
                }
            }
        }
        WorkerLogger.info("Fetch process finished. Total items synced: $totalFetched")
        Unit
    }

    suspend fun releaseAllStaleLocks() = withContext(Dispatchers.IO) {
        Log.d("AddressRepository", "releaseAllStaleLocks started")
        val staleThreshold = System.currentTimeMillis() - AppConfig.STALE_LOCK_TIMEOUT_MS
        val releasedGeo = addressDao.releaseStaleGeocodingLockedRows(staleThreshold)
        val releasedUp = addressDao.releaseStaleUploadingLockedRows(staleThreshold)
        if (releasedGeo > 0) WorkerLogger.warning("Released $releasedGeo stale GEOCODING locks")
        if (releasedUp > 0) WorkerLogger.warning("Released $releasedUp stale UPLOADING locks")
        Log.d("AddressRepository", "releaseAllStaleLocks completed")
        Unit
    }

    suspend fun hasPendingWork(): Boolean = withContext(Dispatchers.IO) {
        val pendingCount = addressDao.countByStatus(AddressStatus.PENDING_GEOCODE.name)
        val uploadableCount = addressDao.countByStatus(AddressStatus.GEOCODED_PENDING_UPLOAD.name)
        return@withContext pendingCount > 0 || uploadableCount > 0
    }

    // ──────────────────────────────────────────────
    //  Geocoding batch (Unchanged logic, just ensure imports)
    // ──────────────────────────────────────────────

    suspend fun processNextBatch(): Boolean = withContext(Dispatchers.IO) {
        Log.d("AddressRepository", "processNextBatch started")
        val staleThreshold = System.currentTimeMillis() - AppConfig.STALE_LOCK_TIMEOUT_MS
        val releasedGeo = addressDao.releaseStaleGeocodingLockedRows(staleThreshold)
        if (releasedGeo > 0) WorkerLogger.warning("Released $releasedGeo stale GEOCODING locks")

        val pending = addressDao.getPendingAddresses(limit = AppConfig.PROCESS_BATCH_SIZE)
        if (pending.isEmpty()) return@withContext false

        WorkerLogger.info("Batch geocoding started – ${pending.size} addresses")
        val ids = pending.map { it.localId }
        addressDao.markAsGeocoding(ids)

        pending.map { entity ->
            async {
                if (!isActive) return@async
                geocodeSemaphore.withPermit {
                    try {
                        val result = geocoderClient.geocode(entity.address)
                        addressDao.markAsGeocoded(
                            id = entity.localId,
                            lat = result.latitude,
                            lng = result.longitude,
                            normalizedAddress = result.normalizedAddress
                        )
                    } catch (e: IOException) {
                        WorkerLogger.warning("Temp geocode failure [${entity.serverId}]: ${e.message}")
                        if (entity.geocodeAttempts >= AppConfig.MAX_GEOCODE_ATTEMPTS) {
                            WorkerLogger.error("Perm geocode failure [${entity.serverId}]: max retries reached")
                            addressDao.markAsFailedPerm(entity.localId, "Max retries: ${e.message}")
                        } else {
                            val backoff = calculateBackoff(entity.geocodeAttempts)
                            addressDao.markAsFailedTemp(entity.localId, e.message, System.currentTimeMillis() + backoff)
                        }
                    } catch (e: Exception) {
                        WorkerLogger.error("Perm geocode failure [${entity.serverId}]: ${e.message}")
                        addressDao.markAsFailedPerm(entity.localId, e.message)
                    }
                }
            }
        }.awaitAll()

        WorkerLogger.info("Batch geocoding complete – ${pending.size} processed")
        Log.d("AddressRepository", "processNextBatch completed")
        return@withContext true
    }

    // ──────────────────────────────────────────────
    //  Upload batch
    // ──────────────────────────────────────────────

    suspend fun uploadCompletedBatch(): Boolean = withContext(Dispatchers.IO) {
        Log.d("AddressRepository", "uploadCompletedBatch started")
        val staleThreshold = System.currentTimeMillis() - AppConfig.STALE_LOCK_TIMEOUT_MS
        val releasedUp = addressDao.releaseStaleUploadingLockedRows(staleThreshold)
        if (releasedUp > 0) WorkerLogger.warning("Released $releasedUp stale UPLOADING locks")

        val readyToUpload = addressDao.getPendingUploadAddresses(limit = AppConfig.PROCESS_BATCH_SIZE)
        if (readyToUpload.isEmpty()) return@withContext false

        WorkerLogger.info("Upload started – ${readyToUpload.size} addresses")
        val ids = readyToUpload.map { it.localId }
        addressDao.markAsUploading(ids)

        val batchRequest = UploadGeocodedBatchRequestDto(
            batchId = UUID.randomUUID().toString(), // Idempotency key
            items = readyToUpload.map { it.toUploadItemDto() }
        )

        val result = remoteDataSource.uploadGeocodedBatch(batchRequest)
        
        val success = when (result) {
            is ApiResult.Success -> {
                addressDao.markAsSent(ids)
                WorkerLogger.info("Upload success – ${result.data.acceptedCount} items accepted. Message: ${result.data.message}")
                true
            }
            is ApiResult.RateLimited -> {
                val wait = result.retryAfterSeconds ?: 15
                WorkerLogger.warning("Upload rate limited. Waiting ${wait}s…")
                handleUploadFailure(readyToUpload, "Rate limited (429)")
                delay(wait * 1000L)
                false
            }
            is ApiResult.NetworkError -> {
                WorkerLogger.warning("Upload network error: ${result.exception.message}")
                handleUploadFailure(readyToUpload, result.exception.message)
                false
            }
            is ApiResult.HttpError -> {
                WorkerLogger.error("Upload HTTP error [${result.code}]: ${result.message}")
                if (result.code in 400..499) {
                    addressDao.markAsUploadFailedPerm(ids, "HTTP ${result.code}: ${result.message}")
                } else {
                    handleUploadFailure(readyToUpload, "Server Error ${result.code}")
                }
                false
            }
            is ApiResult.Unauthorized -> {
                WorkerLogger.error("Upload failed: Unauthorized (401). Authentication required.")
                handleUploadFailure(readyToUpload, "Unauthorized")
                false
            }
            is ApiResult.UnknownError -> {
                WorkerLogger.error("Upload failed: Unknown error.")
                handleUploadFailure(readyToUpload, "Unknown error")
                false
            }
        }
        Log.d("AddressRepository", "uploadCompletedBatch completed: $success")
        return@withContext success
    }

    private suspend fun handleUploadFailure(items: List<AddressEntity>, error: String?) {
        val tempFail = mutableListOf<Long>()
        val permFail = mutableListOf<Long>()
        for (item in items) {
            if (item.uploadAttempts >= AppConfig.MAX_UPLOAD_ATTEMPTS) permFail.add(item.localId)
            else tempFail.add(item.localId)
        }
        if (tempFail.isNotEmpty()) {
            val backoff = calculateBackoff(items.first().uploadAttempts)
            addressDao.markAsUploadFailedTemp(tempFail, error, System.currentTimeMillis() + backoff)
        }
        if (permFail.isNotEmpty()) {
            WorkerLogger.error("${permFail.size} upload(s) reached max attempts – FAILED_PERM")
            addressDao.markAsUploadFailedPerm(permFail, "Max retries: $error")
        }
    }

    // ──────────────────────────────────────────────
    //  Retry actions
    // ──────────────────────────────────────────────

    suspend fun retryTemporaryFailuresNow(): Int = withContext(Dispatchers.IO) {
        val count = addressDao.retryTemporaryFailures()
        WorkerLogger.info("Retry triggered – $count FAILED_TEMP rows reset to PENDING_GEOCODE")
        count
    }

    suspend fun retryPermanentFailures(): Int = withContext(Dispatchers.IO) {
        val count = addressDao.retryPermanentFailures()
        WorkerLogger.warning("Force-retry – $count FAILED_PERM rows reset to PENDING_GEOCODE (attempts cleared)")
        count
    }

    suspend fun clearPermanentFailures(): Int = withContext(Dispatchers.IO) {
        val count = addressDao.clearPermanentFailures()
        WorkerLogger.warning("Deleted $count FAILED_PERM rows")
        count
    }

    // ──────────────────────────────────────────────
    //  Data queries
    // ──────────────────────────────────────────────

    suspend fun getAddressesByStatus(status: AddressStatus?, limit: Int = 200): List<AddressEntity> =
        withContext(Dispatchers.IO) {
            if (status == null) addressDao.getAll(limit) else addressDao.getByStatus(status.name, limit)
        }

    suspend fun getCountTotal(): Int = addressDao.countTotal()
    suspend fun getCountByStatus(status: AddressStatus): Int = addressDao.countByStatus(status.name)

    suspend fun resetLocalData() = withContext(Dispatchers.IO) {
        addressDao.deleteAll()
        WorkerLogger.info("Local data reset – all addresses deleted")
    }

    // ──────────────────────────────────────────────
    //  Backoff
    // ──────────────────────────────────────────────

    private fun calculateBackoff(attempts: Int): Long {
        val delay = kotlin.math.min(
            AppConfig.BACKOFF_BASE_MS * (2.0.pow(attempts.toDouble()).toLong()),
            AppConfig.BACKOFF_MAX_MS
        )
        val jitter = Random.nextLong((delay * AppConfig.BACKOFF_JITTER_RATIO).toLong().coerceAtLeast(1L))
        return delay + jitter
    }

    /**
     * Imports addresses from a raw string.
     * Expected format per line: "ID | Address" or just "Address".
     */
    suspend fun importManualAddresses(rawInput: String) = withContext(Dispatchers.IO) {
        val lines = rawInput.lines()
            .map { it.trim() }
            .filter { it.isNotBlank() }

        val entities = lines.map { line ->
            val parts = line.split("|", limit = 2).map { it.trim() }
            val (serverId, address) = if (parts.size == 2) {
                parts[0] to parts[1]
            } else {
                "MANUAL_${System.currentTimeMillis()}_${line.hashCode()}" to line
            }

            AddressEntity(
                serverId = serverId,
                address = address,
                normalizedAddress = null,
                latitude = null,
                longitude = null,
                status = AddressStatus.PENDING_GEOCODE.name
            )
        }

        if (entities.isNotEmpty()) {
            addressDao.insertAddresses(entities)
            WorkerLogger.info("Imported ${entities.size} manual addresses")
        }
    }
}
