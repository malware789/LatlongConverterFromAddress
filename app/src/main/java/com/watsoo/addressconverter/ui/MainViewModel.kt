package com.watsoo.addressconverter.ui

import android.app.Application
import dagger.hilt.android.lifecycle.HiltViewModel
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.watsoo.addressconverter.config.AppConfig
import com.watsoo.addressconverter.data.local.AddressStatus
import com.watsoo.addressconverter.data.local.AppDatabase
import com.watsoo.addressconverter.data.local.WorkerLogDao
import com.watsoo.addressconverter.data.local.WorkerLogEntity
import com.watsoo.addressconverter.data.local.WorkerLogger
import com.watsoo.addressconverter.data.repository.AddressRepository
import com.watsoo.addressconverter.worker.AddressWorkStarter
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    application: Application,
    private val repository: AddressRepository,
    private val workerLogDao: WorkerLogDao
) : AndroidViewModel(application) {

    // ── Count state flows ──────────────────────────────────────────────
    private val _totalCount = MutableStateFlow(0)
    val totalCount: StateFlow<Int> = _totalCount.asStateFlow()

    private val _pendingCount = MutableStateFlow(0)
    val pendingCount: StateFlow<Int> = _pendingCount.asStateFlow()

    private val _geocodingCount = MutableStateFlow(0)
    val geocodingCount: StateFlow<Int> = _geocodingCount.asStateFlow()

    private val _geocodedPendingUploadCount = MutableStateFlow(0)
    val geocodedPendingUploadCount: StateFlow<Int> = _geocodedPendingUploadCount.asStateFlow()

    private val _uploadingCount = MutableStateFlow(0)
    val uploadingCount: StateFlow<Int> = _uploadingCount.asStateFlow()

    private val _sentCount = MutableStateFlow(0)
    val sentCount: StateFlow<Int> = _sentCount.asStateFlow()

    private val _tempFailedCount = MutableStateFlow(0)
    val tempFailedCount: StateFlow<Int> = _tempFailedCount.asStateFlow()

    private val _permFailedCount = MutableStateFlow(0)
    val permFailedCount: StateFlow<Int> = _permFailedCount.asStateFlow()

    // ── Worker log state flow ─────────────────────────────────────────
    private val _recentLogs = MutableStateFlow<List<WorkerLogEntity>>(emptyList())
    val recentLogs: StateFlow<List<WorkerLogEntity>> = _recentLogs.asStateFlow()

    private val _geocoderMode = MutableStateFlow(AppConfig.GEOCODER_MODE)
    val geocoderMode: StateFlow<AppConfig.GeocoderMode> = _geocoderMode.asStateFlow()

    init {
        WorkerLogger.init(workerLogDao)
        startPolling()
    }

    private fun startPolling() {
        viewModelScope.launch {
            while (isActive) {
                _totalCount.value = repository.getCountTotal()
                _pendingCount.value = repository.getCountByStatus(AddressStatus.PENDING_GEOCODE)
                _geocodingCount.value = repository.getCountByStatus(AddressStatus.GEOCODING)
                _geocodedPendingUploadCount.value = repository.getCountByStatus(AddressStatus.GEOCODED_PENDING_UPLOAD)
                _uploadingCount.value = repository.getCountByStatus(AddressStatus.UPLOADING)
                _sentCount.value = repository.getCountByStatus(AddressStatus.SENT)
                _tempFailedCount.value = repository.getCountByStatus(AddressStatus.FAILED_TEMP)
                _permFailedCount.value = repository.getCountByStatus(AddressStatus.FAILED_PERM)
                _recentLogs.value = workerLogDao.getRecentLogs(limit = 50)
                
                // Keep UI geocoder mode in sync with AppConfig
                _geocoderMode.value = AppConfig.GEOCODER_MODE
                
                delay(AppConfig.UI_POLL_INTERVAL_MS)
            }
        }
    }

    // ── WorkManager actions ───────────────────────────────────────────
    fun startWork() {
        WorkerLogger.info("User triggered Start Work")
        AddressWorkStarter.startWork(getApplication())
    }

    fun cancelWork() {
        WorkerLogger.warning("User triggered Cancel Work")
        AddressWorkStarter.cancelWork(getApplication())
    }

    fun resetLocalData() {
        viewModelScope.launch {
            WorkerLogger.warning("User triggered Reset Local Data")
            AddressWorkStarter.cancelWork(getApplication())
            repository.resetLocalData()
        }
    }

    // ── Retry actions ─────────────────────────────────────────────────
    fun retryTemporaryFailures() {
        viewModelScope.launch { repository.retryTemporaryFailuresNow() }
    }

    fun retryPermanentFailures() {
        viewModelScope.launch { repository.retryPermanentFailures() }
    }

    fun importManualAddresses(rawInput: String) {
        viewModelScope.launch {
            repository.importManualAddresses(rawInput)
        }
    }
}
