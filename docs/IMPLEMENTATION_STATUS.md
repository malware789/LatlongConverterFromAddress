# Implementation Status - Excel Geocoding Pipeline

## Current Status: VERIFIED & BUG-FIXED

### Features Implemented:
1. **Excel Import**:
   - Supports tanker schedule Excel files (Tanker No, FPL, Monday-Sunday).
   - Stores original file URI with persistable permissions.
   - Detects header rows and parses address cells.
2. **Batch Geocoding**:
   - Processes Excel cells in background via `ExcelGeocodeWorker`.
   - Supports both `FAKE` and `ANDROID_GEOCODER` modes.
   - Caches results within batches to optimize repeated addresses.
   - Saves Lat/Long results directly back to Room.
3. **Excel Export**:
   - **Rebuilt Logic**: Rebuilds the output workbook from scratch into a new structure to ensure absolute column alignment.
   - **Fixed**: Resolved misalignment and missing header issues caused by in-place shifting.
   - **Column Mapping**: Explicitly maps columns: Tanker No (A), FPL (B), Monday (C), Monday LatLong (D), ..., Sunday LatLong (P).
   - **Validations**: Validates exact header names and indices before saving.
   - **Logs**: Detailed logging of original vs final columns and first 5 data rows.

### Verified Flow:
1. **Import**: Activity launches `ACTION_OPEN_DOCUMENT` -> Permission persisted -> Job created in Room -> Cells parsed and stored.
2. **Work**: `ExcelGeocodeWorker` picks up job -> Geocodes batches -> Updates Room with `$lat,$lng` or error status.
3. **Export**: Activity launches `ACTION_CREATE_DOCUMENT` -> ViewModel opens original file -> Processor rebuilds into new structure -> Stream flushed and closed.

### Recent Fixes:
- **Column Misalignment**: Switched from `shiftColumns` to explicit workbook rebuilding. This ensures headers like "Tuesday" are never lost and stay aligned with their data.
- **Header Validation**: Added a strict check that the final header row matches the expected structure.
- **Cell Lookup Optimization**: Using `rowIndex` and `dayName` to match geocoded results accurately between Room and the Excel structure.
