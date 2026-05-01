# Manual QA Checklist

Use this checklist to verify the AddressConverter application's behavior on real devices with the fake implementation before switching to real APIs.

## 1. Initial Setup
- [ ] Fresh install of the app.
- [ ] Open the app and verify the "Main" and "Logs" tabs are visible.
- [ ] Verify "Ready to start" message is shown.

## 2. Core Conversion Flow
- [ ] Click **Start Work**.
- [ ] Verify Foreground Notification appears.
- [ ] Observe logs:
    - [ ] "Fetching server addresses started"
    - [ ] "Batch geocoding started"
    - [ ] "Upload started"
- [ ] Verify status counts in UI update periodically.
- [ ] Verify progress bar moves.

## 3. Interruptions & Persistence
- [ ] Click **Stop Work** while processing.
    - [ ] Verify notification disappears.
    - [ ] Verify logs show "Worker stopped/cancelled".
- [ ] Click **Start Work** again.
    - [ ] Verify it resumes from the existing database state (no duplicate fetches if serverId exists).
- [ ] Force close the app from Recent Apps while worker is running.
    - [ ] Reopen app and verify status counts are persisted.
    - [ ] WorkManager should resume the work eventually (check system logs or wait).

## 4. Error Handling (Simulated)
- [ ] Check Logs for `FAILED_TEMP` (Yellow/Warning).
    - [ ] Verify `nextRetryAt` is shown in the database/UI if possible.
    - [ ] Verify it retries after the backoff period.
- [ ] Check Logs for `FAILED_PERM` (Red/Error).
    - [ ] Verify row stays in `FAILED_PERM` status.
- [ ] **Reset Local Data**:
    - [ ] Verify all counts go to zero.
    - [ ] Verify Room database is cleared.

## 5. Connectivity Tests
- [ ] Start work, then disable Wi-Fi/Mobile Data.
    - [ ] Verify `Upload network error` or `Fetch network error` appears in logs.
    - [ ] Verify Worker stays in retry state or fails gracefully.
- [ ] Re-enable internet.
    - [ ] Verify work resumes successfully.

## 6. Edge Cases
- [ ] **Device Reboot**:
    - [ ] (If BootReceiver is implemented) Verify worker resumes after reboot.
- [ ] **Notification Interaction**:
    - [ ] Click the notification; verify it brings the app to the foreground.
- [ ] **Large Dataset**:
    - [ ] (Debug) Increase `FETCH_BATCH_SIZE` or run multiple times.
    - [ ] Verify UI remains responsive with thousands of rows.

## 7. Status Integrity
- [ ] Total = Pending + Geocoding + Geocoded + Uploading + Sent + Failed (Temp/Perm).
- [ ] Ensure counts never decrease (except when Resetting).
- [ ] Ensure no duplicate `serverId` in the list.
