package com.watsoo.addressconverter.data.excel

import android.content.Context
import android.net.Uri
import com.watsoo.addressconverter.data.local.ExcelAddressCellEntity
import com.watsoo.addressconverter.data.local.ExcelStatus
import org.apache.poi.ss.usermodel.*
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.InputStream
import java.io.OutputStream

class ExcelFileProcessor(private val context: Context) {

    private val expectedHeaders = listOf(
        "Tanker No", "FPL", "Monday", "Tuesday", "Wednesday",
        "Thursday", "Friday", "Saturday", "Sunday"
    )

    data class ParsedExcel(
        val cells: List<ExcelAddressCellEntity>,
        val totalAddressCells: Int
    )

    fun parseExcel(uri: Uri, jobId: Long): ParsedExcel {
        val inputStream: InputStream = context.contentResolver.openInputStream(uri)
            ?: throw Exception("Could not open Excel file")
        
        val workbook = WorkbookFactory.create(inputStream)
        val sheet = workbook.getSheetAt(0)
        val headerRow = findHeaderRow(sheet) ?: throw Exception("Expected headers not found in Excel")
        
        val headerMap = mapHeaders(headerRow)
        val cells = mutableListOf<ExcelAddressCellEntity>()
        
        var lastTankerNo = ""
        var lastFpl = ""
        
        for (rowIndex in (headerRow.rowNum + 1)..sheet.lastRowNum) {
            val row = sheet.getRow(rowIndex) ?: continue
            
            val tankerNo = getCellValueAsString(row.getCell(headerMap["Tanker No"] ?: -1)).ifBlank { lastTankerNo }
            val fpl = getCellValueAsString(row.getCell(headerMap["FPL"] ?: -1)).ifBlank { lastFpl }
            
            lastTankerNo = tankerNo
            lastFpl = fpl
            
            for (day in listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")) {
                val colIndex = headerMap[day] ?: continue
                val address = getCellValueAsString(row.getCell(colIndex)).trim()
                
                if (address.isNotBlank()) {
                    cells.add(
                        ExcelAddressCellEntity(
                            jobId = jobId,
                            sheetName = sheet.sheetName,
                            rowIndex = rowIndex,
                            columnIndex = colIndex,
                            dayName = day,
                            tankerNo = tankerNo,
                            fpl = fpl,
                            address = address,
                            normalizedAddress = address.lowercase()
                        )
                    )
                }
            }
        }
        
        workbook.close()
        return ParsedExcel(cells, cells.size)
    }

    fun exportExcel(inputUri: Uri, outputStream: OutputStream, geocodedCells: List<ExcelAddressCellEntity>) {
        android.util.Log.d("ExcelFileProcessor", "Exporting Excel. Job ID: ${geocodedCells.firstOrNull()?.jobId}, Cells: ${geocodedCells.size}")
        
        if (geocodedCells.isEmpty()) {
            throw Exception("Export failed: no address rows found for this Excel job")
        }

        val inputStream: InputStream = context.contentResolver.openInputStream(inputUri)
            ?: throw Exception("Could not open original Excel file at $inputUri")
        
        val sourceWorkbook = WorkbookFactory.create(inputStream)
        if (sourceWorkbook.numberOfSheets == 0) {
            sourceWorkbook.close()
            throw Exception("Original Excel file has no sheets")
        }

        val sourceSheet = sourceWorkbook.getSheetAt(0)
        val sourceHeaderRow = findHeaderRow(sourceSheet) ?: throw Exception("Header row not found in source")
        val sourceHeaderMap = mapHeaders(sourceHeaderRow)
        
        // Define exact output column order
        val outputHeaders = listOf(
            "Tanker No", "FPL",
            "Monday", "Monday LatLong",
            "Tuesday", "Tuesday LatLong",
            "Wednesday", "Wednesday LatLong",
            "Thursday", "Thursday LatLong",
            "Friday", "Friday LatLong",
            "Saturday", "Saturday LatLong",
            "Sunday", "Sunday LatLong"
        )

        val destWorkbook = XSSFWorkbook()
        val destSheet = destWorkbook.createSheet("Geocoded Results")
        
        // Create Header Row
        val destHeaderRow = destSheet.createRow(0)
        outputHeaders.forEachIndexed { index, header ->
            destHeaderRow.createCell(index).setCellValue(header)
        }

        // Cache cells for fast lookup: rowIndex -> dayName -> cell
        val cellCache = geocodedCells.groupBy { it.rowIndex }
            .mapValues { entry -> entry.value.associateBy { it.dayName } }

        var destRowIndex = 1
        var latLongWrittenCount = 0

        val days = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")

        // Iterate through source data rows
        for (rowIndex in (sourceHeaderRow.rowNum + 1)..sourceSheet.lastRowNum) {
            val sourceRow = sourceSheet.getRow(rowIndex) ?: continue
            val destRow = destSheet.createRow(destRowIndex++)

            // 1. Copy Tanker No and FPL
            val tankerNo = getCellValueAsString(sourceRow.getCell(sourceHeaderMap["Tanker No"] ?: -1))
            val fpl = getCellValueAsString(sourceRow.getCell(sourceHeaderMap["FPL"] ?: -1))
            destRow.createCell(0).setCellValue(tankerNo)
            destRow.createCell(1).setCellValue(fpl)

            // 2. Process each day
            days.forEachIndexed { dayIndex, day ->
                val addrColIdx = 2 + (dayIndex * 2)
                val latLongColIdx = addrColIdx + 1

                // Copy original address
                val sourceAddrCol = sourceHeaderMap[day] ?: -1
                val address = getCellValueAsString(sourceRow.getCell(sourceAddrCol))
                destRow.createCell(addrColIdx).setCellValue(address)

                // Write LatLong result
                val cellData = cellCache[rowIndex]?.get(day)
                val value = when (cellData?.status) {
                    ExcelStatus.GEOCODED -> cellData.latLongText ?: ""
                    ExcelStatus.FAILED_PERM -> "not converted"
                    ExcelStatus.FAILED_TEMP -> "retry pending"
                    ExcelStatus.PENDING_GEOCODE, ExcelStatus.GEOCODING -> "pending"
                    null -> if (address.isNotBlank()) "pending" else ""
                    ExcelStatus.EXPORTED -> cellData.latLongText ?: ""
                }
                
                if (value.isNotBlank()) {
                    destRow.createCell(latLongColIdx).setCellValue(value)
                    latLongWrittenCount++
                }
            }
            
            if (destRowIndex <= 6) { // Log first 5 data rows
                android.util.Log.d("ExcelFileProcessor", "Row ${destRowIndex-1} written: Tanker=${tankerNo}, MonAddr=${destRow.getCell(2).stringCellValue}, MonLat=${destRow.getCell(3).stringCellValue}")
            }
        }

        // Final Validation
        validateHeaders(destSheet, outputHeaders)
        
        android.util.Log.d("ExcelFileProcessor", "Writing new workbook. Total rows: ${destRowIndex-1}, LatLong cells: $latLongWrittenCount")
        
        destWorkbook.write(outputStream)
        outputStream.flush()
        
        sourceWorkbook.close()
        destWorkbook.close()
        android.util.Log.d("ExcelFileProcessor", "Export completed successfully")
    }

    private fun validateHeaders(sheet: Sheet, expected: List<String>) {
        val row = sheet.getRow(0) ?: throw Exception("Validation failed: No header row in output")
        expected.forEachIndexed { index, name ->
            val value = row.getCell(index)?.stringCellValue
            if (value != name) {
                throw Exception("Validation failed: Column $index expected '$name' but found '$value'")
            }
        }
    }

    private fun findHeaderRow(sheet: Sheet): Row? {
        for (rowIndex in 0..sheet.lastRowNum) {
            val row = sheet.getRow(rowIndex) ?: continue
            var matchCount = 0
            for (cellIndex in 0..row.lastCellNum) {
                val value = getCellValueAsString(row.getCell(cellIndex))
                if (expectedHeaders.any { it.equals(value, ignoreCase = true) }) {
                    matchCount++
                }
            }
            if (matchCount >= 5) return row // Threshold to identify header row
        }
        return null
    }

    private fun mapHeaders(row: Row): Map<String, Int> {
        val map = mutableMapOf<String, Int>()
        for (cellIndex in 0 until row.lastCellNum) {
            val value = getCellValueAsString(row.getCell(cellIndex))
            map[value] = cellIndex
        }
        return map
    }

    private fun getCellValueAsString(cell: Cell?): String {
        if (cell == null) return ""
        return when (cell.cellType) {
            CellType.STRING -> cell.stringCellValue
            CellType.NUMERIC -> {
                if (DateUtil.isCellDateFormatted(cell)) {
                    cell.dateCellValue.toString()
                } else {
                    cell.numericCellValue.toString().removeSuffix(".0")
                }
            }
            CellType.BOOLEAN -> cell.booleanCellValue.toString()
            CellType.FORMULA -> {
                try {
                    cell.stringCellValue
                } catch (e: Exception) {
                    cell.numericCellValue.toString()
                }
            }
            else -> ""
        }
    }
}
