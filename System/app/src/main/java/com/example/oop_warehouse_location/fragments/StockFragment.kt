package com.example.oop_warehouse_location.fragments

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.oop_warehouse_location.R
import com.example.oop_warehouse_location.adapters.ParcelAdapter
import com.example.oop_warehouse_location.models.Parcel
import com.example.oop_warehouse_location.repository.FirebaseRepository
import com.google.firebase.firestore.ListenerRegistration

class StockFragment : Fragment() {

    enum class StatusFilter { IN_STOCK, OUTBOUND_STAGED, SHIPPED, ALL }

    private lateinit var repository: FirebaseRepository
    private lateinit var adapter: ParcelAdapter
    private val allParcels = mutableListOf<Parcel>()
    private var parcelsListener: ListenerRegistration? = null
    private var currentFilter = ""
    private var statusFilter = StatusFilter.IN_STOCK
    private var warehouseId: String = ""

    companion object {
        private const val ARG_WAREHOUSE_ID = "warehouseId"
        private const val ARG_INITIAL_FILTER = "initialFilter"

        fun newInstance(warehouseId: String, initialFilter: StatusFilter = StatusFilter.IN_STOCK): StockFragment {
            return StockFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_WAREHOUSE_ID, warehouseId)
                    putString(ARG_INITIAL_FILTER, initialFilter.name)
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        warehouseId = arguments?.getString(ARG_WAREHOUSE_ID) ?: ""
        val initialFilterName = arguments?.getString(ARG_INITIAL_FILTER)
        if (!initialFilterName.isNullOrBlank()) {
            statusFilter = runCatching { StatusFilter.valueOf(initialFilterName) }.getOrDefault(StatusFilter.IN_STOCK)
        }
        repository = FirebaseRepository(warehouseId = warehouseId)

        adapter = ParcelAdapter(
            onItemClicked = { parcel ->
                openParcelDetail(parcel)
            },
            onShipClicked = { parcel ->
                openParcelDetail(parcel)
            }
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_stock, container, false)

        val recyclerView = view.findViewById<RecyclerView>(R.id.rvParcels)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = adapter

        view.findViewById<EditText>(R.id.etStockFilter).addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
            override fun afterTextChanged(s: Editable?) {
                currentFilter = s?.toString()?.trim().orEmpty()
                renderParcels(view)
            }
        })

        view.findViewById<TextView>(R.id.tvFilterActive)?.setOnClickListener {
            setStatusFilter(view, StatusFilter.IN_STOCK)
        }
        view.findViewById<TextView>(R.id.tvFilterOutbound)?.setOnClickListener {
            setStatusFilter(view, StatusFilter.OUTBOUND_STAGED)
        }
        view.findViewById<TextView>(R.id.tvFilterShipped)?.setOnClickListener {
            setStatusFilter(view, StatusFilter.SHIPPED)
        }
        view.findViewById<TextView>(R.id.tvFilterAll)?.setOnClickListener {
            setStatusFilter(view, StatusFilter.ALL)
        }
        updateFilterChipStyles(view)

        loadParcel(view)
        return view
    }

    private fun openParcelDetail(parcel: Parcel) {
        val bottomSheet = ParcelDetailBottomSheetFragment.newInstance(warehouseId, parcel)
        bottomSheet.onActionCompleted = {
            if (isAdded && view != null) {
                loadParcel(requireView())
            }
        }
        bottomSheet.show(childFragmentManager, "ParcelDetailBottomSheet")
    }

    fun loadParcel(view: View = requireView()) {
        parcelsListener?.remove()
        parcelsListener = repository.observeParcels { parcels ->
            allParcels.clear()
            allParcels.addAll(parcels)
            if (isAdded && this.view != null) {
                renderParcels(view)
            }
        }
    }

    fun setStatusFilter(view: View, filter: StatusFilter) {
        statusFilter = filter
        updateFilterChipStyles(view)
        renderParcels(view)
    }

    private fun updateFilterChipStyles(view: View) {
        view.findViewById<TextView>(R.id.tvFilterActive)?.let {
            styleChip(it, statusFilter == StatusFilter.IN_STOCK)
        }
        view.findViewById<TextView>(R.id.tvFilterOutbound)?.let {
            styleChip(it, statusFilter == StatusFilter.OUTBOUND_STAGED)
        }
        view.findViewById<TextView>(R.id.tvFilterShipped)?.let {
            styleChip(it, statusFilter == StatusFilter.SHIPPED)
        }
        view.findViewById<TextView>(R.id.tvFilterAll)?.let {
            styleChip(it, statusFilter == StatusFilter.ALL)
        }
    }

    private fun styleChip(chip: TextView, selected: Boolean) {
        if (selected) {
            chip.setBackgroundResource(R.drawable.bg_badge_pink_solid)
            chip.setTextColor(ContextCompat.getColor(requireContext(), R.color.white))
        } else {
            chip.setBackgroundResource(R.drawable.bg_card_rounded)
            chip.setTextColor(ContextCompat.getColor(requireContext(), R.color.black))
        }
    }

    private fun renderParcels(view: View = requireView()) {
        val byStatus = when (statusFilter) {
            StatusFilter.IN_STOCK -> allParcels.filter { it.isInStock() }
            StatusFilter.OUTBOUND_STAGED -> allParcels.filter { it.isOutboundStaged() }
            StatusFilter.SHIPPED -> allParcels.filter { it.isShipped() }
            StatusFilter.ALL -> allParcels
        }
        val filtered = if (currentFilter.isBlank()) {
            byStatus
        } else {
            byStatus.filter { parcel ->
                parcel.parcelID.contains(currentFilter, ignoreCase = true) ||
                    parcel.receiptName.contains(currentFilter, ignoreCase = true) ||
                    parcel.carrier.contains(currentFilter, ignoreCase = true) ||
                    parcel.zone.contains(currentFilter, ignoreCase = true)
            }
        }

        adapter.submitList(filtered)
        val emptyState = view.findViewById<LinearLayout>(R.id.emptyState)
        val recyclerView = view.findViewById<RecyclerView>(R.id.rvParcels)
        val isEmpty = filtered.isEmpty()
        emptyState?.visibility = if (isEmpty) View.VISIBLE else View.GONE
        recyclerView?.visibility = if (isEmpty) View.GONE else View.VISIBLE
    }

    override fun onDestroyView() {
        parcelsListener?.remove()
        parcelsListener = null
        super.onDestroyView()
    }
}
