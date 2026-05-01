package com.watsoo.addressconverter.worker

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.watsoo.addressconverter.data.local.WorkerLogger
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class CancelWorkReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        WorkerLogger.warning("Cancel triggered from notification")
        AddressWorkStarter.cancelWork(context)
    }
}
