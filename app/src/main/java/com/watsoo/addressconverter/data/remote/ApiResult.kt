package com.watsoo.addressconverter.data.remote

/**
 * A sealed class to represent the result of an API call.
 * This helps in handling network errors, HTTP errors, and success states uniformly.
 */
sealed class ApiResult<out T> {
    data class Success<out T>(val data: T) : ApiResult<T>()
    
    // Network errors (no internet, timeout, etc.)
    data class NetworkError(val exception: Exception) : ApiResult<Nothing>()
    
    // HTTP errors (4xx, 5xx)
    data class HttpError(val code: Int, val message: String?, val errorBody: String? = null) : ApiResult<Nothing>()
    
    // Specifically for 429
    data class RateLimited(val retryAfterSeconds: Int?) : ApiResult<Nothing>()
    
    // Specifically for 401
    data object Unauthorized : ApiResult<Nothing>()
    
    // Fallback
    data class UnknownError(val exception: Throwable) : ApiResult<Nothing>()
}
