package com.watsoo.addressconverter.ui

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.watsoo.addressconverter.R
import com.watsoo.addressconverter.data.local.AddressEntity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AddressItemAdapter : ListAdapter<AddressEntity, AddressItemAdapter.ViewHolder>(DIFF) {

    private val sdf = SimpleDateFormat("MM-dd HH:mm:ss", Locale.getDefault())

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_address, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvServerId: TextView = itemView.findViewById(R.id.tvServerId)
        private val tvAddress: TextView = itemView.findViewById(R.id.tvAddress)
        private val tvStatus: TextView = itemView.findViewById(R.id.tvStatus)
        private val tvAttempts: TextView = itemView.findViewById(R.id.tvAttempts)
        private val tvLatLng: TextView = itemView.findViewById(R.id.tvLatLng)
        private val tvLastError: TextView = itemView.findViewById(R.id.tvLastError)
        private val tvUpdatedAt: TextView = itemView.findViewById(R.id.tvUpdatedAt)

        fun bind(entity: AddressEntity) {
            tvServerId.text = "ID: ${entity.serverId}"
            tvAddress.text = entity.address
            tvStatus.text = entity.status
            tvStatus.setTextColor(statusColor(entity.status))
            tvAttempts.text = "Geo: ${entity.geocodeAttempts} | Upload: ${entity.uploadAttempts}"

            if (entity.latitude != null && entity.longitude != null) {
                tvLatLng.text = "Lat: ${"%.5f".format(entity.latitude)}  Lng: ${"%.5f".format(entity.longitude)}"
                tvLatLng.visibility = View.VISIBLE
            } else {
                tvLatLng.visibility = View.GONE
            }

            if (!entity.lastError.isNullOrBlank()) {
                tvLastError.text = "Error: ${entity.lastError}"
                tvLastError.visibility = View.VISIBLE
            } else {
                tvLastError.visibility = View.GONE
            }

            tvUpdatedAt.text = sdf.format(Date(entity.updatedAt))
        }

        private fun statusColor(status: String): Int = when (status) {
            "PENDING_GEOCODE" -> Color.parseColor("#757575")
            "GEOCODING" -> Color.parseColor("#1976D2")
            "GEOCODED_PENDING_UPLOAD" -> Color.parseColor("#388E3C")
            "UPLOADING" -> Color.parseColor("#F57C00")
            "SENT" -> Color.parseColor("#2E7D32")
            "FAILED_TEMP" -> Color.parseColor("#F9A825")
            "FAILED_PERM" -> Color.parseColor("#C62828")
            else -> Color.DKGRAY
        }
    }

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<AddressEntity>() {
            override fun areItemsTheSame(a: AddressEntity, b: AddressEntity) = a.localId == b.localId
            override fun areContentsTheSame(a: AddressEntity, b: AddressEntity) = a == b
        }
    }
}
