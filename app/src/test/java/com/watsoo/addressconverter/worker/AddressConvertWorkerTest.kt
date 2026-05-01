package com.watsoo.addressconverter.worker

import android.app.Notification
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.work.ListenableWorker
import androidx.work.WorkerParameters
import androidx.work.testing.TestListenableWorkerBuilder
import com.google.common.truth.Truth.assertThat
import com.watsoo.addressconverter.data.local.WorkerLogDao
import com.watsoo.addressconverter.data.repository.AddressRepository
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkAll
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AddressConvertWorkerTest {

    private lateinit var context: Context
    private lateinit var repository: AddressRepository
    private lateinit var workerLogDao: WorkerLogDao

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        repository = mockk()
        workerLogDao = mockk(relaxed = true)
        
        // Mock NotificationHelper to avoid resource errors in Robolectric
        mockkObject(NotificationHelper)
        coEvery { 
            NotificationHelper.createNotification(any(), any(), any(), any(), any()) 
        } returns mockk<Notification>(relaxed = true)
    }

    @After
    fun teardown() {
        unmockkAll()
    }

    @Test
    fun `worker returns success when no work remains`() = runBlocking {
        coEvery { repository.releaseAllStaleLocks() } returns Unit
        coEvery { repository.fetchAndSaveAddresses() } returns Unit
        coEvery { repository.processNextBatch() } returns false
        coEvery { repository.uploadCompletedBatch() } returns false
        coEvery { repository.getCountTotal() } returns 0
        coEvery { repository.getCountByStatus(any()) } returns 0
        
        val worker = TestListenableWorkerBuilder<AddressConvertWorker>(context)
            .setWorkerFactory(object : androidx.work.WorkerFactory() {
                override fun createWorker(
                    appContext: Context,
                    workerClassName: String,
                    workerParameters: WorkerParameters
                ): ListenableWorker? {
                    return AddressConvertWorker(appContext, workerParameters, repository, workerLogDao)
                }
            })
            .build()

        val result = worker.doWork()
        assertThat(result).isEqualTo(ListenableWorker.Result.success())
    }
}
