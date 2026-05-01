package com.watsoo.addressconverter.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class LogLevel { INFO, WARNING, ERROR }

@Entity(tableName = "worker_logs")
data class WorkerLogEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val level: String = LogLevel.INFO.name,
    val message: String
)
