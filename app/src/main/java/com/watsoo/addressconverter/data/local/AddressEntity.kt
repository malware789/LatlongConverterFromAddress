package com.watsoo.addressconverter.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "addresses",
    indices = [Index(value = ["serverId"], unique = true)]
)
data class AddressEntity(
    @PrimaryKey(autoGenerate = true)
    val localId: Long = 0,
    val serverId: String,
    val address: String,
    val normalizedAddress: String?,
    val latitude: Double?,
    val longitude: Double?,
    val status: String = AddressStatus.PENDING_GEOCODE.name,
    val geocodeAttempts: Int = 0,
    val uploadAttempts: Int = 0,
    val nextRetryAt: Long = 0,
    val lockedAt: Long = 0,
    val lastError: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
