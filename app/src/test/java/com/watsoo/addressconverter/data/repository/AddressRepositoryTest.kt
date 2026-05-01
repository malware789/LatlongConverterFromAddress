package com.watsoo.addressconverter.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.watsoo.addressconverter.config.AppConfig
import com.watsoo.addressconverter.data.local.AddressDao
import com.watsoo.addressconverter.data.local.AddressEntity
import com.watsoo.addressconverter.data.local.AddressStatus
import com.watsoo.addressconverter.data.local.AppDatabase
import com.watsoo.addressconverter.data.remote.AddressDto
import com.watsoo.addressconverter.data.remote.AddressRemoteDataSource
import com.watsoo.addressconverter.data.remote.ApiResult
import com.watsoo.addressconverter.data.remote.FetchAddressesResponseDto
import com.watsoo.addressconverter.data.remote.UploadGeocodedBatchResponseDto
import com.watsoo.addressconverter.geocode.GeocodeResult
import com.watsoo.addressconverter.geocode.GeocoderClient
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.IOException

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AddressRepositoryTest {

    private lateinit var database: AppDatabase
    private lateinit var dao: AddressDao
    private lateinit var remoteDataSource: AddressRemoteDataSource
    private lateinit var geocoderClient: GeocoderClient
    private lateinit var repository: AddressRepository

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = database.addressDao()
        remoteDataSource = mockk()
        geocoderClient = mockk()
        repository = AddressRepository(dao, remoteDataSource, geocoderClient)
    }

    @After
    fun teardown() {
        database.close()
    }

    @Test
    fun `fetchAndSaveAddresses inserts new addresses`() = runBlocking {
        val response = FetchAddressesResponseDto(
            addresses = listOf(AddressDto("s1", "Addr 1")),
            nextCursor = null,
            hasMore = false
        )
        coEvery { remoteDataSource.fetchAddresses(any(), any()) } returns ApiResult.Success(response)

        repository.fetchAndSaveAddresses()

        val all = dao.getAllAddressesFlow().first()
        assertThat(all).hasSize(1)
        assertThat(all[0].serverId).isEqualTo("s1")
    }

    @Test
    fun `processNextBatch success marks as GEOCODED`() = runBlocking {
        dao.insertAddresses(listOf(AddressEntity(serverId = "s1", address = "A1", normalizedAddress = null, latitude = null, longitude = null)))
        coEvery { geocoderClient.geocode(any()) } returns GeocodeResult(1.0, 2.0, "Norm A1")

        val result = repository.processNextBatch()

        assertThat(result).isTrue()
        val item = dao.getAllAddressesFlow().first()[0]
        assertThat(item.status).isEqualTo(AddressStatus.GEOCODED_PENDING_UPLOAD.name)
        assertThat(item.latitude).isEqualTo(1.0)
    }

    @Test
    fun `processNextBatch temp failure marks as FAILED_TEMP`() = runBlocking {
        dao.insertAddresses(listOf(AddressEntity(serverId = "s1", address = "A1", normalizedAddress = null, latitude = null, longitude = null)))
        coEvery { geocoderClient.geocode(any()) } throws IOException("Timeout")

        repository.processNextBatch()

        val item = dao.getAllAddressesFlow().first()[0]
        assertThat(item.status).isEqualTo(AddressStatus.FAILED_TEMP.name)
        assertThat(item.geocodeAttempts).isEqualTo(1)
        assertThat(item.nextRetryAt).isGreaterThan(System.currentTimeMillis())
    }

    @Test
    fun `processNextBatch max retries marks as FAILED_PERM`() = runBlocking {
        val item = AddressEntity(
            serverId = "s1", 
            address = "A1", 
            normalizedAddress = null, 
            latitude = null, 
            longitude = null, 
            geocodeAttempts = AppConfig.MAX_GEOCODE_ATTEMPTS
        )
        dao.insertAddresses(listOf(item))
        
        coEvery { geocoderClient.geocode(any()) } throws IOException("Final Timeout")

        repository.processNextBatch()

        val updated = dao.getAllAddressesFlow().first()[0]
        assertThat(updated.status).isEqualTo(AddressStatus.FAILED_PERM.name)
    }

    @Test
    fun `uploadCompletedBatch success marks as SENT`() = runBlocking {
        val item = AddressEntity(
            serverId = "s1", 
            address = "A1", 
            latitude = 1.0, 
            longitude = 2.0, 
            normalizedAddress = "Norm",
            status = AddressStatus.GEOCODED_PENDING_UPLOAD.name
        )
        dao.insertAddresses(listOf(item))
        coEvery { remoteDataSource.uploadGeocodedBatch(any()) } returns ApiResult.Success(
            UploadGeocodedBatchResponseDto(true, 1, "OK", emptyList())
        )

        val result = repository.uploadCompletedBatch()

        assertThat(result).isTrue()
        val updated = dao.getAllAddressesFlow().first()[0]
        assertThat(updated.status).isEqualTo(AddressStatus.SENT.name)
    }
}
