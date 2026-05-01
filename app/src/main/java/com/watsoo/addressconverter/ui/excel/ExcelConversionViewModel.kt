package com.watsoo.addressconverter.ui.excel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.watsoo.addressconverter.data.local.ExcelImportJobEntity
import com.watsoo.addressconverter.data.local.ExcelStatus
import com.watsoo.addressconverter.data.repository.ExcelRepository
import com.watsoo.addressconverter.worker.AddressWorkStarter
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.io.OutputStream
import javax.inject.Inject

@HiltViewModel
class ExcelConversionViewModel @Inject constructor(
    application: Application,
    private val repository: ExcelRepository
) : AndroidViewModel(application) {

    private val _currentJob = MutableStateFlow<ExcelImportJobEntity?>(null)
    val currentJob: StateFlow<ExcelImportJobEntity?> = _currentJob

    private val _pendingCount = MutableStateFlow(0)
    val pendingCount: StateFlow<Int> = _pendingCount

    init {
        viewModelScope.launch {
            repository.getAllJobsFlow().collect { jobs ->
                if (jobs.isNotEmpty()) {
                    val job = jobs.first()
                    _currentJob.value = job
                    _pendingCount.value = repository.countCellsByStatus(job.jobId, ExcelStatus.PENDING_GEOCODE) +
                                         repository.countCellsByStatus(job.jobId, ExcelStatus.FAILED_TEMP)
                } else {
                    _currentJob.value = null
                    _pendingCount.value = 0
                }
            }
        }
    }

    fun importExcel(uri: Uri, fileName: String) {
        viewModelScope.launch {
            try {
                repository.importExcel(uri, fileName)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun startWork() {
        val jobId = _currentJob.value?.jobId ?: return
        AddressWorkStarter.startExcelWork(getApplication(), jobId)
    }

    fun cancelWork() {
        val jobId = _currentJob.value?.jobId ?: return
        AddressWorkStarter.cancelExcelWork(getApplication(), jobId)
    }

    suspend fun exportExcel(inputUri: Uri, outputStream: OutputStream) {
        val jobId = _currentJob.value?.jobId ?: throw Exception("No active job")
        repository.exportJob(jobId, inputUri, outputStream)
    }

    fun clearJob() {
        val jobId = _currentJob.value?.jobId ?: return
        viewModelScope.launch {
            repository.deleteJob(jobId)
        }
    }
}
