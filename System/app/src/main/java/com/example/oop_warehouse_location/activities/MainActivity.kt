package com.example.oop_warehouse_location.activities

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import com.example.oop_warehouse_location.fragments.DashboardFragment
import com.example.oop_warehouse_location.R
import com.example.oop_warehouse_location.fragments.StockFragment

class MainActivity : AppCompatActivity() {

    private lateinit var navHome: LinearLayout
    private lateinit var navStock: LinearLayout
    private lateinit var navAdd: ImageView
    private lateinit var navHomeIcon: ImageView
    private lateinit var navHomeLabel: TextView
    private lateinit var navStockIcon: ImageView
    private lateinit var navStockLabel: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Bind navigation views
        navHome = findViewById(R.id.navHome)
        navStock = findViewById(R.id.navStock)
        navAdd = findViewById(R.id.navAdd)
        navHomeIcon = findViewById(R.id.navHomeIcon)
        navHomeLabel = findViewById(R.id.navHomeLabel)
        navStockIcon = findViewById(R.id.navStockIcon)
        navStockLabel = findViewById(R.id.navStockLabel)

        // Load the default fragment (Dashboard) only on first launch
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