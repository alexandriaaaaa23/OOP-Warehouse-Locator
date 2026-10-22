package com.example.oop_warehouse_location.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.oop_warehouse_location.R
import com.example.oop_warehouse_location.models.Parcel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ParcelAdapter : RecyclerView.Adapter<ParcelAdapter.ParcelViewHolder>() {

    private val parcels = mutableListOf<Parcel>()
    private val dateFormat = SimpleDateFormat("MMM d, yyyy — h:mm a", Locale.getDefault())

    fun submitList(items: List<Parcel>) {
        parcels.clear()
        parcels.addAll(items)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ParcelViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_parcel, parent, false)
        return ParcelViewHolder(view)
    }

    override fun onBindViewHolder(holder: ParcelViewHolder, position: Int) {
        holder.bind(parcels[position])
    }

    override fun getItemCount(): Int = parcels.size

    inner class ParcelViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvTrackingNumber: TextView = itemView.findViewById(R.id.tvTrackingNumber)
        private val tvCarrierBadge: TextView = itemView.findViewById(R.id.tvCarrierBadge)
        private val tvZoneBadge: TextView = itemView.findViewById(R.id.tvZoneBadge)
        private val tvBinInfo: TextView = itemView.findViewById(R.id.tvBinInfo)
        private val tvRecipient: TextView = itemView.findViewById(R.id.tvRecipient)
        private val tvStatus: TextView = itemView.findViewById(R.id.tvStatus)
        private val tvTimestamp: TextView = itemView.findViewById(R.id.tvTimestamp)

        fun bind(parcel: Parcel) {
            tvTrackingNumber.text = parcel.parcelID
            tvCarrierBadge.text = parcel.carrier
            tvZoneBadge.text = parcel.zone
            tvBinInfo.text = parcel.binCoordinate.ifBlank { parcel.rack }
            tvRecipient.text = parcel.receiptName
            tvStatus.text = parcel.status
            tvTimestamp.text = if (parcel.createdAt > 0) {
                dateFormat.format(Date(parcel.createdAt))
            } else {
                ""
            }
        }
    }
}
