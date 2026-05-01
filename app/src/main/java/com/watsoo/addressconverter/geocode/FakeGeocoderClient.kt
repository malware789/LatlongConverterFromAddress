package com.watsoo.addressconverter.geocode

import com.watsoo.addressconverter.config.AppConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.IOException
import kotlin.random.Random

/**
 * A fake implementation of [GeocoderClient] for testing and development.
 * WARNING: This implementation returns SIMULATED coordinates based on address hash.
 * It does NOT use a real geocoding service and is intended only for pipeline validation.
 */
class FakeGeocoderClient : GeocoderClient {

    override suspend fun geocode(address: String): GeocodeResult = withContext(Dispatchers.IO) {
        delay(Random.nextLong(100, 300))
        val rand = Random.nextFloat() * 100f // Convert to 0-100 range

        if (rand < AppConfig.FAKE_GEOCODER_TEMP_FAILURE_PERCENT) {
            throw IOException("Temporary geocoding network error (Fake)")
        }
        if (rand < AppConfig.FAKE_GEOCODER_TEMP_FAILURE_PERCENT + AppConfig.FAKE_GEOCODER_PERM_FAILURE_PERCENT) {
            throw IllegalArgumentException("Fake: Address not found (Permanent failure)")
        }

        val hash = address.hashCode()
        val lat = (hash % 180_000) / 1000.0 - 90.0
        val lng = (hash % 360_000) / 1000.0 - 180.0

        GeocodeResult(
            latitude = lat,
            longitude = lng,
            normalizedAddress = "Normalized: $address"
        )
    }
}
