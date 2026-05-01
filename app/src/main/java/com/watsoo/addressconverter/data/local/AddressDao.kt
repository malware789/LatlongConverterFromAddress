package com.watsoo.addressconverter.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface AddressDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAddresses(addresses: List<AddressEntity>)

    @Query("SELECT * FROM addresses ORDER BY createdAt DESC")
    fun getAllAddressesFlow(): kotlinx.coroutines.flow.Flow<List<AddressEntity>>

    // get next 100 pending addresses
    @Query("""
        SELECT * FROM addresses 
        WHERE status = 'PENDING_GEOCODE' 
        AND nextRetryAt <= :currentTime
        ORDER BY createdAt ASC 
        LIMIT :limit
    """)
    suspend fun getPendingAddresses(limit: Int = 100, currentTime: Long = System.currentTimeMillis()): List<AddressEntity>

    // mark rows as GEOCODING
    @Query("""
        UPDATE addresses 
        SET status = 'GEOCODING', lockedAt = :lockedAt, updatedAt = :currentTime
        WHERE localId IN (:ids)
    """)
    suspend fun markAsGeocoding(ids: List<Long>, lockedAt: Long = System.currentTimeMillis(), currentTime: Long = System.currentTimeMillis())

    // mark one address as successfully geocoded
    @Query("""
        UPDATE addresses 
        SET status = 'GEOCODED_PENDING_UPLOAD', 
            latitude = :lat, 
            longitude = :lng, 
            normalizedAddress = :normalizedAddress,
            updatedAt = :currentTime
        WHERE localId = :id
    """)
    suspend fun markAsGeocoded(id: Long, lat: Double, lng: Double, normalizedAddress: String?, currentTime: Long = System.currentTimeMillis())

    // mark one address as failed temporarily
    @Query("""
        UPDATE addresses 
        SET status = 'FAILED_TEMP', 
            geocodeAttempts = geocodeAttempts + 1,
            nextRetryAt = :nextRetryAt,
            lastError = :error,
            updatedAt = :currentTime
        WHERE localId = :id
    """)
    suspend fun markAsFailedTemp(id: Long, error: String?, nextRetryAt: Long, currentTime: Long = System.currentTimeMillis())

    // mark one address as failed permanently
    @Query("""
        UPDATE addresses 
        SET status = 'FAILED_PERM', 
            geocodeAttempts = geocodeAttempts + 1,
            lastError = :error,
            updatedAt = :currentTime
        WHERE localId = :id
    """)
    suspend fun markAsFailedPerm(id: Long, error: String?, currentTime: Long = System.currentTimeMillis())

    // get next 100 geocoded rows pending upload
    @Query("""
        SELECT * FROM addresses 
        WHERE status = 'GEOCODED_PENDING_UPLOAD' 
        AND nextRetryAt <= :currentTime
        ORDER BY updatedAt ASC 
        LIMIT :limit
    """)
    suspend fun getPendingUploadAddresses(limit: Int = 100, currentTime: Long = System.currentTimeMillis()): List<AddressEntity>

    // mark rows as UPLOADING
    @Query("""
        UPDATE addresses 
        SET status = 'UPLOADING', lockedAt = :lockedAt, updatedAt = :currentTime
        WHERE localId IN (:ids)
    """)
    suspend fun markAsUploading(ids: List<Long>, lockedAt: Long = System.currentTimeMillis(), currentTime: Long = System.currentTimeMillis())

    // mark uploaded rows as SENT
    @Query("""
        UPDATE addresses 
        SET status = 'SENT',
            updatedAt = :currentTime
        WHERE localId IN (:ids)
    """)
    suspend fun markAsSent(ids: List<Long>, currentTime: Long = System.currentTimeMillis())

    // mark upload as failed temporarily
    @Query("""
        UPDATE addresses 
        SET status = 'GEOCODED_PENDING_UPLOAD', 
            uploadAttempts = uploadAttempts + 1,
            nextRetryAt = :nextRetryAt,
            lastError = :error,
            lockedAt = 0,
            updatedAt = :currentTime
        WHERE localId IN (:ids)
    """)
    suspend fun markAsUploadFailedTemp(ids: List<Long>, error: String?, nextRetryAt: Long, currentTime: Long = System.currentTimeMillis())

    // mark upload as failed permanently
    @Query("""
        UPDATE addresses 
        SET status = 'FAILED_PERM', 
            uploadAttempts = uploadAttempts + 1,
            lastError = :error,
            lockedAt = 0,
            updatedAt = :currentTime
        WHERE localId IN (:ids)
    """)
    suspend fun markAsUploadFailedPerm(ids: List<Long>, error: String?, currentTime: Long = System.currentTimeMillis())

    // release stale GEOCODING rows back to PENDING_GEOCODE
    @Query("""
        UPDATE addresses 
        SET status = 'PENDING_GEOCODE', lockedAt = 0, updatedAt = :currentTime
        WHERE status = 'GEOCODING' AND lockedAt < :staleThreshold
    """)
    suspend fun releaseStaleGeocodingLockedRows(staleThreshold: Long, currentTime: Long = System.currentTimeMillis()): Int

    // release stale UPLOADING rows back to GEOCODED_PENDING_UPLOAD
    @Query("""
        UPDATE addresses 
        SET status = 'GEOCODED_PENDING_UPLOAD', lockedAt = 0, updatedAt = :currentTime
        WHERE status = 'UPLOADING' AND lockedAt < :staleThreshold
    """)
    suspend fun releaseStaleUploadingLockedRows(staleThreshold: Long, currentTime: Long = System.currentTimeMillis()): Int

    // count by status for UI progress
    @Query("SELECT COUNT(*) FROM addresses WHERE status = :status")
    suspend fun countByStatus(status: String): Int
    
    // total count
    @Query("SELECT COUNT(*) FROM addresses")
    suspend fun countTotal(): Int

    // reset database
    @Query("DELETE FROM addresses")
    suspend fun deleteAll()

    // get addresses filtered by status (for list screen)
    @Query("SELECT * FROM addresses WHERE status = :status ORDER BY updatedAt DESC LIMIT :limit OFFSET :offset")
    suspend fun getByStatus(status: String, limit: Int = 200, offset: Int = 0): List<AddressEntity>

    // get ALL addresses (for "All" filter)
    @Query("SELECT * FROM addresses ORDER BY updatedAt DESC LIMIT :limit OFFSET :offset")
    suspend fun getAll(limit: Int = 200, offset: Int = 0): List<AddressEntity>

    // reset FAILED_TEMP → PENDING_GEOCODE immediately
    @Query("""
        UPDATE addresses
        SET status = 'PENDING_GEOCODE',
            nextRetryAt = 0,
            lockedAt = 0,
            lastError = NULL,
            updatedAt = :currentTime
        WHERE status = 'FAILED_TEMP'
    """)
    suspend fun retryTemporaryFailures(currentTime: Long = System.currentTimeMillis()): Int

    // reset FAILED_PERM → PENDING_GEOCODE (used intentionally after confirmation)
    @Query("""
        UPDATE addresses
        SET status = 'PENDING_GEOCODE',
            geocodeAttempts = 0,
            uploadAttempts = 0,
            nextRetryAt = 0,
            lockedAt = 0,
            lastError = NULL,
            updatedAt = :currentTime
        WHERE status = 'FAILED_PERM'
    """)
    suspend fun retryPermanentFailures(currentTime: Long = System.currentTimeMillis()): Int

    // delete FAILED_PERM rows entirely
    @Query("DELETE FROM addresses WHERE status = 'FAILED_PERM'")
    suspend fun clearPermanentFailures(): Int
}
