package com.watsoo.addressconverter.geocode

data class GeocodeResult(
    val latitude: Double,
    val longitude: Double,
    val normalizedAddress: String?
)

interface GeocoderClient {
    suspend fun geocode(address: String): GeocodeResult
}
