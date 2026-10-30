package com.example.oop_warehouse_location.fragments

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.example.oop_warehouse_location.R
import com.example.oop_warehouse_location.models.Parcel
import com.example.oop_warehouse_location.repository.FirebaseRepository
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ParcelDetailBottomSheetFragment : BottomSheetDialogFragment() {

    private lateinit var repository: FirebaseRepository
    private var warehouseId: String = ""
    private var parcel: Parcel = Parcel()
    private val dateFormat = SimpleDateFormat("MMM d, yyyy — h:mm a", Locale.getDefault())

    var onActionCompleted: (() -> Unit)? = null

    companion object {
        fun newInstance(warehouseId: String, parcel: Parcel): ParcelDetailBottomSheetFragment {
            return ParcelDetailBottomSheetFragment().apply {
                this.warehouseId = warehouseId
                this.parcel = parcel
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        repository = FirebaseRepository(warehouseId = warehouseId)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.bottom_sheet_parcel_detail, container, false)
        bindViews(view)
        return view
    }

    private fun bindViews(view: View) {
        val tvTracking = view.findViewById<TextView>(R.id.sheetTvTrackingNumber)
        val tvCarrier = view.findViewById<TextView>(R.id.sheetTvCarrierBadge)
        val tvStatus = view.findViewById<TextView>(R.id.sheetTvStatus)
        val tvCreatedAt = view.findViewById<TextView>(R.id.sheetTvCreatedAt)

        val tvRecipientName = view.findViewById<TextView>(R.id.sheetTvRecipientName)
        val tvRecipientPhone = view.findViewById<TextView>(R.id.sheetTvRecipientPhone)
        val tvDeliveryHub = view.findViewById<TextView>(R.id.sheetTvDeliveryHub)

        val tvZone = view.findViewById<TextView>(R.id.sheetTvZone)
        val tvRack = view.findViewById<TextView>(R.id.sheetTvRack)
        val tvBin = view.findViewById<TextView>(R.id.sheetTvBinCoordinate)

        val tvParcelType = view.findViewById<TextView>(R.id.sheetTvParcelType)
        val tvHandling = view.findViewById<TextView>(R.id.sheetTvHandling)

        val tvDispatchDetails = view.findViewById<TextView>(R.id.sheetTvDispatchDetails)
        val tilNotes = view.findViewById<TextInputLayout>(R.id.sheetTilNotes)
        val etNotes = view.findViewById<TextInputEditText>(R.id.sheetEtNotes)

        val progressBar = view.findViewById<ProgressBar>(R.id.sheetProgressBar)
        val actionContainer = view.findViewById<LinearLayout>(R.id.sheetActionContainer)
        val btnStageOutbound = view.findViewById<MaterialButton>(R.id.sheetBtnStageOutbound)
        val btnShipOut = view.findViewById<MaterialButton>(R.id.sheetBtnShipOut)
        val btnReturnToStock = view.findViewById<MaterialButton>(R.id.sheetBtnReturnToStock)

        // Populate values
        tvTracking.text = parcel.parcelID
        tvCarrier.text = parcel.carrier.ifBlank { "Carrier" }

        tvRecipientName.text = "Recipient: ${parcel.receiptName.ifBlank { "—" }}"
        tvRecipientPhone.text = "Phone: ${parcel.recipientPhone.ifBlank { "—" }}"
        tvDeliveryHub.text = "Hub: ${parcel.deliveryHub.ifBlank { "—" }}"

        tvZone.text = "Zone: ${parcel.zone.ifBlank { "—" }}"
        tvRack.text = "Rack: ${parcel.rack.ifBlank { "—" }}"
        tvBin.text = "Bin: ${parcel.binCoordinate.ifBlank { "—" }}"

        tvParcelType.text = "Type: ${parcel.parcelType.ifBlank { "Standard" }}"
        val handlingText = if (parcel.handling.isEmpty()) "Standard handling" else parcel.handling.joinToString(", ")
        tvHandling.text = "Handling: $handlingText"

        tvCreatedAt.text = if (parcel.createdAt > 0) {
            "Added: ${dateFormat.format(Date(parcel.createdAt))}"
        } else {
            ""
        }

        // Status Styling & Visibility
        when {
            parcel.isShipped() -> {
                tvStatus.text = parcel.status
                tvStatus.setBackgroundResource(R.drawable.bg_badge_pink)
                tvStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.pink))

                val shippedDateStr = if (parcel.shippedAt > 0) dateFormat.format(Date(parcel.shippedAt)) else "Recently"
                val dispatcherStr = if (parcel.shippedByUserName.isNotBlank()) " by ${parcel.shippedByUserName}" else ""
                val notesStr = if (parcel.dispatchNotes.isNotBlank()) "\nNotes: ${parcel.dispatchNotes}" else ""
                tvDispatchDetails.text = "✓ Dispatched on $shippedDateStr$dispatcherStr$notesStr"
                tvDispatchDetails.visibility = View.VISIBLE

                tilNotes.visibility = View.GONE
                actionContainer.visibility = View.GONE
            }
            parcel.isOutboundStaged() -> {
                tvStatus.text = "Outbound Staged"
                tvStatus.setBackgroundResource(R.drawable.bg_badge_orange)
                tvStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.orangeAccent))

                val stagedDateStr = if (parcel.stagedAt > 0) dateFormat.format(Date(parcel.stagedAt)) else "Recently"
                tvDispatchDetails.text = "Staged for shipping at $stagedDateStr (Location: Staging Area)"
                tvDispatchDetails.visibility = View.VISIBLE

                btnStageOutbound.visibility = View.GONE
                btnShipOut.visibility = View.VISIBLE
                btnShipOut.text = "Complete Shipment / Dispatch"
                btnReturnToStock.visibility = View.VISIBLE
                tilNotes.visibility = View.VISIBLE
            }
            else -> {
                tvStatus.text = "In Stock"
                tvStatus.setBackgroundResource(R.drawable.bg_badge_green)
                tvStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.greenAccent))

                tvDispatchDetails.visibility = View.GONE
                btnStageOutbound.visibility = View.VISIBLE
                btnShipOut.visibility = View.VISIBLE
                btnShipOut.text = "Direct Ship Out"
                btnReturnToStock.visibility = View.GONE
                tilNotes.visibility = View.VISIBLE
            }
        }

        // Actions
        btnStageOutbound.setOnClickListener {
            progressBar.visibility = View.VISIBLE
            actionContainer.visibility = View.GONE
            repository.stageForOutbound(parcel.parcelID)
                .addOnSuccessListener {
                    if (isAdded) {
                        Toast.makeText(requireContext(), "Parcel staged for outbound", Toast.LENGTH_SHORT).show()
                        onActionCompleted?.invoke()
                        dismiss()
                    }
                }
                .addOnFailureListener { e ->
                    if (isAdded) {
                        progressBar.visibility = View.GONE
                        actionContainer.visibility = View.VISIBLE
                        Toast.makeText(requireContext(), e.message ?: "Failed to stage parcel", Toast.LENGTH_SHORT).show()
                    }
                }
        }

        btnShipOut.setOnClickListener {
            val notes = etNotes.text?.toString()?.trim().orEmpty()
            AlertDialog.Builder(requireContext())
                .setTitle("Confirm Dispatch")
                .setMessage("Ship out parcel ${parcel.parcelID} via ${parcel.carrier.ifBlank { "carrier" }}?")
                .setPositiveButton("Dispatch") { _, _ ->
                    progressBar.visibility = View.VISIBLE
                    actionContainer.visibility = View.GONE
                    repository.completeShipment(parcel.parcelID, notes)
                        .addOnSuccessListener {
                            if (isAdded) {
                                Toast.makeText(requireContext(), "Parcel shipped successfully", Toast.LENGTH_SHORT).show()
                                onActionCompleted?.invoke()
                                dismiss()
                            }
                        }
                        .addOnFailureListener { e ->
                            if (isAdded) {
                                progressBar.visibility = View.GONE
                                actionContainer.visibility = View.VISIBLE
                                Toast.makeText(requireContext(), e.message ?: "Failed to ship parcel", Toast.LENGTH_SHORT).show()
                            }
                        }
                }
                .setNegativeButton("Cancel", null)
                .show()
        }

        btnReturnToStock.setOnClickListener {
            progressBar.visibility = View.VISIBLE
            actionContainer.visibility = View.GONE
            repository.returnToStock(parcel.parcelID)
                .addOnSuccessListener {
                    if (isAdded) {
                        Toast.makeText(requireContext(), "Parcel returned to stock", Toast.LENGTH_SHORT).show()
                        onActionCompleted?.invoke()
                        dismiss()
                    }
                }
                .addOnFailureListener { e ->
                    if (isAdded) {
                        progressBar.visibility = View.GONE
                        actionContainer.visibility = View.VISIBLE
                        Toast.makeText(requireContext(), e.message ?: "Failed to return parcel to stock", Toast.LENGTH_SHORT).show()
                    }
                }
        }
    }
}

