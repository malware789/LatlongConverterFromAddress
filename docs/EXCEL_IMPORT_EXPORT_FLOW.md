# Excel Import/Export Geocoding Flow

## 1. Overview
The Excel flow allows users to geocode addresses from tanker schedule spreadsheets. The app preserves the original data and rebuilds it into a structured format with side-by-side LatLong columns.

## 2. Detailed Pipeline

### A. Import Phase
1. User selects a file via `ActivityResultContracts.OpenDocument`.
2. App requests **persistable URI permissions**:
   ```kotlin
   contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
   ```
3. `ExcelRepository` creates a new `ExcelImportJobEntity`.
4. `ExcelFileProcessor` scans for headers ("Tanker No", "FPL", "Monday"...).
5. Rows are parsed. Tanker No and FPL are inherited from previous rows if blank (top-down fill).
6. Address cells are saved to `excel_address_cells` table with `PENDING_GEOCODE`.

### B. Geocoding Phase
1. `ExcelGeocodeWorker` runs in the background.
2. It fetches pending cells in batches (default 50).
3. For each unique address in a batch, it calls `GeocoderClient`.
4. Results are saved immediately to the database (`GEOCODED`, `FAILED_TEMP`, or `FAILED_PERM`).
5. Progress is shown in the system notification.

### C. Export Phase (Rebuild Logic)
1. User selects an output location via `ActivityResultContracts.CreateDocument`.
2. App opens the **Original Source File** using the stored `inputFileUri`.
3. `ExcelFileProcessor` creates a **NEW** `XSSFWorkbook`.
4. A new header row is constructed with explicit column indices:
   - A: Tanker No | B: FPL
   - C: Monday | D: Monday LatLong
   - E: Tuesday | F: Tuesday LatLong
   - ... and so on until P: Sunday LatLong
5. For each data row in the source file:
   - Original "Tanker No" and "FPL" are copied to A and B.
   - For each weekday, the original address is copied to its target column (C, E, G, etc.).
   - The geocoded result is looked up in Room by `rowIndex` and `dayName`.
   - The result (LatLong or status string) is written to the adjacent column (D, F, H, etc.).
6. **Validation**: The final header row is validated against the expected list of 16 columns.
7. The new workbook is written to the output `OutputStream`.

## 3. Data Safety & Validations
- **Alignment**: By rebuilding the workbook rather than shifting columns, we ensure that headers and data never become misaligned.
- **Empty Cells**: If a cell was blank in the original file, it remains blank in the export.
- **Status Reporting**:
  - `GEOCODED`: "latitude,longitude"
  - `PENDING_GEOCODE`: "pending"
  - `FAILED_PERM`: "not converted"
- **Logs**: The app logs the first 5 rows of the export to Logcat for audit verification.
