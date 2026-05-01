# Architecture Notes

This document justifies the architectural decisions made in the `AddressConverter` app.

## Core Pipeline Principle
Room database is the source of truth. The app must follow:
Fetch API → Room → Geocode → Room → Upload API → Room status update.

## Why Room is used
When dealing with tens of thousands of records on a mobile device, retaining them in memory (like a `List<Address>` inside a ViewModel) is dangerous. An Out-Of-Memory (OOM) error or an OS process kill would wipe out all progress.
Room provides a robust SQLite wrapper to persist the queue. Every address is tracked via the `AddressStatus` enum, ensuring we know exactly where each item is in the pipeline, even if the phone reboots.

## Why WorkManager instead of a plain Service
Processing 50,000 addresses can take hours depending on network limits and geocoder API rate limits.
Plain Android `Service`s (even foreground services) are heavily restricted by modern Android battery optimization features (Doze mode, App Standby Buckets). 
`WorkManager` is the Android architectural standard for guaranteed execution. It natively supports constraints (e.g., "only run when connected to unmetered network") and automatically handles restarts upon OS death.

## Why each address result is saved immediately
In `AddressRepository.processNextBatch()`, you'll notice a loop that calls `addressDao.markAsGeocoded()` *immediately* after every successful geocode, rather than waiting for the whole batch of 100 to finish.
This is because Geocoding APIs often have strict rate limits (e.g., 50 requests per second). A batch of 100 might take several seconds. If the app crashes on item 99, we don't want to re-geocode the first 98 items, as this wastes API quota.

## Why batches of 100 are used
While geocoding is done one-by-one, uploading is done in batches of 100.
Uploading 50,000 addresses via 50,000 individual HTTP requests creates massive networking overhead (TCP handshakes, headers, battery drain). Batching them groups the payload into reasonable JSON arrays, significantly reducing battery consumption and speeding up server ingress.

## Why Coroutine Concurrency should be limited
Right now, `geocoderClient.geocode(address)` is executed sequentially inside a `for` loop.
If you attempt to use `async { }` and `awaitAll()` to geocode 100 items simultaneously, you will likely trigger HTTP 429 (Too Many Requests) from the geocoder provider. Concurrency should be implemented carefully using Kotlin `Semaphore` or `Flow.flatMapMerge(concurrency = 5)` to throttle network egress.

## Why fake APIs exist for now
Development shouldn't be blocked by backend readiness. By implementing `FakeAddressApi` and `FakeGeocoderClient`, the Android team can fully build out the UI, database locking, batch limits, error retries, and WorkManager configurations safely. Once the backend team finishes the API, it's a simple interface swap.

## Android Background Execution Considerations
Because WorkManager handles the processing, the user can press the home button, open YouTube, or lock the phone, and the geocoding will safely continue. 
If processing times strictly require running immediately without being deferred by Doze, you may eventually want to turn `AddressConvertWorker` into an Expedited Worker (`setForeground()`). However, WorkManager's default behavior is generally sufficient for asynchronous bulk data processing.
