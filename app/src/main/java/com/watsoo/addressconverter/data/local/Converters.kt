package com.watsoo.addressconverter.data.local

import androidx.room.TypeConverter

class Converters {
    @TypeConverter
    fun fromExcelStatus(status: ExcelStatus): String {
        return status.name
    }

    @TypeConverter
    fun toExcelStatus(status: String): ExcelStatus {
        return ExcelStatus.valueOf(status)
    }
}
