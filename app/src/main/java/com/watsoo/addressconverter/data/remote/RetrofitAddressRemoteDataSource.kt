package com.watsoo.addressconverter.data.remote

import com.google.gson.Gson
import retrofit2.Response
import java.io.IOException

/**
 * Real implementation of AddressRemoteDataSource using Retrofit.
 */
class RetrofitAddressRemoteDataSource(
    private val api: AddressApi
) : AddressRemoteDataSource {

    override suspend fun fetchAddresses(limit: Int, cursor: String?): ApiResult<FetchAddressesResponseDto> {
        return try {
            val response = api.getAddresses(limit, cursor)
            handleResponse(response)
        } catch (e: Exception) {
            handleException(e)
        }
    }

    override suspend fun uploadGeocodedBatch(request: UploadGeocodedBatchRequestDto): ApiResult<UploadGeocodedBatchResponseDto> {
        return try {
            val response = api.uploadGeocodedBatch(request.batchId, request)
            handleResponse(response)
        } catch (e: Exception) {
            handleException(e)
        }
    }

    private fun <T> handleResponse(response: Response<T>): ApiResult<T> {
        val body = response.body()
        return when {
            response.isSuccessful && body != null -> ApiResult.Success(body)
            response.code() == 429 -> {
                val retryAfter = response.headers()["Retry-After"]?.toIntOrNull()
                ApiResult.RateLimited(retryAfter)
            }
            response.code() == 401 -> ApiResult.Unauthorized
            else -> {
                val errorBody = response.errorBody()?.string()
                ApiResult.HttpError(response.code(), response.message(), errorBody)
            }
        }
    }

    private fun <T> handleException(e: Exception): ApiResult<T> {
        return when (e) {
            is IOException -> ApiResult.NetworkError(e)
            else -> ApiResult.UnknownError(e)
        }
    }
}
