package com.watsoo.addressconverter.data.remote

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Query

/**
 * Retrofit interface for the Address Converter Backend.
 */
interface AddressApi {

    /**
     * Fetch addresses with pagination.
     * TODO: Update "api/v1/addresses" with the actual path once provided by backend.
     */
    @GET("api/v1/addresses")
    suspend fun getAddresses(
        @Query("limit") limit: Int,
        @Query("cursor") cursor: String? = null
    ): Response<FetchAddressesResponseDto>

    /**
     * Upload a batch of geocoded addresses.
     * Uses Idempotency-Key to prevent duplicate processing on the server.
     * TODO: Update "api/v1/addresses/upload" with the actual path.
     */
    @POST("api/v1/addresses/upload")
    suspend fun uploadGeocodedBatch(
        @Header("Idempotency-Key") idempotencyKey: String,
        @Body request: UploadGeocodedBatchRequestDto
    ): Response<UploadGeocodedBatchResponseDto>

    // TODO: Add Authorization header or interceptor for authentication when token logic is ready.
}
