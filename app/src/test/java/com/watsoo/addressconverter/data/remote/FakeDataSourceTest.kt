package com.watsoo.addressconverter.data.remote

import com.google.common.truth.Truth.assertThat
import com.watsoo.addressconverter.config.AppConfig
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import java.util.UUID

class FakeDataSourceTest {

    @Before
    fun setup() {
        // Disable randomness for deterministic tests
        AppConfig.FAKE_FETCH_FAILURE_PERCENT = 0.0f
        AppConfig.FAKE_UPLOAD_FAILURE_PERCENT = 0.0f
    }

    @Test
    fun `fake fetch returns requested items`() = runBlocking {
        val dataSource = FakeAddressRemoteDataSource()
        val result = dataSource.fetchAddresses(10, null)

        assertThat(result).isInstanceOf(ApiResult.Success::class.java)
        val data = (result as ApiResult.Success).data
        assertThat(data.addresses).hasSize(10)
        assertThat(data.hasMore).isTrue()
    }

    @Test
    fun `fake upload accepts batch`() = runBlocking {
        val dataSource = FakeAddressRemoteDataSource()
        val request = UploadGeocodedBatchRequestDto(
            batchId = UUID.randomUUID().toString(),
            items = listOf(
                UploadGeocodedItemDto("s1", "A1", 1.0, 2.0, "SUCCESS", null)
            )
        )

        val result = dataSource.uploadGeocodedBatch(request)
        assertThat(result).isInstanceOf(ApiResult.Success::class.java)
        val resp = (result as ApiResult.Success).data
        assertThat(resp.acceptedCount).isEqualTo(1)
    }
}
