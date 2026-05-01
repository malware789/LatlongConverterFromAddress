package com.watsoo.addressconverter.worker

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.watsoo.addressconverter.data.local.AddressStatus
import com.watsoo.addressconverter.data.local.AppDatabase
import com.watsoo.addressconverter.data.local.WorkerLogger
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class BootReceiver : BroadcastReceiver() {

    @Inject
    lateinit var database: AppDatabase

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            scope.launch {
                val unfinishedGeocode = database.addressDao().countByStatus(AddressStatus.PENDING_GEOCODE.name)
                val unfinishedUpload = database.addressDao().countByStatus(AddressStatus.GEOCODED_PENDING_UPLOAD.name)
                val tempFailed = database.addressDao().countByStatus(AddressStatus.FAILED_TEMP.name)

                if (unfinishedGeocode > 0 || unfinishedUpload > 0 || tempFailed > 0) {
                    WorkerLogger.init(database.workerLogDao())
                    WorkerLogger.info("Boot detected – unfinished work found ($unfinishedGeocode pending, $unfinishedUpload ready, $tempFailed retryable). Restarting worker…")
                    AddressWorkStarter.startWork(context)
                }
            }
        }
    }
}
