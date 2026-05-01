package com.watsoo.addressconverter.data.local

enum class ExcelStatus {
    PENDING_GEOCODE,
    GEOCODING,
    GEOCODED,
    FAILED_TEMP,
    FAILED_PERM,
    EXPORTED
}
