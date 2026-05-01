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
import com.watsoo.addressconverter.data.local.WorkerLogEntity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class WorkerLogAdapter : ListAdapter<WorkerLogEntity, WorkerLogAdapter.ViewHolder>(DIFF) {

    private val sdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault())

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_log, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvTimestamp: TextView = itemView.findViewById(R.id.tvLogTimestamp)
        private val tvLevel: TextView = itemView.findViewById(R.id.tvLogLevel)
        private val tvMessage: TextView = itemView.findViewById(R.id.tvLogMessage)

        fun bind(log: WorkerLogEntity) {
            tvTimestamp.text = sdf.format(Date(log.timestamp))
            tvLevel.text = log.level
            tvLevel.setTextColor(levelColor(log.level))
            tvMessage.text = log.message
        }

        private fun levelColor(level: String): Int = when (level) {
            "WARNING" -> Color.parseColor("#F57C00")
            "ERROR" -> Color.parseColor("#C62828")
            else -> Color.parseColor("#1976D2")
        }
    }

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<WorkerLogEntity>() {
            override fun areItemsTheSame(a: WorkerLogEntity, b: WorkerLogEntity) = a.id == b.id
            override fun areContentsTheSame(a: WorkerLogEntity, b: WorkerLogEntity) = a == b
        }
    }
}
