package com.watsoo.addressconverter.config

object AppConfig {

    // ------ Environment ------
    const val USE_FAKE_API = true
    const val BASE_URL = "https://api.address-converter.example.com/" // Placeholder
    const val API_TIMEOUT_SECONDS = 30L

    // ------ Fetch ------
    const val FETCH_BATCH_SIZE = 100 // Set smaller for pagination testing

    // ------ Processing ------
    const val PROCESS_BATCH_SIZE = 100
    const val MAX_BATCHES_PER_WORKER_RUN = 10
    const val GEOCODING_CONCURRENCY = 3

    // ------ Retry limits ------
    const val MAX_GEOCODE_ATTEMPTS = 5
    const val MAX_UPLOAD_ATTEMPTS = 5

    // ------ Backoff ------
    const val BACKOFF_BASE_MS = 5_000L        // 5 seconds
    const val BACKOFF_MAX_MS = 3_600_000L     // 1 hour
    const val BACKOFF_JITTER_RATIO = 0.2      // 20% jitter

    // ------ Stale lock timeout ------
    const val STALE_LOCK_TIMEOUT_MS = 10 * 60 * 1_000L // 10 minutes

    // ------ Fake failure rates (mutable for testing) ------
    var FAKE_GEOCODER_TEMP_FAILURE_PERCENT = 5.0f // 5% chance of network error
    var FAKE_GEOCODER_PERM_FAILURE_PERCENT = 1.0f // 1% chance of "Zero Results"
    
    // ── Geocoder Settings ──────────────────────────────────────────

    enum class GeocoderMode {
        FAKE,
        ANDROID_GEOCODER
    }
    
    /**
     * Set to [GeocoderMode.ANDROID_GEOCODER] to use the device's system geocoder.
     * This will return REAL coordinates but depends on system availability and network.
     */
    var GEOCODER_MODE = /*GeocoderMode.FAKE*/  GeocoderMode.ANDROID_GEOCODER

    var FAKE_UPLOAD_FAILURE_PERCENT = 0.15f
    var FAKE_FETCH_FAILURE_PERCENT = 0.10f

    // ------ UI polling ------
    const val UI_POLL_INTERVAL_MS = 2_000L
}
