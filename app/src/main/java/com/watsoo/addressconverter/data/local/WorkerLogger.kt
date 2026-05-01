package com.watsoo.addressconverter.data.local

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Singleton logger that writes structured events to the worker_logs Room table.
 * Trims old logs automatically to cap storage usage.
 */
object WorkerLogger {

    private var logDao: WorkerLogDao? = null

    fun init(dao: WorkerLogDao) {
        logDao = dao
    }

    fun info(message: String) = log(LogLevel.INFO, message)
    fun warning(message: String) = log(LogLevel.WARNING, message)
    fun error(message: String) = log(LogLevel.ERROR, message)

    private fun log(level: LogLevel, message: String) {
        val dao = logDao ?: return
        CoroutineScope(Dispatchers.IO).launch {
            dao.insert(WorkerLogEntity(level = level.name, message = message))
            dao.trimOldLogs(keepCount = 500)
        }
    }
}
