package com.watsoo.addressconverter.data.mapper

import com.google.common.truth.Truth.assertThat
import com.watsoo.addressconverter.data.local.AddressStatus
import com.watsoo.addressconverter.data.remote.AddressDto
import org.junit.Test

class AddressMappersTest {

    @Test
    fun `AddressDto to AddressEntity mapping`() {
        val dto = AddressDto(
            serverId = "id_1",
            address = "Test Address"
        )

        val entity = dto.toEntity()

        assertThat(entity.serverId).isEqualTo("id_1")
        assertThat(entity.address).isEqualTo("Test Address")
        assertThat(entity.status).isEqualTo(AddressStatus.PENDING_GEOCODE.name)
        assertThat(entity.latitude).isNull()
        assertThat(entity.longitude).isNull()
        assertThat(entity.normalizedAddress).isNull()
    }

    @Test
    fun `AddressEntity to UploadItemDto mapping success`() {
        val entity = com.watsoo.addressconverter.data.local.AddressEntity(
            serverId = "s_1",
            address = "Orig Address",
            latitude = 12.34,
            longitude = 56.78,
            normalizedAddress = "Normalized Address",
            status = AddressStatus.GEOCODED_PENDING_UPLOAD.name
        )

        val uploadDto = entity.toUploadItemDto()

        assertThat(uploadDto.id).isEqualTo("s_1")
        assertThat(uploadDto.latitude).isEqualTo(12.34)
        assertThat(uploadDto.longitude).isEqualTo(56.78)
        assertThat(uploadDto.address).isEqualTo("Orig Address")
        assertThat(uploadDto.geocodeStatus).isEqualTo("SUCCESS")
        assertThat(uploadDto.errorMessage).isNull()
    }

    @Test
    fun `AddressEntity to UploadItemDto mapping failure`() {
        val entity = com.watsoo.addressconverter.data.local.AddressEntity(
            serverId = "s_fail",
            address = "Fail Address",
            normalizedAddress = null,
            latitude = null,
            longitude = null,
            status = AddressStatus.FAILED_PERM.name,
            lastError = "Zero results"
        )

        val uploadDto = entity.toUploadItemDto()

        assertThat(uploadDto.id).isEqualTo("s_fail")
        assertThat(uploadDto.latitude).isNull()
        assertThat(uploadDto.longitude).isNull()
        assertThat(uploadDto.geocodeStatus).isEqualTo("FAILED_PERM")
        assertThat(uploadDto.errorMessage).isEqualTo("Zero results")
    }
}
