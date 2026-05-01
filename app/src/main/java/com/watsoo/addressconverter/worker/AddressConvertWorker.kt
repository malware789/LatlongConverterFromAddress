package com.watsoo.addressconverter.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.watsoo.addressconverter.config.AppConfig
import com.watsoo.addressconverter.data.local.AddressStatus
import com.watsoo.addressconverter.data.local.WorkerLogDao
import com.watsoo.addressconverter.data.local.WorkerLogger
import com.watsoo.addressconverter.data.repository.AddressRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import android.util.Log
import kotlinx.coroutines.delay

@HiltWorker
class AddressConvertWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val repository: AddressRepository,
    private val workerLogDao: WorkerLogDao
) : CoroutineWorker(appContext, workerParams) {

    init {
        Log.d("AddressConvertWorker", "AddressConvertWorker constructor called")
    }

    override suspend fun doWork(): Result {
        WorkerLogger.init(workerLogDao)
        WorkerLogger.info("doWork started")
        Log.d("AddressConvertWorker", "doWork started")
        
        var currentStage = "STARTUP"

        // ── Foreground Support ───────────────────────────────────────────
        try {
            currentStage = "NOTIFICATION"
            val foregroundInfo = getForegroundInfo("Starting…")
            setForeground(foregroundInfo)
            WorkerLogger.info("setForeground called successfully")
        } catch (e: Exception) {
            WorkerLogger.warning("Could not set foreground: ${e.message}")
        }

        return try {
            WorkerLogger.info("Selected Geocoder Mode: ${AppConfig.GEOCODER_MODE}")

            // 1. Release stale locks first
            currentStage = "RELEASE_LOCKS"
            WorkerLogger.info("repository.releaseAllStaleLocks started")
            repository.releaseAllStaleLocks()
            WorkerLogger.info("repository.releaseAllStaleLocks completed")

            // 2. Initial state check
            currentStage = "STATE_CHECK"
            val initialPending = repository.getCountByStatus(AddressStatus.PENDING_GEOCODE)
            val initialUploadable = repository.getCountByStatus(AddressStatus.GEOCODED_PENDING_UPLOAD)
            val totalInRoom = repository.getCountTotal()
            
            WorkerLogger.info("Initial Status Check – Pending: $initialPending, Uploadable: $initialUploadable, Total: $totalInRoom")

            // 3. Fetch phase (only if room is empty or low on pending work)
            if (initialPending == 0 && initialUploadable == 0) {
                currentStage = "FETCH"
                updateProgress("FETCHING", "Fetching server addresses…", 0, totalInRoom)
                WorkerLogger.info("fetchAndSaveAddresses started")
                try {
                    repository.fetchAndSaveAddresses()
                    WorkerLogger.info("fetchAndSaveAddresses completed")
                } catch (e: Exception) {
                    WorkerLogger.warning("Fetch phase failed: ${e.message}. Continuing with local data if any.")
                }
            } else {
                WorkerLogger.info("Skipping fetch – local work exists in Room.")
            }

            if (isStopped) {
                logAndNotifyStopped("Worker stopped after check/fetch phase")
                return Result.success()
            }

            // ── Process / Upload phase ───────────────────────────────────
            currentStage = "PROCESSING_LOOP"
            var batchesProcessed = 0
            var hadWork = true

            while (hadWork && batchesProcessed < AppConfig.MAX_BATCHES_PER_WORKER_RUN && !isStopped) {
                val total = repository.getCountTotal()
                val sent = repository.getCountByStatus(AddressStatus.SENT)
                val pending = repository.getCountByStatus(AddressStatus.PENDING_GEOCODE)
                val processed = total - pending

                updateProgress(
                    stage = "PROCESSING",
                    msg = "Batch ${batchesProcessed + 1} | Sent: $sent/$total",
                    processed = processed,
                    total = total
                )

                WorkerLogger.info("processNextBatch started (Batch ${batchesProcessed + 1})")
                val geocodedSomething = repository.processNextBatch()
                
                WorkerLogger.info("uploadCompletedBatch started (Batch ${batchesProcessed + 1})")
                val uploadedSomething = repository.uploadCompletedBatch()

                hadWork = geocodedSomething || uploadedSomething
                batchesProcessed++
                delay(300) // small yield
            }

            when {
                isStopped -> {
                    logAndNotifyStopped("Worker cancelled/stopped mid-run")
                    Result.success()
                }
                hadWork && batchesProcessed >= AppConfig.MAX_BATCHES_PER_WORKER_RUN -> {
                    WorkerLogger.info("Batch limit reached ($batchesProcessed), rescheduling…")
                    updateProgress("RESCHEDULING", "Limit reached – rescheduling", 0, 0)
                    Result.retry()
                }
                else -> {
                    val total = repository.getCountTotal()
                    val sent = repository.getCountByStatus(AddressStatus.SENT)
                    WorkerLogger.info("Worker completed – all work finished (Sent: $sent/$total)")
                    updateProgress("COMPLETED", "All caught up ($sent/$total) ✓", sent, total)
                    Result.success()
                }
            }
        } catch (e: Exception) {
            val stackTrace = Log.getStackTraceString(e)
            WorkerLogger.error("Worker CRITICAL FAILURE [Stage: $currentStage]: ${e.message}\n$stackTrace")
            
            updateProgress("ERROR", "Failed at $currentStage: ${e.message}", 0, 0)
            
            val outputData = workDataOf(
                "errorMessage" to (e.message ?: "Unknown error"),
                "errorClass" to e.javaClass.simpleName,
                "failedStage" to currentStage,
                "stacktrace" to stackTrace
            )
            
            if (e is IllegalStateException || e is NullPointerException) {
                Result.failure(outputData)
            } else {
                Result.retry()
            }
        }
    }

    private suspend fun updateProgress(stage: String, msg: String, processed: Int, total: Int) {
        val data = workDataOf(
            "stage" to stage,
            "progress_msg" to msg,
            "processed_count" to processed,
            "total_count" to total
        )
        setProgress(data)
        
        // Update notification
        try {
            setForeground(getForegroundInfo(msg, processed, total))
        } catch (e: Exception) {
            // ignore foreground update failures if already stopped
        }
    }

    private fun logAndNotifyStopped(message: String) {
        WorkerLogger.warning(message)
        // Can't setProgress after stop easily, but we logged it.
    }

    override suspend fun getForegroundInfo(): androidx.work.ForegroundInfo {
        return getForegroundInfo("Running address conversion…")
    }

    private fun getForegroundInfo(message: String, processed: Int = 0, total: Int = 0): androidx.work.ForegroundInfo {
        val notification = NotificationHelper.createNotification(
            applicationContext,
            "Address conversion running",
            message,
            processed,
            total
        )
        
        return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            androidx.work.ForegroundInfo(
                NotificationHelper.NOTIFICATION_ID,
                notification,
                android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            )
        } else {
            androidx.work.ForegroundInfo(NotificationHelper.NOTIFICATION_ID, notification)
        }
    }
}
