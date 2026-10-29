package com.example.oop_warehouse_location.fragments

import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
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

    private lateinit var repository: FirebaseRepository
    private var warehouse = Warehouse()
    private var parcelsListener: ListenerRegistration? = null
    private var warehouseId: String = ""
    private var accountType: String = ""

    companion object {
        private const val ARG_WAREHOUSE_ID = "warehouseId"
        private const val ARG_ACCOUNT_TYPE = "accountType"

        fun newInstance(warehouseId: String, accountType: String): DashboardFragment {
            return DashboardFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_WAREHOUSE_ID, warehouseId)
                    putString(ARG_ACCOUNT_TYPE, accountType)
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        warehouseId = arguments?.getString(ARG_WAREHOUSE_ID) ?: ""
        accountType = arguments?.getString(ARG_ACCOUNT_TYPE) ?: ""
        repository = FirebaseRepository(warehouseId = warehouseId)
    }

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

        // Show/hide invite code section based on account type
        setupInviteCodeSection(view)

        loadDashboardStarts(view)
        return view
    }

    override fun onResume() {
        super.onResume()
        // Re-subscribe so the dashboard refreshes when returning from AddInventoryActivity
        if (view != null) {
            loadDashboardStarts(requireView())
        }
    }

    /**
     * If the user is a Manager, show the invite code card.
     * The card is added dynamically so we don't need to modify the existing layout XML
     * if the view doesn't exist yet. If tvInviteCode exists in the layout, we use it.
     */
    private fun setupInviteCodeSection(view: View) {
        val inviteCodeCard = view.findViewById<View>(R.id.inviteCodeCard)
        val tvInviteCode = view.findViewById<TextView>(R.id.tvInviteCode)

        if (accountType == "Manager" && inviteCodeCard != null && tvInviteCode != null) {
            inviteCodeCard.visibility = View.VISIBLE
            // Load the warehouse to get the invite code
            repository.getWarehouse(warehouseId)
                .addOnSuccessListener { wh ->
                    if (isAdded && this.view != null) {
                        warehouse = wh
                        tvInviteCode.text = wh.inviteCode

                        // Set up copy-to-clipboard
                        inviteCodeCard.setOnClickListener {
                            val clipboard = requireContext().getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                            val clip = android.content.ClipData.newPlainText("Invite Code", wh.inviteCode)
                            clipboard.setPrimaryClip(clip)
                            Toast.makeText(requireContext(), "Invite code copied!", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
        } else if (inviteCodeCard != null) {
            inviteCodeCard.visibility = View.GONE
        }
    }

    fun loadDashboardStarts(view: View = requireView()) {
        parcelsListener?.remove()
        parcelsListener = repository.observeParcels { parcels ->
            if (isAdded && this.view != null) {
                bindStats(view, parcels)
                bindAisleBarChart(view, parcels)
            }
        }
    }

    private fun bindStats(view: View, parcels: List<Parcel>) {
        val onHand = parcels.count { (it.status ?: "").equals("In Stock", ignoreCase = true) }
        val inboundToday = parcels.count { isToday(it.createdAt) }
        val outboundStaged = parcels.count {
            (it.status ?: "").equals("Outbound Staged", ignoreCase = true) ||
                (it.zone ?: "").equals("Staging", ignoreCase = true)
        }
        val totalCap = if (warehouse.totalCapacity > 0) warehouse.totalCapacity else 500
        val capacityPercent = ((parcels.size * 100) / totalCap).coerceAtMost(100)

        view.findViewById<TextView>(R.id.tvOnHand)?.text = onHand.toString()
        view.findViewById<TextView>(R.id.tvInboundToday)?.text = inboundToday.toString()
        view.findViewById<TextView>(R.id.tvOutboundStaged)?.text = outboundStaged.toString()
        view.findViewById<TextView>(R.id.tvCapacityPercent)?.text = "$capacityPercent%"
        view.findViewById<ProgressBar>(R.id.progressCapacity)?.progress = capacityPercent
        view.findViewById<TextView>(R.id.tvOutboundDock)?.text = outboundStaged.toString()

        val zoneA = occupancyFor(parcels, "Zone A")
        val zoneB = occupancyFor(parcels, "Zone B")
        val zoneC = occupancyFor(parcels, "Zone C")
        view.findViewById<TextView>(R.id.tvZoneA)?.text = "$zoneA%"
        view.findViewById<TextView>(R.id.tvZoneB)?.text = "$zoneB%"
        view.findViewById<TextView>(R.id.tvZoneC)?.text = "$zoneC%"
    }

    // ── Aisle Load Index: build bar chart from rack distribution ────────
    private fun bindAisleBarChart(view: View, parcels: List<Parcel>) {
        val container = view.findViewById<LinearLayout>(R.id.aisleBarChart) ?: return
        container.removeAllViews()

        // Rack labels matching those in AddInventoryActivity
        val rackLabels = listOf(
            "Rack 01 - Top", "Rack 02 - Top", "Rack 03 - Mid",
            "Rack 04 - Mid", "Rack 05 - Bottom", "Rack 06 - Floor"
        )
        val shortLabels = listOf("R01", "R02", "R03", "R04", "R05", "R06")

        val rackCounts = rackLabels.map { label ->
            parcels.count { (it.rack ?: "").equals(label, ignoreCase = true) }
        }
        val maxCount = rackCounts.maxOrNull()?.coerceAtLeast(1) ?: 1
        val maxBarHeight = 60 // dp
        val ctx = context ?: return
        val density = ctx.resources.displayMetrics.density
        val pinkColor = ContextCompat.getColor(ctx, R.color.pink)
        val grayColor = ContextCompat.getColor(ctx, R.color.borderGray)
        val textSecondary = ContextCompat.getColor(ctx, R.color.textSecondary)

        rackCounts.forEachIndexed { index, count ->
            // Column wrapper
            val column = LinearLayout(ctx).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER_HORIZONTAL or Gravity.BOTTOM
                layoutParams = LinearLayout.LayoutParams(
                    0,
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    1f
                )
            }

            // Count label above bar
            val countLabel = TextView(ctx).apply {
                text = count.toString()
                textSize = 9f
                setTextColor(textSecondary)
                gravity = Gravity.CENTER
            }

            // Bar
            val barHeight = if (count > 0) {
                ((count.toFloat() / maxCount) * maxBarHeight).toInt().coerceAtLeast(4)
            } else {
                4
            }
            val bar = View(ctx).apply {
                layoutParams = LinearLayout.LayoutParams(
                    (16 * density).toInt(),
                    (barHeight * density).toInt()
                ).apply {
                    topMargin = (4 * density).toInt()
                }
                setBackgroundColor(if (count > 0) pinkColor else grayColor)
            }

            // Rack label below bar
            val label = TextView(ctx).apply {
                text = shortLabels[index]
                textSize = 8f
                setTextColor(textSecondary)
                gravity = Gravity.CENTER
                setPadding(0, (2 * density).toInt(), 0, 0)
            }

            column.addView(countLabel)
            column.addView(bar)
            column.addView(label)
            container.addView(column)
        }
    }

    private fun occupancyFor(parcels: List<Parcel>, zone: String): Int {
        val totalCap = if (warehouse.totalCapacity > 0) warehouse.totalCapacity else 500
        val zoneCapacity = totalCap / 3
        if (zoneCapacity <= 0) return 0
        val count = parcels.count { (it.zone ?: "").equals(zone, ignoreCase = true) }
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
