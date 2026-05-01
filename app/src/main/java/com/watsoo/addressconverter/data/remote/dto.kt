package com.watsoo.addressconverter.data.remote

/**
 * FETCH ADDRESSES
 */
data class FetchAddressesResponseDto(
    val addresses: List<AddressDto>,
    val nextCursor: String?,
    val hasMore: Boolean
)

data class AddressDto(
    val serverId: String,
    val address: String
)

/**
 * UPLOAD GEOCODED BATCH
 */
data class UploadGeocodedBatchRequestDto(
    val batchId: String, // Can be used as Idempotency-Key
    val items: List<UploadGeocodedItemDto>
)

data class UploadGeocodedItemDto(
    val id: String, // Server ID
    val address: String,
    val latitude: Double?,
    val longitude: Double?,
    val geocodeStatus: String, // e.g., "SUCCESS", "FAILED_PERM"
    val errorMessage: String?
)

data class UploadGeocodedBatchResponseDto(
    val success: Boolean,
    val acceptedCount: Int,
    val message: String?,
    val failedItems: List<String>? = null // List of IDs that failed specifically
)

/**
 * ERROR RESPONSE
 */
data class ErrorResponseDto(
    val error: String,
    val message: String
)
