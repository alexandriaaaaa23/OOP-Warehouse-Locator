package com.example.oop_warehouse_location.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.oop_warehouse_location.R
import com.example.oop_warehouse_location.adapters.base.BaseListAdapter
import com.example.oop_warehouse_location.models.ActivityLog
import com.example.oop_warehouse_location.utils.DateUtils

class ActivityLogAdapter : BaseListAdapter<ActivityLog, ActivityLogAdapter.LogViewHolder>() {

    private val userNames = mutableMapOf<String, String>()

    override fun submitList(newItems: List<ActivityLog>) {
        super.submitList(newItems)
    }

    fun submitList(newItems: List<ActivityLog>, usersMap: Map<String, String>) {
        if (usersMap.isNotEmpty()) {
            userNames.putAll(usersMap)
        }
        super.submitList(newItems)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LogViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_activity_log, parent, false)
        return LogViewHolder(view)
    }

    override fun onBindViewHolder(holder: LogViewHolder, position: Int) {
        holder.bind(items[position])
    }

    inner class LogViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvLogAction: TextView = itemView.findViewById(R.id.tvLogAction)
        private val tvLogParcel: TextView = itemView.findViewById(R.id.tvLogParcel)
        private val tvLogUser: TextView = itemView.findViewById(R.id.tvLogUser)
        private val tvLogTime: TextView = itemView.findViewById(R.id.tvLogTime)

        fun bind(log: ActivityLog) {
            tvLogAction.text = log.action.ifBlank { "Parcel updated" }
            tvLogParcel.text = if (log.parcelId.isNotBlank()) "Tracking: ${log.parcelId}" else ""
            tvLogParcel.visibility = if (log.parcelId.isNotBlank()) View.VISIBLE else View.GONE

            val userName = userNames[log.userId] ?: if (log.userId.isNotBlank()) "User ID: ${log.userId.take(8)}..." else "System"
            tvLogUser.text = "Logged by: $userName"

            tvLogTime.text = DateUtils.formatLogTimestamp(log.timestamp)
        }
    }
}
