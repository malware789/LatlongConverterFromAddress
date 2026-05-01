package com.watsoo.addressconverter.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.ForegroundInfo
import com.watsoo.addressconverter.data.local.ExcelStatus
import com.watsoo.addressconverter.data.local.WorkerLogger
import com.watsoo.addressconverter.data.repository.ExcelRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.delay

@HiltWorker
class ExcelGeocodeWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val repository: ExcelRepository
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val jobId = inputData.getLong("jobId", -1L)
        if (jobId == -1L) {
            WorkerLogger.error("ExcelGeocodeWorker: No jobId provided")
            return Result.failure()
        }

        WorkerLogger.info("ExcelGeocodeWorker started for job $jobId")
        
        setForeground(createForegroundInfo(0, 0))
        
        repository.releaseStaleLocks()
        
        var hasMore = true
        while (hasMore && !isStopped) {
            try {
                val total = repository.countTotalCells(jobId)
                val geocoded = repository.countCellsByStatus(jobId, ExcelStatus.GEOCODED)
                val failed = repository.countCellsByStatus(jobId, ExcelStatus.FAILED_PERM)
                val processed = geocoded + failed
                
                setForeground(createForegroundInfo(processed, total))
                
                hasMore = repository.processExcelBatch()
                if (hasMore) {
                    delay(500) // Small delay between batches
                }
            } catch (e: Exception) {
                WorkerLogger.error("ExcelGeocodeWorker error: ${e.message}")
                return Result.retry()
            }
        }
        
        WorkerLogger.info("ExcelGeocodeWorker finished for job $jobId")
        return Result.success()
    }

    private fun createForegroundInfo(processed: Int, total: Int): ForegroundInfo {
        val notification = NotificationHelper.createNotification(
            context = applicationContext,
            title = "Excel Geocoding",
            message = "Processing $processed of $total addresses",
            processedCount = processed,
            totalCount = total
        )
        
        return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            ForegroundInfo(
                NotificationHelper.NOTIFICATION_ID,
                notification,
                android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            )
        } else {
            ForegroundInfo(NotificationHelper.NOTIFICATION_ID, notification)
        }
    }

    override suspend fun getForegroundInfo(): ForegroundInfo {
        return createForegroundInfo(0, 0)
    }
}
