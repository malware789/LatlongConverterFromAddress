package com.watsoo.addressconverter.ui.excel

import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.watsoo.addressconverter.databinding.ActivityExcelConversionBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ExcelConversionActivity : AppCompatActivity() {

    private lateinit var binding: ActivityExcelConversionBinding
    private val viewModel: ExcelConversionViewModel by viewModels()

    private val importLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let {
            try {
                contentResolver.takePersistableUriPermission(it, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            val fileName = getFileName(it)
            viewModel.importExcel(it, fileName)
        }
    }

    private val exportLauncher = registerForActivityResult(ActivityResultContracts.CreateDocument("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")) { uri ->
        uri?.let { outputUri ->
            val job = viewModel.currentJob.value ?: return@let
            val inputUri = job.inputFileUri?.let { Uri.parse(it) } ?: return@let
            
            lifecycleScope.launch {
                try {
                    contentResolver.openOutputStream(outputUri)?.use { os ->
                        viewModel.exportExcel(inputUri, os)
                        Toast.makeText(this@ExcelConversionActivity, "Export successful!", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(this@ExcelConversionActivity, "Export failed: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityExcelConversionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnImportExcel.setOnClickListener {
            importLauncher.launch(arrayOf("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "application/vnd.ms-excel"))
        }

        binding.btnStartWork.setOnClickListener {
            viewModel.startWork()
            Toast.makeText(this, "Worker started", Toast.LENGTH_SHORT).show()
        }

        binding.btnCancelWork.setOnClickListener {
            viewModel.cancelWork()
            Toast.makeText(this, "Worker cancelled", Toast.LENGTH_SHORT).show()
        }

        binding.btnExportExcel.setOnClickListener {
            val job = viewModel.currentJob.value ?: return@setOnClickListener
            exportLauncher.launch("Converted_${job.fileName}")
        }

        binding.btnClearJob.setOnClickListener {
            viewModel.clearJob()
            Toast.makeText(this, "Job cleared", Toast.LENGTH_SHORT).show()
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.currentJob.collectLatest { job ->
                        if (job != null) {
                            binding.tvFileName.text = "File: ${job.fileName}"
                            binding.tvStatus.text = "Status: ${job.status}"
                            binding.tvTotalCount.text = job.totalAddressCells.toString()
                            binding.tvGeocodedCount.text = job.convertedCount.toString()
                            binding.tvFailedCount.text = job.failedCount.toString()
                            
                            binding.btnStartWork.isEnabled = job.status == "PENDING" || job.status == "PROCESSING" || job.status == "IMPORTING"
                            binding.btnExportExcel.isEnabled = job.convertedCount > 0
                        } else {
                            binding.tvFileName.text = "File: None selected"
                            binding.tvStatus.text = "Status: Idle"
                            binding.tvTotalCount.text = "0"
                            binding.tvGeocodedCount.text = "0"
                            binding.tvFailedCount.text = "0"
                            
                            binding.btnStartWork.isEnabled = false
                            binding.btnExportExcel.isEnabled = false
                        }
                    }
                }
                launch {
                    viewModel.pendingCount.collectLatest { count ->
                        binding.tvPendingCount.text = count.toString()
                    }
                }
            }
        }
    }

    private fun getFileName(uri: Uri): String {
        var name = "unknown"
        contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (index != -1) name = cursor.getString(index)
            }
        }
        return name
    }
}
