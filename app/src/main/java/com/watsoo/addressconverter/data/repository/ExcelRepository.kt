package com.watsoo.addressconverter.data.repository

import android.net.Uri
import com.watsoo.addressconverter.data.excel.ExcelFileProcessor
import com.watsoo.addressconverter.data.local.*
import com.watsoo.addressconverter.geocode.GeocoderClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.OutputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ExcelRepository @Inject constructor(
    private val excelDao: ExcelDao,
    private val geocoderClient: GeocoderClient,
    private val excelFileProcessor: ExcelFileProcessor
) {

    fun getAllJobsFlow(): Flow<List<ExcelImportJobEntity>> = excelDao.getAllJobsFlow()

    suspend fun getJobById(jobId: Long): ExcelImportJobEntity? = excelDao.getJobById(jobId)

    suspend fun importExcel(uri: Uri, fileName: String): Long = withContext(Dispatchers.IO) {
        val jobId = excelDao.insertJob(
            ExcelImportJobEntity(
                fileName = fileName,
                status = "IMPORTING",
                totalAddressCells = 0,
                inputFileUri = uri.toString()
            )
        )
        
        try {
            val parsed = excelFileProcessor.parseExcel(uri, jobId)
            excelDao.insertCells(parsed.cells)
            
            val updatedJob = excelDao.getJobById(jobId)?.copy(
                status = "PENDING",
                totalAddressCells = parsed.totalAddressCells
            )
            if (updatedJob != null) {
                excelDao.updateJob(updatedJob)
            }
            WorkerLogger.info("Excel import completed: $fileName ($jobId)")
            jobId
        } catch (e: Exception) {
            excelDao.updateJob(
                excelDao.getJobById(jobId)?.copy(status = "FAILED_IMPORT") 
                ?: ExcelImportJobEntity(jobId = jobId, fileName = fileName, status = "FAILED_IMPORT", totalAddressCells = 0)
            )
            WorkerLogger.error("Excel import failed: ${e.message}")
            throw e
        }
    }

    suspend fun processExcelBatch(limit: Int = 50): Boolean = withContext(Dispatchers.IO) {
        val pendingCells = excelDao.getPendingCells(limit)
        if (pendingCells.isEmpty()) return@withContext false

        // Cache for normalized addresses in this batch
        val batchCache = mutableMapOf<String, com.watsoo.addressconverter.geocode.GeocodeResult>()

        for (cell in pendingCells) {
            // Update status to GEOCODING
            excelDao.updateCell(cell.copy(status = ExcelStatus.GEOCODING, updatedAt = System.currentTimeMillis()))
            
            // Check cache
            val cachedResult = batchCache[cell.normalizedAddress]
            if (cachedResult != null) {
                updateCellWithResult(cell, cachedResult.latitude, cachedResult.longitude)
                continue
            }
            
            // Real geocoding
            try {
                val result = geocoderClient.geocode(cell.address)
                batchCache[cell.normalizedAddress] = result
                updateCellWithResult(cell, result.latitude, result.longitude)
            } catch (e: Exception) {
                // Temporary failure (exception)
                val newAttempts = cell.attempts + 1
                val status = if (newAttempts >= 5) ExcelStatus.FAILED_PERM else ExcelStatus.FAILED_TEMP
                excelDao.updateCell(cell.copy(
                    status = status,
                    attempts = newAttempts,
                    lastError = e.message,
                    updatedAt = System.currentTimeMillis()
                ))
            }
        }
        
        // Update job counts
        val jobIds = pendingCells.map { it.jobId }.distinct()
        for (jobId in jobIds) {
            updateJobCounts(jobId)
        }
        
        true
    }

    private suspend fun updateCellWithResult(cell: ExcelAddressCellEntity, lat: Double, lng: Double) {
        excelDao.updateCell(cell.copy(
            status = ExcelStatus.GEOCODED,
            latitude = lat,
            longitude = lng,
            latLongText = "$lat,$lng",
            updatedAt = System.currentTimeMillis()
        ))
    }

    private suspend fun updateJobCounts(jobId: Long) {
        val job = excelDao.getJobById(jobId) ?: return
        val converted = excelDao.countCellsByStatus(jobId, ExcelStatus.GEOCODED)
        val failed = excelDao.countCellsByStatus(jobId, ExcelStatus.FAILED_PERM)
        
        excelDao.updateJob(job.copy(
            convertedCount = converted,
            failedCount = failed
        ))
    }

    suspend fun exportJob(jobId: Long, inputUri: Uri, outputStream: OutputStream) = withContext(Dispatchers.IO) {
        val job = excelDao.getJobById(jobId) ?: throw Exception("Job not found")
        val cells = excelDao.getCellsByJob(jobId)
        
        excelFileProcessor.exportExcel(inputUri, outputStream, cells)
        
        excelDao.updateJob(job.copy(status = "COMPLETED"))
        WorkerLogger.info("Excel export completed for job $jobId")
    }

    suspend fun deleteJob(jobId: Long) = withContext(Dispatchers.IO) {
        excelDao.deleteJobAndCells(jobId)
    }

    suspend fun releaseStaleLocks() = withContext(Dispatchers.IO) {
        excelDao.releaseStaleLocks()
    }
    
    suspend fun countTotalCells(jobId: Long): Int = excelDao.countTotalCells(jobId)
    suspend fun countCellsByStatus(jobId: Long, status: ExcelStatus): Int = excelDao.countCellsByStatus(jobId, status)
}
