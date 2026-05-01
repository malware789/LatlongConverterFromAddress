# Project Overview

**Project Name**: AddressConverter
**Platform**: Android (Kotlin + XML)
**Architecture**: MVVM with Repository Pattern

## Purpose
The primary goal of this application is to fetch a large volume of addresses (e.g., 50,000+), locally convert them into their respective latitude and longitude coordinates, and then upload the resulting coordinates back to a server. 

Since the process of converting tens of thousands of addresses is network and time-intensive, it needs to be resilient against device reboots, network failures, and OS-level process kills.

## Expected Production Flow
1. **Fetch Batch**: Download a chunk of unprocessed addresses (e.g., 5,000 at a time) from the server.
2. **Local Storage**: Save these addresses into the local Room database to maintain a persistent queue.
3. **Geocode in Batches**: Process the queued addresses in batches (e.g., 100 at a time) by hitting a Geocoding service.
4. **Immediate Persistence**: Every single geocoded result is saved immediately to the local database to ensure no work is lost if the app is killed mid-batch.
5. **Upload Results**: Upload the successfully geocoded batch (Id + Address + Latitude + Longitude) to the server.
6. **Retry Mechanism**: If a temporary network error occurs during geocoding or uploading, apply exponential backoff and retry later.
7. **Permanent Failures**: If an address is malformed or fundamentally un-geocodeable, mark it as a permanent failure to avoid endless retry loops.

## Current Stage
**Status**: Initial Foundation / Backend Simulation
The backend APIs and the real Geocoding API are not yet ready. To allow Android development to proceed unblocked, the app currently uses **Fake / Mock API** implementations (`FakeAddressApi` and `FakeGeocoderClient`). These fake implementations simulate network latency and randomly generate errors to validate the robustness of the architecture's error-handling and retry logic.
