package com.example.oop_warehouse_location.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.oop_warehouse_location.R
import com.example.oop_warehouse_location.models.Parcel
import com.google.android.material.button.MaterialButton
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ParcelAdapter(
    private val onItemClicked: (Parcel) -> Unit = {},
    private val onShipClicked: (Parcel) -> Unit = {}
) : RecyclerView.Adapter<ParcelAdapter.ParcelViewHolder>() {

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
        private val btnShipOut: MaterialButton = itemView.findViewById(R.id.btnShipOut)

        fun bind(parcel: Parcel) {
            tvTrackingNumber.text = parcel.parcelID
            tvCarrierBadge.text = parcel.carrier
            tvZoneBadge.text = parcel.zone.ifBlank { "—" }
            val binOrRack = parcel.binCoordinate.ifBlank { parcel.rack }
            tvBinInfo.text = binOrRack
            tvRecipient.text = parcel.receiptName
            tvStatus.text = parcel.status
            val statusColor = when {
                parcel.isShipped() -> R.color.pink
                parcel.isOutboundStaged() -> R.color.orangeAccent
                else -> R.color.greenAccent
            }
            tvStatus.setTextColor(ContextCompat.getColor(itemView.context, statusColor))
            tvTimestamp.text = if (parcel.createdAt > 0) {
                dateFormat.format(Date(parcel.createdAt))
            } else {
                ""
            }

            when {
                parcel.isInStock() -> {
                    btnShipOut.visibility = View.VISIBLE
                    btnShipOut.text = itemView.context.getString(R.string.ship_out)
                    btnShipOut.setOnClickListener { onShipClicked(parcel) }
                }
                parcel.isOutboundStaged() -> {
                    btnShipOut.visibility = View.VISIBLE
                    btnShipOut.text = "Complete Shipment"
                    btnShipOut.setOnClickListener { onShipClicked(parcel) }
                }
                else -> {
                    btnShipOut.visibility = View.GONE
                }
            }

            itemView.setOnClickListener {
                onItemClicked(parcel)
            }
        }
    }
}
