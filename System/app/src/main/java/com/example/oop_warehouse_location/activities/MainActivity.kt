package com.example.oop_warehouse_location.activities

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import com.example.oop_warehouse_location.R
import com.example.oop_warehouse_location.activities.base.BaseActivity
import com.example.oop_warehouse_location.fragments.ActivityLogsFragment
import com.example.oop_warehouse_location.fragments.DashboardFragment
import com.example.oop_warehouse_location.fragments.ManageWarehouseFragment
import com.example.oop_warehouse_location.fragments.StockFragment
import com.example.oop_warehouse_location.models.user.Employee
import com.example.oop_warehouse_location.models.user.User
import com.example.oop_warehouse_location.models.user.UserFactory
import com.example.oop_warehouse_location.repository.FirebaseRepository
import com.google.android.material.navigation.NavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

/**
 * INHERITANCE — extends [BaseActivity] which provides:
 *   • FirebaseAuth initialisation
 *   • Edge-to-edge setup
 *
 * Overrides [setupWindowInsets] because the main layout uses a
 * DrawerLayout root and the system-bar insets must be applied to
 * the inner FrameLayout ([R.id.main]) rather than the drawer itself.
 */
class MainActivity : BaseActivity() {

    enum class NavTab { HOME, STOCK, ACTIVITY, MEMBERS, NONE }

    private lateinit var bottomNavBar: LinearLayout
    private lateinit var navHome: LinearLayout
    private lateinit var navStock: LinearLayout
    private lateinit var navAddContainer: FrameLayout
    private lateinit var navAdd: ImageView
    private lateinit var navActivity: LinearLayout
    private lateinit var navMembers: LinearLayout

    private lateinit var navHomeIcon: ImageView
    private lateinit var navHomeLabel: TextView
    private lateinit var navStockIcon: ImageView
    private lateinit var navStockLabel: TextView
    private lateinit var navActivityIcon: ImageView
    private lateinit var navActivityLabel: TextView
    private lateinit var navMembersIcon: ImageView
    private lateinit var navMembersLabel: TextView

    lateinit var drawerLayout: DrawerLayout
    private lateinit var navigationView: NavigationView
    private var userProfileListener: ListenerRegistration? = null

    /** The warehouse this session operates on. */
    var warehouseId: String = ""
        private set

    /** The current user object (Manager or Employee subclass for polymorphism). */
    var currentUser: User? = null
        private set

    /** The current user's account type ("Manager" or "Employee"). */
    val accountType: String
        get() = currentUser?.getRole().orEmpty()

    override fun getLayoutResId(): Int = R.layout.activity_main

    /**
     * MainActivity uses a DrawerLayout as root, so the default
     * BaseActivity insets (which look for R.id.main) apply to the
     * inner content FrameLayout — that's the right target here.
     */
    override fun setupWindowInsets() {
        val mainRoot = findViewById<View>(R.id.main) ?: return
        ViewCompat.setOnApplyWindowInsetsListener(mainRoot) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Retrieve warehouse context passed from sign-in / sign-up
        warehouseId = intent.getStringExtra("warehouseId") ?: ""
        val initialAccountType = intent.getStringExtra("accountType") ?: ""
        currentUser = UserFactory.create(
            accountType = initialAccountType,
            uid = FirebaseAuth.getInstance().currentUser?.uid.orEmpty(),
            warehouseId = warehouseId
        )

        // Bind all navigation views first to avoid UninitializedPropertyAccessException
        drawerLayout = findViewById(R.id.drawerLayout)
        navigationView = findViewById(R.id.navigationView)
        bottomNavBar = findViewById(R.id.bottomNavBar)
        navHome = findViewById(R.id.navHome)
        navStock = findViewById(R.id.navStock)
        navAddContainer = findViewById(R.id.navAddContainer)
        navAdd = findViewById(R.id.navAdd)
        navActivity = findViewById(R.id.navActivity)
        navMembers = findViewById(R.id.navMembers)

        navHomeIcon = findViewById(R.id.navHomeIcon)
        navHomeLabel = findViewById(R.id.navHomeLabel)
        navStockIcon = findViewById(R.id.navStockIcon)
        navStockLabel = findViewById(R.id.navStockLabel)
        navActivityIcon = findViewById(R.id.navActivityIcon)
        navActivityLabel = findViewById(R.id.navActivityLabel)
        navMembersIcon = findViewById(R.id.navMembersIcon)
        navMembersLabel = findViewById(R.id.navMembersLabel)

        applyRoleBasedUi()

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
                    selectTab(NavTab.HOME)
                    drawerLayout.closeDrawer(GravityCompat.START)
                    true
                }
                R.id.nav_stock -> {
                    loadFragment(StockFragment.newInstance(warehouseId))
                    selectTab(NavTab.STOCK)
                    drawerLayout.closeDrawer(GravityCompat.START)
                    true
                }
                R.id.nav_add_parcel -> {
                    drawerLayout.closeDrawer(GravityCompat.START)
                    val intent = Intent(this, AddInventoryActivity::class.java)
                    intent.putExtra("warehouseId", warehouseId)
                    startActivity(intent)
                    true
                }
                R.id.nav_manage_warehouse -> {
                    loadFragment(ManageWarehouseFragment.newInstance(warehouseId))
                    selectTab(NavTab.MEMBERS)
                    drawerLayout.closeDrawer(GravityCompat.START)
                    true
                }
                R.id.nav_activity_logs -> {
                    loadFragment(ActivityLogsFragment.newInstance(warehouseId))
                    selectTab(NavTab.ACTIVITY)
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

        // DashboardFragment on initial load
        if (savedInstanceState == null) {
            loadFragment(DashboardFragment.newInstance(warehouseId, accountType))
            selectTab(NavTab.HOME)
        }

        if (warehouseId.isBlank() || accountType.isBlank()) {
            loadSessionFromProfile(savedInstanceState == null)
        } else {
            observeEmployeeWarehouseMembership()
        }

        // Home tab click
        navHome.setOnClickListener {
            loadFragment(DashboardFragment.newInstance(warehouseId, accountType))
            selectTab(NavTab.HOME)
        }

        // Stock tab click
        navStock.setOnClickListener {
            loadFragment(StockFragment.newInstance(warehouseId))
            selectTab(NavTab.STOCK)
        }

        // Activity Log tab click (Manager only)
        navActivity.setOnClickListener {
            loadFragment(ActivityLogsFragment.newInstance(warehouseId))
            selectTab(NavTab.ACTIVITY)
        }

        // Manage Member tab click (Manager only)
        navMembers.setOnClickListener {
            loadFragment(ManageWarehouseFragment.newInstance(warehouseId))
            selectTab(NavTab.MEMBERS)
        }

        // Center FAB — open Add Inventory screen (Employee)
        navAdd.setOnClickListener {
            val intent = Intent(this, AddInventoryActivity::class.java)
            intent.putExtra("warehouseId", warehouseId)
            startActivity(intent)
        }

        setupOnBackPressed()
    }

    private fun applyRoleBasedUi() {
        val canManage = currentUser?.canManageWarehouse() == true
        navigationView.menu.findItem(R.id.nav_manage_warehouse)?.isVisible = canManage
        navigationView.menu.findItem(R.id.nav_activity_logs)?.isVisible = canManage

        bottomNavBar.removeAllViews()

        if (canManage) {
            // Manager bottom nav: Home, Stock, Activity Log, Manage Member
            navAddContainer.visibility = View.GONE
            navActivity.visibility = View.VISIBLE
            navMembers.visibility = View.VISIBLE

            bottomNavBar.addView(navHome)
            bottomNavBar.addView(navStock)
            bottomNavBar.addView(navActivity)
            bottomNavBar.addView(navMembers)
        } else {
            // Employee bottom nav: Home, Add Parcel, Stock
            navAddContainer.visibility = View.VISIBLE
            navActivity.visibility = View.GONE
            navMembers.visibility = View.GONE

            bottomNavBar.addView(navHome)
            bottomNavBar.addView(navAddContainer)
            bottomNavBar.addView(navStock)
        }

        val headerView = navigationView.getHeaderView(0)
        headerView?.findViewById<TextView>(R.id.navHeaderAccountType)?.text =
            currentUser?.getRole()?.ifBlank { "User" } ?: "User"
    }


    private fun observeEmployeeWarehouseMembership() {
        if (currentUser !is Employee || warehouseId.isBlank()) return
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        userProfileListener?.remove()
        userProfileListener = FirebaseFirestore.getInstance()
            .collection("warehouses")
            .document(warehouseId)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null || !snapshot.exists()) return@addSnapshotListener
                val members = snapshot.get("members") as? List<*> ?: emptyList<Any>()
                if (!members.contains(uid)) {
                    // Employee was removed from warehouse members by manager
                    // Clear own warehouseId safely as profile owner
                    FirebaseFirestore.getInstance().collection("users").document(uid).update("warehouseId", "")

                    Toast.makeText(
                        this,
                        R.string.removed_by_manager_notice,
                        Toast.LENGTH_LONG
                    ).show()
                    val intent = Intent(this, JoinWarehouseActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    }
                    startActivity(intent)
                    finish()
                }
            }
    }


    private fun loadSessionFromProfile(reloadHome: Boolean) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        FirebaseRepository().getUser(uid)
            .addOnSuccessListener { user ->
                currentUser = user
                warehouseId = user.warehouseId
                applyRoleBasedUi()

                if (user is Employee) {
                    if (warehouseId.isBlank()) {
                        startActivity(Intent(this, JoinWarehouseActivity::class.java))
                        finish()
                        return@addOnSuccessListener
                    } else {
                        // Check if still a valid member of the warehouse
                        FirebaseRepository().getWarehouse(warehouseId).addOnSuccessListener { wh ->
                            if (!wh.members.contains(uid)) {
                                FirebaseRepository().updateUserWarehouseId(uid, "")
                                Toast.makeText(this, R.string.removed_by_manager_notice, Toast.LENGTH_LONG).show()
                                startActivity(Intent(this, JoinWarehouseActivity::class.java))
                                finish()
                            } else {
                                observeEmployeeWarehouseMembership()
                                if (reloadHome) {
                                    loadFragment(DashboardFragment.newInstance(warehouseId, accountType))
                                    selectTab(NavTab.HOME)
                                }
                            }
                        }.addOnFailureListener {
                            observeEmployeeWarehouseMembership()
                            if (reloadHome) {
                                loadFragment(DashboardFragment.newInstance(warehouseId, accountType))
                                selectTab(NavTab.HOME)
                            }
                        }
                        return@addOnSuccessListener
                    }
                }

                observeEmployeeWarehouseMembership()

                if (reloadHome) {
                    loadFragment(DashboardFragment.newInstance(warehouseId, accountType))
                    selectTab(NavTab.HOME)
                }
            }
    }

    /** Opens the navigation drawer from the start (left) side. */
    fun openDrawer() {
        if (::drawerLayout.isInitialized) {
            drawerLayout.openDrawer(GravityCompat.START)
        }
    }

    /** Handle back press using OnBackPressedDispatcher to close drawer first if open. */
    private fun setupOnBackPressed() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (::drawerLayout.isInitialized && drawerLayout.isDrawerOpen(GravityCompat.START)) {
                    drawerLayout.closeDrawer(GravityCompat.START)
                } else {
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                    isEnabled = true
                }
            }
        })
    }

    /** Opens the stock screen with the specified filter. */
    fun openStock(initialFilter: StockFragment.StatusFilter = StockFragment.StatusFilter.IN_STOCK) {
        loadFragment(StockFragment.newInstance(warehouseId, initialFilter))
        selectTab(NavTab.STOCK)
    }

    /** Replace the fragment inside the container. */
    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .commit()
    }

    /** Update icon tints and label colors to reflect the active tab. */
    private fun selectTab(selectedTab: NavTab) {
        if (!::navHomeIcon.isInitialized || !::navStockIcon.isInitialized ||
            !::navActivityIcon.isInitialized || !::navMembersIcon.isInitialized) return

        val activeColor = ContextCompat.getColor(this, R.color.pink)
        val inactiveColor = ContextCompat.getColor(this, R.color.hintGray)

        // Home tab
        val isHome = selectedTab == NavTab.HOME
        navHomeIcon.setColorFilter(if (isHome) activeColor else inactiveColor)
        navHomeLabel.setTextColor(if (isHome) activeColor else inactiveColor)
        navHomeLabel.setTypeface(null, if (isHome) android.graphics.Typeface.BOLD else android.graphics.Typeface.NORMAL)

        // Stock tab
        val isStock = selectedTab == NavTab.STOCK
        navStockIcon.setColorFilter(if (isStock) activeColor else inactiveColor)
        navStockLabel.setTextColor(if (isStock) activeColor else inactiveColor)
        navStockLabel.setTypeface(null, if (isStock) android.graphics.Typeface.BOLD else android.graphics.Typeface.NORMAL)

        // Activity Log tab (Manager only)
        val isActivity = selectedTab == NavTab.ACTIVITY
        navActivityIcon.setColorFilter(if (isActivity) activeColor else inactiveColor)
        navActivityLabel.setTextColor(if (isActivity) activeColor else inactiveColor)
        navActivityLabel.setTypeface(null, if (isActivity) android.graphics.Typeface.BOLD else android.graphics.Typeface.NORMAL)

        // Members tab (Manager only)
        val isMembers = selectedTab == NavTab.MEMBERS
        navMembersIcon.setColorFilter(if (isMembers) activeColor else inactiveColor)
        navMembersLabel.setTextColor(if (isMembers) activeColor else inactiveColor)
        navMembersLabel.setTypeface(null, if (isMembers) android.graphics.Typeface.BOLD else android.graphics.Typeface.NORMAL)
    }

    override fun onDestroy() {
        userProfileListener?.remove()
        userProfileListener = null
        super.onDestroy()
    }
}
