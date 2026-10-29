package com.example.oop_warehouse_location.fragments

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.oop_warehouse_location.R
import com.example.oop_warehouse_location.adapters.ParcelAdapter
import com.example.oop_warehouse_location.models.Parcel
import com.example.oop_warehouse_location.repository.FirebaseRepository
import com.google.firebase.firestore.ListenerRegistration

class StockFragment : Fragment() {

    private lateinit var repository: FirebaseRepository
    private val adapter = ParcelAdapter()
    private val allParcels = mutableListOf<Parcel>()
    private var parcelsListener: ListenerRegistration? = null
    private var currentFilter = ""
    private var warehouseId: String = ""

    companion object {
        private const val ARG_WAREHOUSE_ID = "warehouseId"

        fun newInstance(warehouseId: String): StockFragment {
            return StockFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_WAREHOUSE_ID, warehouseId)
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        warehouseId = arguments?.getString(ARG_WAREHOUSE_ID) ?: ""
        repository = FirebaseRepository(warehouseId = warehouseId)
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
                renderParcels()
            }
        })

        loadParcel(view)
        return view
    }

    fun loadParcel(view: View = requireView()) {
        parcelsListener?.remove()
        parcelsListener = repository.observeParcels { parcels ->
            allParcels.clear()
            allParcels.addAll(parcels)
            renderParcels(view)
        }
    }

    private fun renderParcels(view: View = requireView()) {
        val filtered = if (currentFilter.isBlank()) {
            allParcels
        } else {
            allParcels.filter { parcel ->
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
        emptyState.visibility = if (isEmpty) View.VISIBLE else View.GONE
        recyclerView.visibility = if (isEmpty) View.GONE else View.VISIBLE
    }

    override fun onDestroyView() {
        parcelsListener?.remove()
        parcelsListener = null
        super.onDestroyView()
    }
}
