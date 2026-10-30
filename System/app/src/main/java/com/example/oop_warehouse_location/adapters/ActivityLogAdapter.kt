package com.example.oop_warehouse_location.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.oop_warehouse_location.R
import com.example.oop_warehouse_location.models.ActivityLog
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ActivityLogAdapter : RecyclerView.Adapter<ActivityLogAdapter.LogViewHolder>() {

    private val logs = mutableListOf<ActivityLog>()
    private val userNames = mutableMapOf<String, String>()
    private val dateFormat = SimpleDateFormat("MMM d, yyyy · h:mm a", Locale.getDefault())

    fun submitList(items: List<ActivityLog>, usersMap: Map<String, String> = emptyMap()) {
        logs.clear()
        logs.addAll(items)
        if (usersMap.isNotEmpty()) {
            userNames.putAll(usersMap)
        }
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LogViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_activity_log, parent, false)
        return LogViewHolder(view)
    }

    override fun onBindViewHolder(holder: LogViewHolder, position: Int) {
        holder.bind(logs[position])
    }

    override fun getItemCount(): Int = logs.size

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

            tvLogTime.text = if (log.timestamp > 0) {
                dateFormat.format(Date(log.timestamp))
            } else {
                ""
            }
        }
    }
}
