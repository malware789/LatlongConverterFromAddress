# AddressConverter

An Android application designed to fetch a massive list of addresses (up to 50,000) from a server, systematically geocode them into latitudes and longitudes, and upload the results back to the server.

This project is built using Kotlin, XML layouts, MVVM, Room Database, and WorkManager.

## Notice: Core Pipeline Verified
The core processing pipeline (Fetch → Room → Geocode → Room → Upload → SENT) is now fully verified.
- **Manual Verification**: 100% SUCCESS. The app successfully geocodes real addresses using the system geocoder and simulates the upload process.
- **Unit Tests**: 100% SUCCESS (19/19 passed).
- **Hilt/WorkManager**: Fully integrated and stable.

Real backend API endpoints are still pending integration. The application currently relies on `FakeAddressApi` and `FakeGeocoderClient` (or `AndroidGeocoderClient` for manual testing) to simulate network behavior.

## How to run the app
1. Clone the project and open it in Android Studio.
2. Build the project using gradle (`./gradlew assembleDebug`).
3. Deploy to an emulator or physical device running Android API 26 or higher.

## How to use the app

### Main Screen (Dashboard)
| Button | Action |
|---|---|
| **Start Work** | Enqueues a WorkManager job: fetch → geocode (batches of 100) → upload |
| **Cancel Work** | Cancels any active WorkManager job |
| **Reset Data** | Cancels work, then deletes all local address rows (with confirmation dialog) |
| **Retry Temp** | Immediately resets all `FAILED_TEMP` rows to `PENDING_GEOCODE` |
| **Inspect Rows** | Opens the Address Inspector screen |
| **Clear (Logs)** | Wipes the worker event log table |

The dashboard shows live counts for every status (Pending, Geocoding, Geocoded, Uploading, Sent, Temp Failed, Perm Failed) and a scrollable worker event log updated every 2 seconds.

### Address Inspector Screen
- **Filter bar** (horizontal scroll): All / Pending / Geocoding / Geocoded / Uploading / Sent / Temp Failed / Perm Failed
- Each row shows: server ID, address text, colour-coded status, geocode/upload attempt counts, lat/lng (if available), last error, and last-updated timestamp
- **Retry Temp Failures** – resets all `FAILED_TEMP` to `PENDING_GEOCODE` instantly
- **Retry Perm Failures** – resets all `FAILED_PERM` to `PENDING_GEOCODE` with attempt counters cleared (requires confirmation)
- **Clear Perm** – permanently deletes all `FAILED_PERM` rows (requires confirmation)
- List auto-refreshes every 2 seconds

### Adjusting fake behaviour
Edit `AppConfig.kt` to change batch sizes, concurrency, retry limits, and failure rates without touching business logic:
```
FAKE_GEOCODER_TEMP_FAILURE_PERCENT = 0.05f   // 5% temp failures
FAKE_GEOCODER_PERM_FAILURE_PERCENT = 0.02f   // 2% perm failures
FAKE_UPLOAD_FAILURE_PERCENT        = 0.15f   // 15% upload failures
MAX_BATCHES_PER_WORKER_RUN         = 10
GEOCODING_CONCURRENCY              = 3
```


### Geocoding Modes

The app supports two geocoding modes for development and testing:
- **`FAKE` (Default)**: Returns simulated coordinates without real network calls. Ideal for UI and logic testing.
- **`ANDROID_GEOCODER`**: Uses real device geocoding. Returns actual latitude and longitude. ⚠️ **Warning**: For production bulk conversion of 50k+ addresses, a backend-side solution or a dedicated paid API is recommended over the free system geocoder to ensure throughput and accuracy.

Check [docs/MANUAL_REAL_ADDRESS_TESTING.md](docs/MANUAL_REAL_ADDRESS_TESTING.md) for detailed manual verification steps.


## Testing & Validation

### Automated Tests
Run unit tests for DAO, Repository, Mappers, and Workers:
```bash
./gradlew testDebugUnitTest
```
These tests use **Robolectric** for in-memory database verification and **MockK** for dependency mocking.

### Manual Verification
For real-device testing of the fake flow, follow the [Manual QA Checklist](docs/QA_CHECKLIST.md).

## Handoff Documentation
If you are an AI agent or a developer taking over this project, please read the documentation inside the `docs/` folder:

- [docs/PROJECT_OVERVIEW.md](docs/PROJECT_OVERVIEW.md)
- [docs/IMPLEMENTATION_STATUS.md](docs/IMPLEMENTATION_STATUS.md) - **Check this for current progress**
- [docs/QA_CHECKLIST.md](docs/QA_CHECKLIST.md) - Manual testing steps
- [docs/TODO_BACKEND_INTEGRATION.md](docs/TODO_BACKEND_INTEGRATION.md) - Real API specs
- [docs/ARCHITECTURE_NOTES.md](docs/ARCHITECTURE_NOTES.md)
- [docs/AI_HANDOFF_PROMPT.md](docs/AI_HANDOFF_PROMPT.md)
