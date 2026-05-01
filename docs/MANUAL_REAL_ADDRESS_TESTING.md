# Manual Real Address Testing

This guide explains how to perform manual end-to-end verification of the geocoding pipeline using real addresses and the device's system geocoder.

## Overview
The app supports two geocoding modes:
1.  **`FAKE`**: Returns simulated coordinates instantly. Best for testing UI, Room, and WorkManager logic without network dependency.
2.  **`ANDROID_GEOCODER`**: Uses the Android system `Geocoder` service. Returns real latitude/longitude for real addresses.

## Prerequisites
- **Real Device or Emulator**: Must have Google Play Services and internet access for `AndroidGeocoderClient` to work.
- **Permission**: Ensure notification permission is granted to see background progress.

## Test Steps
1.  **Switch Mode**: In `AppConfig.kt`, set `GEOCODER_MODE = GeocoderMode.ANDROID_GEOCODER`.
2.  **Import Addresses**:
    - Open `docs/manual_test_addresses_100.txt`.
    - Copy the contents.
    - Click **"Import"** on the app dashboard and paste the list.
3.  **Start Conversion**: Click **"Start Work"**.
4.  **Monitor**:
    - Watch the dashboard counters.
    - Check the `worker_logs` for "Geocoding success" or "Temp failure" entries.
    - Open the **Address List** to see the actual latitude and longitude assigned to landmarks.

## Verification Status (2026-05-01)
- **Manual Pasted Data**: ✅ PASSED. The app correctly ignores the fetch phase when local data exists and processes the imported list.
- **Geocoding Results**: ✅ PASSED. Landmarks from the 100-address list were accurately converted to lat/lng.
- **Worker Stability**: ✅ PASSED. Following the HiltWorker instantiation fix, the worker starts reliably every time.
- **Pipeline Completion**: ✅ PASSED. Rows successfully reached the `SENT` status after the simulated upload phase.

## Offline/Manual Resilience
The app is now optimized to process manually imported rows even without a backend API.
- If you import addresses and press "Start Work", the app will detect the local work and **skip the fetch phase**.
- Even if the fetch phase fails (e.g., due to a simulated 10% network failure), the app will continue to geocode and upload your local rows.
- This ensures that manual QA is never blocked by "Simulated fetch network errors".

## ⚠️ Important Warnings
- **Bulk Processing Limit**: The `AndroidGeocoderClient` uses the free system service which is **not intended for bulk 50k+ conversions**. It may be rate-limited or return "Service Unavailable" if abused.
- **Production Recommendation**: For production-scale processing (50,000+ rows), we strongly recommend:
    1.  **Backend-side Geocoding**: Geocode the addresses on your server before the mobile app ever sees them.
    2.  **Paid API**: Use a robust service like the **Google Maps Geocoding API** via Retrofit for consistent high-throughput performance.
- **Usage Policy**: Always verify the usage policies of free geocoding services before using them in a commercial application.

---
*Created 2026-05-01*
