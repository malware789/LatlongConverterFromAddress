# AI Handoff Prompt

*Copy and paste the following text into the prompt window of a new AI agent (like Cursor, Gemini, or ChatGPT) to get them up to speed on this project.*

---

**Context**: You are assisting with an Android project named `AddressConverter`. It is written in **Kotlin + XML** using **MVVM**, **Room (v2, KSP)**, and **WorkManager (CoroutineWorker)** for resilient background processing.

**Project Purpose**:
The app fetches up to 5 000 addresses from a backend, geocodes them into lat/lng in batches of 100, persists every result immediately to Room, then uploads batches to the server. Retry logic handles temporary and permanent failures using exponential backoff with jitter.

**Current State**:
Architecture is complete and compiling. Real backend APIs and geocoding services are **not integrated yet**. Everything runs on `FakeAddressApi` and `FakeGeocoderClient`, which are controlled by constants in `AppConfig.kt`.

**Key files to read first (in order)**:
1. `docs/IMPLEMENTATION_STATUS.md` — full file inventory and change log
2. `app/.../config/AppConfig.kt` — ALL tunable constants (batch sizes, concurrency, retry limits, failure %)
3. `app/.../data/local/AddressDao.kt` — query patterns, locking, retry resets
4. `app/.../data/repository/AddressRepository.kt` — fetch / geocode / upload orchestration with `Semaphore`, backoff, `WorkerLogger`
5. `app/.../worker/AddressConvertWorker.kt` — batch loop, `isStopped` guard, `AppConfig.MAX_BATCHES_PER_WORKER_RUN`
6. `app/.../data/local/AppDatabase.kt` — Room v2, includes `worker_logs` table + `MIGRATION_1_2`
7. `app/.../data/local/WorkerLogger.kt` — singleton `info/warning/error` logger → Room
8. `app/.../ui/AddressListActivity.kt` — address inspector with 8 filter buttons and retry/clear actions

**Architecture summary**:
```
MainActivity (dashboard)
  └── MainViewModel
        ├── AddressRepository
        │     ├── AddressDao (Room)
        │     ├── FakeAddressApi / AddressApi (Retrofit future)
        │     └── FakeGeocoderClient / GeocoderClient (real API future)
        └── WorkerLogDao (Room)

AddressConvertWorker (WorkManager)
  └── AddressRepository (same pattern as above)

AddressListActivity (inspector)
  └── AddressRepository (read-only + retry actions)
```

**AddressStatus lifecycle**:
`PENDING_GEOCODE` → `GEOCODING` → `GEOCODED_PENDING_UPLOAD` → `UPLOADING` → `SENT`
Failures: `FAILED_TEMP` (retried after backoff) | `FAILED_PERM` (after max attempts)

**Before making code changes**:
1. Run `./gradlew assembleDebug` to confirm build is clean.
2. Read all files listed above.
3. Check `docs/IMPLEMENTATION_STATUS.md` Change Log for the latest changes.
4. Make changes modularly — do not break `AppConfig` constants or `WorkerLogger` patterns.

**Documentation Update Rule**:
After every meaningful code change you MUST update:
1. `docs/IMPLEMENTATION_STATUS.md` — add an entry to the Change Log
2. `docs/AI_HANDOFF_PROMPT.md` — if architecture, key files, or next steps changed
3. `README.md` — if app usage or build steps changed

**Recommended next steps** (in priority order):
1. **Real API integration** — replace `FakeAddressApi` with Retrofit + OkHttp; replace `FakeGeocoderClient` with Google Maps / Mapbox Geocoding
2. **Server-side pagination** — update fetch to page through results instead of receiving all 5 000 at once
3. **Production hardening** — add network constraint to `AddressWorkStarter`; add Crashlytics / error monitoring
