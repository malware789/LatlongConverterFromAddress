package com.watsoo.addressconverter.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AddressDaoTest {

    private lateinit var database: AppDatabase
    private lateinit var dao: AddressDao

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = database.addressDao()
    }

    @After
    fun teardown() {
        database.close()
    }

    @Test
    fun `insert and retrieve address`() = runBlocking {
        val entity = AddressEntity(serverId = "s1", address = "Address 1", normalizedAddress = null, latitude = null, longitude = null)
        dao.insertAddresses(listOf(entity))

        val all = dao.getAllAddressesFlow().first()
        assertThat(all).hasSize(1)
        assertThat(all[0].serverId).isEqualTo("s1")
    }

    @Test
    fun `duplicate serverId ignore strategy`() = runBlocking {
        val entity1 = AddressEntity(serverId = "s1", address = "Address 1", normalizedAddress = null, latitude = null, longitude = null)
        val entity2 = AddressEntity(serverId = "s1", address = "Address 1 Modified", normalizedAddress = null, latitude = null, longitude = null)

        dao.insertAddresses(listOf(entity1))
        dao.insertAddresses(listOf(entity2))

        val all = dao.getAllAddressesFlow().first()
        assertThat(all).hasSize(1)
        assertThat(all[0].address).isEqualTo("Address 1") // Ignored
    }

    @Test
    fun `mark as geocoding locks rows`() = runBlocking {
        val entity = AddressEntity(serverId = "s1", address = "Address 1", normalizedAddress = null, latitude = null, longitude = null)
        dao.insertAddresses(listOf(entity))
        
        val list = dao.getPendingAddresses(10)
        dao.markAsGeocoding(list.map { it.localId })

        val updated = dao.getPendingAddresses(10)
        assertThat(updated).isEmpty() // Locked rows should not be in pending
    }

    @Test
    fun `mark as geocoded moves to pending upload`() = runBlocking {
        val entity = AddressEntity(serverId = "s1", address = "Address 1", normalizedAddress = null, latitude = null, longitude = null)
        dao.insertAddresses(listOf(entity))
        
        val item = dao.getPendingAddresses(1).first()
        dao.markAsGeocoded(item.localId, 1.0, 2.0, "Norm 1")

        val readyToUpload = dao.getPendingUploadAddresses(10)
        assertThat(readyToUpload).hasSize(1)
        assertThat(readyToUpload[0].status).isEqualTo(AddressStatus.GEOCODED_PENDING_UPLOAD.name)
    }

    @Test
    fun `clear data works`() = runBlocking {
        dao.insertAddresses(listOf(AddressEntity(serverId = "s1", address = "A1", normalizedAddress = null, latitude = null, longitude = null)))
        dao.deleteAll()
        
        val all = dao.getAllAddressesFlow().first()
        assertThat(all).isEmpty()
    }
}
