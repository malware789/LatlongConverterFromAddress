package com.watsoo.addressconverter.data.mapper

import com.watsoo.addressconverter.data.local.AddressEntity
import com.watsoo.addressconverter.data.remote.AddressDto
import com.watsoo.addressconverter.data.remote.UploadGeocodedItemDto

fun AddressDto.toEntity(): AddressEntity {
    return AddressEntity(
        serverId = this.serverId,
        address = this.address,
        normalizedAddress = null,
        latitude = null,
        longitude = null
    )
}

fun AddressEntity.toUploadItemDto(): UploadGeocodedItemDto {
    val statusLabel = if (this.latitude != null && this.longitude != null) {
        "SUCCESS"
    } else {
        this.status // e.g. "FAILED_PERM" or "FAILED_TEMP"
    }

    return UploadGeocodedItemDto(
        id = this.serverId,
        address = this.address,
        latitude = this.latitude,
        longitude = this.longitude,
        geocodeStatus = statusLabel,
        errorMessage = this.lastError
    )
}
