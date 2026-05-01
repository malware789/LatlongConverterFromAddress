package com.watsoo.addressconverter.data.local

enum class AddressStatus {
    PENDING_GEOCODE,
    GEOCODING,
    GEOCODED_PENDING_UPLOAD,
    UPLOADING,
    SENT,
    FAILED_TEMP,
    FAILED_PERM
}
