package com.watsoo.addressconverter.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Index

@Entity(
    tableName = "excel_address_cells",
    indices = [
        Index(value = ["jobId"]),
        Index(value = ["status"])
    ]
)
data class ExcelAddressCellEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val jobId: Long,
    val sheetName: String,
    val rowIndex: Int,
    val columnIndex: Int,
    val dayName: String,
    val tankerNo: String,
    val fpl: String,
    val address: String,
    val normalizedAddress: String,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val latLongText: String? = null,
    val status: ExcelStatus = ExcelStatus.PENDING_GEOCODE,
    val attempts: Int = 0,
    val nextRetryAt: Long = 0,
    val lastError: String? = null,
    val updatedAt: Long = System.currentTimeMillis()
)
