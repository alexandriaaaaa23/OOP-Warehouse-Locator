package com.example.oop_warehouse_location.activities

import android.content.Intent
import android.os.Bundle
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
import com.example.oop_warehouse_location.fragments.StockFragment
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        // Drawer setup
        drawerLayout = findViewById(R.id.drawerLayout)
        navigationView = findViewById(R.id.navigationView)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Populate nav header with Firebase user details
        val headerView = navigationView.getHeaderView(0)
        val tvName = headerView.findViewById<TextView>(R.id.navHeaderName)
        val tvEmail = headerView.findViewById<TextView>(R.id.navHeaderEmail)
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser != null) {
            tvName.text = currentUser.displayName ?: "User"
            tvEmail.text = currentUser.email ?: ""
        }

        // Handle drawer menu item clicks
        navigationView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_home -> {
                    loadFragment(DashboardFragment())
                    selectTab(isHomeSelected = true)
                    drawerLayout.closeDrawer(GravityCompat.START)
                    true
                }
                R.id.nav_stock -> {
                    loadFragment(StockFragment())
                    selectTab(isHomeSelected = false)
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

        // Bind navigation views
        navHome = findViewById(R.id.navHome)
        navStock = findViewById(R.id.navStock)
        navAdd = findViewById(R.id.navAdd)
        navHomeIcon = findViewById(R.id.navHomeIcon)
        navHomeLabel = findViewById(R.id.navHomeLabel)
        navStockIcon = findViewById(R.id.navStockIcon)
        navStockLabel = findViewById(R.id.navStockLabel)

       // DashboardFragment df
        if (savedInstanceState == null) {
            loadFragment(DashboardFragment())
            selectTab(isHomeSelected = true)
        }

        // Home tab click
        navHome.setOnClickListener {
            loadFragment(DashboardFragment())
            selectTab(isHomeSelected = true)
        }

        // Stock tab click
        navStock.setOnClickListener {
            loadFragment(StockFragment())
            selectTab(isHomeSelected = false)
        }

        // Center FAB — open Add Inventory screen
        navAdd.setOnClickListener {
            startActivity(Intent(this, AddInventoryActivity::class.java))
        }
    }

    /** Opens the navigation drawer from the start (left) side. */
    fun openDrawer() {
        drawerLayout.openDrawer(GravityCompat.START)
    }

    /** Handle back press to close drawer first if open. */
    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
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