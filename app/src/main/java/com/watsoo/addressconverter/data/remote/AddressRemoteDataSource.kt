package com.watsoo.addressconverter.data.remote

/**
 * Interface defining operations for interacting with a remote address service.
 */
interface AddressRemoteDataSource {
    
    /**
     * Fetches a page of addresses from the server.
     * @param limit Maximum number of addresses to return.
     * @param cursor Pagination cursor (optional).
     */
    suspend fun fetchAddresses(limit: Int, cursor: String? = null): ApiResult<FetchAddressesResponseDto>

    /**
     * Uploads a batch of geocoded addresses.
     * @param request The batch request containing items and an idempotency key (batchId).
     */
    suspend fun uploadGeocodedBatch(request: UploadGeocodedBatchRequestDto): ApiResult<UploadGeocodedBatchResponseDto>
}
