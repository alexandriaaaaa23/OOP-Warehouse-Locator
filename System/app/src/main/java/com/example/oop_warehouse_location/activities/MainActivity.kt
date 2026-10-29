package com.example.oop_warehouse_location.activities

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import com.example.oop_warehouse_location.fragments.DashboardFragment
import com.example.oop_warehouse_location.R
import com.example.oop_warehouse_location.fragments.ManageWarehouseFragment
import com.example.oop_warehouse_location.fragments.StockFragment
import com.example.oop_warehouse_location.repository.FirebaseRepository
import com.google.android.material.navigation.NavigationView
import com.google.firebase.auth.FirebaseAuth

class MainActivity : AppCompatActivity() {

    private lateinit var navHome: LinearLayout
    private lateinit var navStock: LinearLayout
    private lateinit var navAdd: ImageView
    private lateinit var navHomeIcon: ImageView
    private lateinit var navHomeLabel: TextView
    private lateinit var navStockIcon: ImageView
    private lateinit var navStockLabel: TextView
    lateinit var drawerLayout: DrawerLayout
    private lateinit var navigationView: NavigationView

    /** The warehouse this session operates on. */
    var warehouseId: String = ""
        private set

    /** The current user's account type ("Manager" or "Employee"). */
    var accountType: String = ""
        private set

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        // Retrieve warehouse context passed from sign-in / sign-up
        warehouseId = intent.getStringExtra("warehouseId") ?: ""
        accountType = intent.getStringExtra("accountType") ?: ""

        // Bind all navigation views first to avoid UninitializedPropertyAccessException
        drawerLayout = findViewById(R.id.drawerLayout)
        navigationView = findViewById(R.id.navigationView)
        navHome = findViewById(R.id.navHome)
        navStock = findViewById(R.id.navStock)
        navAdd = findViewById(R.id.navAdd)
        navHomeIcon = findViewById(R.id.navHomeIcon)
        navHomeLabel = findViewById(R.id.navHomeLabel)
        navStockIcon = findViewById(R.id.navStockIcon)
        navStockLabel = findViewById(R.id.navStockLabel)

        applyRoleBasedUi()

        val mainRoot = findViewById<View>(R.id.main)
        if (mainRoot != null) {
            ViewCompat.setOnApplyWindowInsetsListener(mainRoot) { v, insets ->
                val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
                insets
            }
        }

        // Populate nav header with Firebase user details safely
        val headerView = navigationView.getHeaderView(0)
        if (headerView != null) {
            val tvName = headerView.findViewById<TextView>(R.id.navHeaderName)
            val tvEmail = headerView.findViewById<TextView>(R.id.navHeaderEmail)
            val tvAccountType = headerView.findViewById<TextView>(R.id.navHeaderAccountType)
            val currentUser = FirebaseAuth.getInstance().currentUser
            if (currentUser != null) {
                tvName?.text = currentUser.displayName ?: "User"
                tvEmail?.text = currentUser.email ?: ""
            }
            tvAccountType?.text = if (accountType.isNotBlank()) accountType else "User"
        }

        // Handle drawer menu item clicks
        navigationView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_home -> {
                    loadFragment(DashboardFragment.newInstance(warehouseId, accountType))
                    selectTab(isHomeSelected = true)
                    drawerLayout.closeDrawer(GravityCompat.START)
                    true
                }
                R.id.nav_stock -> {
                    loadFragment(StockFragment.newInstance(warehouseId))
                    selectTab(isHomeSelected = false)
                    drawerLayout.closeDrawer(GravityCompat.START)
                    true
                }
                R.id.nav_manage_warehouse -> {
                    loadFragment(ManageWarehouseFragment.newInstance(warehouseId))
                    drawerLayout.closeDrawer(GravityCompat.START)
                    true
                }
                R.id.nav_activity_logs -> {
                    val intent = Intent(this, ActivityLogsActivity::class.java)
                    intent.putExtra("warehouseId", warehouseId)
                    startActivity(intent)
                    drawerLayout.closeDrawer(GravityCompat.START)
                    true
                }
                R.id.nav_logout -> {
                    FirebaseAuth.getInstance().signOut()
                    val intent = Intent(this, SignInPage::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    true
                }
                else -> false
            }
        }

        // DashboardFragment
        if (savedInstanceState == null) {
            loadFragment(DashboardFragment.newInstance(warehouseId, accountType))
            selectTab(isHomeSelected = true)
        }

        if (warehouseId.isBlank() || accountType.isBlank()) {
            loadSessionFromProfile(savedInstanceState == null)
        }

        // Home tab click
        navHome.setOnClickListener {
            loadFragment(DashboardFragment.newInstance(warehouseId, accountType))
            selectTab(isHomeSelected = true)
        }

        // Stock tab click
        navStock.setOnClickListener {
            loadFragment(StockFragment.newInstance(warehouseId))
            selectTab(isHomeSelected = false)
        }

        // Center FAB — open Add Inventory screen
        navAdd.setOnClickListener {
            val intent = Intent(this, AddInventoryActivity::class.java)
            intent.putExtra("warehouseId", warehouseId)
            startActivity(intent)
        }
    }

    private fun applyRoleBasedUi() {
        // Both Manager and Employee can add inventory to the repository
        navAdd.visibility = View.VISIBLE
        navigationView.menu.findItem(R.id.nav_manage_warehouse)?.isVisible = accountType == "Manager"
        navigationView.menu.findItem(R.id.nav_activity_logs)?.isVisible = accountType == "Manager"
        val headerView = navigationView.getHeaderView(0)
        headerView?.findViewById<TextView>(R.id.navHeaderAccountType)?.text =
            if (accountType.isNotBlank()) accountType else "User"
    }

    /**
     * Reloads warehouseId / accountType from Firestore when Intent extras are missing
     * (process restore, older accounts, etc.).
     */
    private fun loadSessionFromProfile(reloadHome: Boolean) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        FirebaseRepository().getUser(uid)
            .addOnSuccessListener { user ->
                warehouseId = user.warehouseId
                accountType = user.accountType
                applyRoleBasedUi()

                if (warehouseId.isBlank() && accountType == "Employee") {
                    startActivity(Intent(this, JoinWarehouseActivity::class.java))
                    finish()
                    return@addOnSuccessListener
                }

                if (reloadHome) {
                    loadFragment(DashboardFragment.newInstance(warehouseId, accountType))
                    selectTab(isHomeSelected = true)
                }
            }
    }

    /** Opens the navigation drawer from the start (left) side. */
    fun openDrawer() {
        if (::drawerLayout.isInitialized) {
            drawerLayout.openDrawer(GravityCompat.START)
        }
    }

    /** Handle back press to close drawer first if open. */
    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (::drawerLayout.isInitialized && drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }

    /** Replace the fragment inside the container. */
    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .commit()
    }

    /** Update icon tints and label colors to reflect the active tab. */
    private fun selectTab(isHomeSelected: Boolean) {
        if (!::navHomeIcon.isInitialized || !::navStockIcon.isInitialized) return

        val activeColor = ContextCompat.getColor(this, R.color.pink)
        val inactiveColor = ContextCompat.getColor(this, R.color.hintGray)

        // Home tab
        navHomeIcon.setColorFilter(if (isHomeSelected) activeColor else inactiveColor)
        navHomeLabel.setTextColor(if (isHomeSelected) activeColor else inactiveColor)
        navHomeLabel.setTypeface(null, if (isHomeSelected) android.graphics.Typeface.BOLD else android.graphics.Typeface.NORMAL)

        // Stock tab
        navStockIcon.setColorFilter(if (isHomeSelected) inactiveColor else activeColor)
        navStockLabel.setTextColor(if (isHomeSelected) inactiveColor else activeColor)
        navStockLabel.setTypeface(null, if (isHomeSelected) android.graphics.Typeface.NORMAL else android.graphics.Typeface.BOLD)
    }
}