package com.watsoo.addressconverter.ui

import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.watsoo.addressconverter.config.AppConfig
import com.watsoo.addressconverter.data.local.WorkerLogDao
import com.watsoo.addressconverter.databinding.ActivityMainBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val viewModel: MainViewModel by viewModels()
    private lateinit var logAdapter: WorkerLogAdapter

    @Inject
    lateinit var workerLogDao: WorkerLogDao

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        requestNotificationPermission()
        setupLogRecycler()
        setupButtons()
        observeViewModel()
        observeWorkManager()
    }

    private fun requestNotificationPermission() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 101)
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 101) {
            if (grantResults.isNotEmpty() && grantResults[0] != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                binding.tvProgressMsg.text = "Notification permission denied. Progress won't show in status bar."
            }
        }
    }

    private fun setupLogRecycler() {
        logAdapter = WorkerLogAdapter()
        binding.recyclerLogs.apply {
            layoutManager = LinearLayoutManager(this@MainActivity).also { it.stackFromEnd = true }
            adapter = logAdapter
        }
    }

    private fun setupButtons() {
        binding.btnStart.setOnClickListener { viewModel.startWork() }

        binding.btnCancel.setOnClickListener { viewModel.cancelWork() }

        binding.btnReset.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Reset All Data")
                .setMessage("Cancel background work and delete all local address data?")
                .setPositiveButton("Yes") { _, _ -> viewModel.resetLocalData() }
                .setNegativeButton("No", null)
                .show()
        }

        binding.btnRetryFailed.setOnClickListener {
            viewModel.retryTemporaryFailures()
        }

        binding.btnViewAddresses.setOnClickListener {
            startActivity(Intent(this, AddressListActivity::class.java))
        }

        binding.btnManualImport.setOnClickListener {
            showImportDialog()
        }

        binding.btnGoToExcel.setOnClickListener {
            startActivity(Intent(this, com.watsoo.addressconverter.ui.excel.ExcelConversionActivity::class.java))
        }

        binding.btnClearLogs.setOnClickListener {
            lifecycleScope.launch {
                workerLogDao.deleteAll()
            }
        }
    }

    private fun showImportDialog() {
        val input = android.widget.EditText(this).apply {
            hint = "Format: ID | Address\nOne per line..."
            gravity = android.view.Gravity.TOP
            setLines(5)
        }

        AlertDialog.Builder(this)
            .setTitle("Manual Import")
            .setMessage("Paste addresses below. Format: ID | Address or just Address.")
            .setView(input)
            .setPositiveButton("Import") { _, _ ->
                val text = input.text.toString()
                if (text.isNotBlank()) {
                    viewModel.importManualAddresses(text)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.totalCount.collect {
                        binding.tvTotalCount.text = "Total: $it"
                    }
                }
                launch {
                    viewModel.geocoderMode.collect { mode ->
                        binding.tvGeocoderMode.text = "Geocoder: $mode"
                        binding.tvGeocoderMode.setTextColor(
                            if (mode == AppConfig.GeocoderMode.FAKE)
                                android.graphics.Color.parseColor("#388E3C") // Green
                            else
                                android.graphics.Color.parseColor("#1976D2") // Blue
                        )
                    }
                }
                launch {
                    viewModel.pendingCount.collect {
                        binding.tvPendingCount.text = "Pending Geocode: $it"
                    }
                }
                launch {
                    viewModel.geocodingCount.collect {
                        binding.tvGeocodingCount.text = "Geocoding: $it"
                    }
                }
                launch {
                    viewModel.geocodedPendingUploadCount.collect {
                        binding.tvGeocodedPendingUploadCount.text = "Geocoded Pending Upload: $it"
                    }
                }
                launch {
                    viewModel.uploadingCount.collect {
                        binding.tvUploadingCount.text = "Uploading: $it"
                    }
                }
                launch {
                    viewModel.sentCount.collect {
                        binding.tvSentCount.text = "Sent: $it"
                    }
                }
                launch {
                    viewModel.tempFailedCount.collect {
                        binding.tvTempFailedCount.text = "Temp Failed: $it"
                    }
                }
                launch {
                    viewModel.permFailedCount.collect {
                        binding.tvPermFailedCount.text = "Perm Failed: $it"
                    }
                }
                launch {
                    viewModel.recentLogs.collect { logs ->
                        logAdapter.submitList(logs)
                        if (logs.isNotEmpty()) {
                            binding.recyclerLogs.scrollToPosition(logs.size - 1)
                        }
                    }
                }
            }
        }
    }

    private fun observeWorkManager() {
        WorkManager.getInstance(this)
            .getWorkInfosForUniqueWorkLiveData("AddressConvertWork")
            .observe(this) { workInfos ->
                if (workInfos.isNullOrEmpty()) {
                    binding.tvProgressMsg.text = "Worker: Idle"
                    return@observe
                }
                val info = workInfos[0]
                val stage = info.progress.getString("stage") ?: "—"
                val msg = info.progress.getString("progress_msg") ?: "—"
                val processed = info.progress.getInt("processed_count", 0)
                val total = info.progress.getInt("total_count", 0)

                val statusText = if (total > 0) {
                    "Worker: ${info.state.name} | $stage | $msg ($processed/$total)"
                } else {
                    "Worker: ${info.state.name} | $stage | $msg"
                }

                binding.tvProgressMsg.text = statusText
                if (info.state == WorkInfo.State.FAILED) {
                    val error = info.outputData.getString("errorMessage") ?: "Unknown error"
                    val stage = info.outputData.getString("failedStage") ?: "unknown"
                    val clazz = info.outputData.getString("errorClass") ?: ""
                    binding.tvProgressMsg.text = "Worker: FAILED at $stage | $error ($clazz)"
                    binding.tvProgressMsg.setTextColor(android.graphics.Color.RED)
                } else {
                    binding.tvProgressMsg.setTextColor(android.graphics.Color.DKGRAY)
                }
            }
    }
}
