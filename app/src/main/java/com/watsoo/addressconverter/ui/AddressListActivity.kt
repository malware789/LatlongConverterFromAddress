package com.watsoo.addressconverter.ui

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.watsoo.addressconverter.R
import com.watsoo.addressconverter.data.local.AddressStatus
import com.watsoo.addressconverter.data.repository.AddressRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class AddressListActivity : AppCompatActivity() {

    @Inject
    lateinit var repository: AddressRepository
    
    private lateinit var adapter: AddressItemAdapter
    private lateinit var tvListCount: TextView
    private lateinit var retryActionsBar: View

    private var activeFilter: AddressStatus? = null // null = All

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_address_list)

        supportActionBar?.title = "Address Inspector"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        adapter = AddressItemAdapter()
        tvListCount = findViewById(R.id.tvListCount)
        retryActionsBar = findViewById(R.id.retryActionsBar)

        val recycler = findViewById<RecyclerView>(R.id.recyclerAddresses)
        recycler.layoutManager = LinearLayoutManager(this)
        recycler.adapter = adapter

        setupFilterButtons()
        setupRetryButtons()
        startPolling()
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }

    private fun setupFilterButtons() {
        fun filter(status: AddressStatus?) {
            activeFilter = status
            val showActions = status == AddressStatus.FAILED_TEMP || status == AddressStatus.FAILED_PERM
            retryActionsBar.visibility = if (showActions) View.VISIBLE else View.GONE
        }

        findViewById<Button>(R.id.btnFilterAll).setOnClickListener { filter(null) }
        findViewById<Button>(R.id.btnFilterPending).setOnClickListener { filter(AddressStatus.PENDING_GEOCODE) }
        findViewById<Button>(R.id.btnFilterGeocoding).setOnClickListener { filter(AddressStatus.GEOCODING) }
        findViewById<Button>(R.id.btnFilterGeocoded).setOnClickListener { filter(AddressStatus.GEOCODED_PENDING_UPLOAD) }
        findViewById<Button>(R.id.btnFilterUploading).setOnClickListener { filter(AddressStatus.UPLOADING) }
        findViewById<Button>(R.id.btnFilterSent).setOnClickListener { filter(AddressStatus.SENT) }
        findViewById<Button>(R.id.btnFilterFailedTemp).setOnClickListener { filter(AddressStatus.FAILED_TEMP) }
        findViewById<Button>(R.id.btnFilterFailedPerm).setOnClickListener { filter(AddressStatus.FAILED_PERM) }
    }

    private fun setupRetryButtons() {
        findViewById<Button>(R.id.btnRetryTemp).setOnClickListener {
            lifecycleScope.launch {
                val count = repository.retryTemporaryFailuresNow()
                showToast("Reset $count temp-failed rows to PENDING_GEOCODE")
            }
        }

        findViewById<Button>(R.id.btnRetryPerm).setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Retry Permanent Failures")
                .setMessage("This will reset ALL permanently-failed rows to PENDING_GEOCODE and clear their attempt counters. Continue?")
                .setPositiveButton("Yes") { _, _ ->
                    lifecycleScope.launch {
                        val count = repository.retryPermanentFailures()
                        showToast("Reset $count perm-failed rows to PENDING_GEOCODE")
                    }
                }
                .setNegativeButton("No", null)
                .show()
        }

        findViewById<Button>(R.id.btnClearPerm).setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Delete Permanent Failures")
                .setMessage("This will permanently DELETE all FAILED_PERM rows. This cannot be undone. Continue?")
                .setPositiveButton("Delete") { _, _ ->
                    lifecycleScope.launch {
                        val count = repository.clearPermanentFailures()
                        showToast("Deleted $count FAILED_PERM rows")
                    }
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }

    private fun startPolling() {
        lifecycleScope.launch {
            while (isActive) {
                val items = repository.getAddressesByStatus(activeFilter, limit = 200)
                adapter.submitList(items)
                val filterLabel = activeFilter?.name ?: "All"
                tvListCount.text = "[$filterLabel] Showing ${items.size} items"
                delay(2000)
            }
        }
    }

    private fun showToast(msg: String) {
        android.widget.Toast.makeText(this, msg, android.widget.Toast.LENGTH_SHORT).show()
    }
}
