package com.example.oop_warehouse_location.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.example.oop_warehouse_location.R
import com.example.oop_warehouse_location.activities.MainActivity
import com.example.oop_warehouse_location.models.Parcel
import com.example.oop_warehouse_location.models.Warehouse
import com.example.oop_warehouse_location.repository.FirebaseRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ListenerRegistration
import java.util.Calendar

class DashboardFragment : Fragment() {

    private val repository = FirebaseRepository()
    private val warehouse = Warehouse()
    private var parcelsListener: ListenerRegistration? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_dashboard, container, false)

        val menuButton = view.findViewById<ImageView>(R.id.ivMenuButton)
        menuButton.setOnClickListener {
            (activity as? MainActivity)?.openDrawer()
        }

        val displayName = FirebaseAuth.getInstance().currentUser?.displayName ?: "User"
        view.findViewById<TextView>(R.id.tvGreeting).text = "Hello, $displayName!"

        loadDashboardStarts(view)
        return view
    }

    fun loadDashboardStarts(view: View = requireView()) {
        parcelsListener?.remove()
        parcelsListener = repository.observeParcels { parcels ->
            bindStats(view, parcels)
        }
    }

    private fun bindStats(view: View, parcels: List<Parcel>) {
        val onHand = parcels.count { it.status.equals("In Stock", ignoreCase = true) }
        val inboundToday = parcels.count { isToday(it.createdAt) }
        val outboundStaged = parcels.count {
            it.status.equals("Outbound Staged", ignoreCase = true) ||
                it.zone.equals("Staging", ignoreCase = true)
        }
        val capacityPercent = if (warehouse.totalCapacity == 0) {
            0
        } else {
            ((parcels.size * 100) / warehouse.totalCapacity).coerceAtMost(100)
        }

        view.findViewById<TextView>(R.id.tvOnHand).text = onHand.toString()
        view.findViewById<TextView>(R.id.tvInboundToday).text = inboundToday.toString()
        view.findViewById<TextView>(R.id.tvOutboundStaged).text = outboundStaged.toString()
        view.findViewById<TextView>(R.id.tvCapacityPercent).text = "$capacityPercent%"
        view.findViewById<ProgressBar>(R.id.progressCapacity).progress = capacityPercent
        view.findViewById<TextView>(R.id.tvOutboundDock).text = outboundStaged.toString()

        val zoneA = occupancyFor(parcels, "Zone A")
        val zoneB = occupancyFor(parcels, "Zone B")
        val zoneC = occupancyFor(parcels, "Zone C")
        view.findViewById<TextView>(R.id.tvZoneA).text = "$zoneA%"
        view.findViewById<TextView>(R.id.tvZoneB).text = "$zoneB%"
        view.findViewById<TextView>(R.id.tvZoneC).text = "$zoneC%"
    }

    private fun occupancyFor(parcels: List<Parcel>, zone: String): Int {
        val zoneCapacity = warehouse.totalCapacity / 3
        if (zoneCapacity == 0) return 0
        val count = parcels.count { it.zone.equals(zone, ignoreCase = true) }
        return ((count * 100) / zoneCapacity).coerceAtMost(100)
    }

    private fun isToday(timestamp: Long): Boolean {
        if (timestamp <= 0L) return false
        val created = Calendar.getInstance().apply { timeInMillis = timestamp }
        val now = Calendar.getInstance()
        return created.get(Calendar.YEAR) == now.get(Calendar.YEAR) &&
            created.get(Calendar.DAY_OF_YEAR) == now.get(Calendar.DAY_OF_YEAR)
    }

    override fun onDestroyView() {
        parcelsListener?.remove()
        parcelsListener = null
        super.onDestroyView()
    }
}
