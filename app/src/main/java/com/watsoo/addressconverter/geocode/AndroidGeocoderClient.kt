package com.watsoo.addressconverter.geocode

import android.content.Context
import android.location.Geocoder
import android.os.Build
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import java.io.IOException

/**
 * Real implementation using Android's system [Geocoder].
 * Provides real coordinates but requires the device to have a geocoding service available.
 */
class AndroidGeocoderClient(private val context: Context) : GeocoderClient {

    override suspend fun geocode(address: String): GeocodeResult = withContext(Dispatchers.IO) {
        if (!Geocoder.isPresent()) {
            throw IOException("Geocoder service is not available on this device")
        }

        val geocoder = Geocoder(context)

        withTimeout(15_000L) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                val deferred = CompletableDeferred<GeocodeResult>()
                
                geocoder.getFromLocationName(address, 1, object : Geocoder.GeocodeListener {
                    override fun onGeocode(addresses: MutableList<android.location.Address>) {
                        if (addresses.isEmpty()) {
                            deferred.completeExceptionally(IllegalArgumentException("No results found for: $address"))
                        } else {
                            val addr = addresses[0]
                            deferred.complete(
                                GeocodeResult(
                                    latitude = addr.latitude,
                                    longitude = addr.longitude,
                                    normalizedAddress = addr.getAddressLine(0) ?: address
                                )
                            )
                        }
                    }

                    override fun onError(errorMessage: String?) {
                        deferred.completeExceptionally(IOException(errorMessage ?: "Geocoder error"))
                    }
                })
                
                deferred.await()
            } else {
                // Blocking API for older versions
                @Suppress("DEPRECATION")
                val results = try {
                    geocoder.getFromLocationName(address, 1)
                } catch (e: Exception) {
                    throw IOException("Network or Service error in Geocoder: ${e.message}", e)
                }

                if (results == null || results.isEmpty()) {
                    throw IllegalArgumentException("No results found for: $address")
                }

                val addr = results[0]
                GeocodeResult(
                    latitude = addr.latitude,
                    longitude = addr.longitude,
                    normalizedAddress = addr.getAddressLine(0) ?: address
                )
            }
        }
    }
}
