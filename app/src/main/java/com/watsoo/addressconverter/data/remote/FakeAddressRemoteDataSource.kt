package com.watsoo.addressconverter.data.remote

import com.watsoo.addressconverter.config.AppConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.IOException
import java.util.Collections
import kotlin.random.Random

/**
 * Fake implementation of AddressRemoteDataSource for testing and development.
 */
class FakeAddressRemoteDataSource : AddressRemoteDataSource {

    private val uploadedBatchIds = Collections.synchronizedSet(mutableSetOf<String>())

    override suspend fun fetchAddresses(limit: Int, cursor: String?): ApiResult<FetchAddressesResponseDto> = withContext(Dispatchers.IO) {
        delay(1000)
        
        if (Random.nextFloat() < AppConfig.FAKE_FETCH_FAILURE_PERCENT) {
            return@withContext ApiResult.NetworkError(IOException("Simulated fetch network error"))
        }

        val startOffset = cursor?.toIntOrNull() ?: 0
        val addresses = (1..limit).map { i ->
            val id = startOffset + i
            AddressDto(
                serverId = "server_id_$id",
                address = "Fake Address Line $id, City ${id % 100}, Country"
            )
        }

        // Simulate having a total of 1000 items
        val hasMore = (startOffset + limit) < 1000
        val nextCursor = if (hasMore) (startOffset + limit).toString() else null

        ApiResult.Success(
            FetchAddressesResponseDto(
                addresses = addresses,
                nextCursor = nextCursor,
                hasMore = hasMore
            )
        )
    }

    override suspend fun uploadGeocodedBatch(request: UploadGeocodedBatchRequestDto): ApiResult<UploadGeocodedBatchResponseDto> = withContext(Dispatchers.IO) {
        delay(800)

        // Check idempotency
        if (uploadedBatchIds.contains(request.batchId)) {
            return@withContext ApiResult.Success(
                UploadGeocodedBatchResponseDto(
                    success = true,
                    acceptedCount = 0, // Already accepted previously
                    message = "Batch already processed (Idempotent)"
                )
            )
        }

        if (Random.nextFloat() < AppConfig.FAKE_UPLOAD_FAILURE_PERCENT) {
            return@withContext ApiResult.NetworkError(IOException("Simulated upload network error"))
        }

        // Simulate success
        uploadedBatchIds.add(request.batchId)
        
        ApiResult.Success(
            UploadGeocodedBatchResponseDto(
                success = true,
                acceptedCount = request.items.size,
                message = "Successfully uploaded ${request.items.size} items."
            )
        )
    }
}
