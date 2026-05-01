package com.watsoo.addressconverter.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "excel_import_jobs")
data class ExcelImportJobEntity(
    @PrimaryKey(autoGenerate = true)
    val jobId: Long = 0,
    val fileName: String,
    val importedAt: Long = System.currentTimeMillis(),
    val status: String, // e.g., "PROCESSING", "COMPLETED"
    val totalAddressCells: Int,
    val convertedCount: Int = 0,
    val failedCount: Int = 0,
    val inputFileUri: String? = null,
    val outputFileUri: String? = null
)
