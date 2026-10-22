package com.example.oop_warehouse_location.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.fragment.app.Fragment
import com.example.oop_warehouse_location.R
import com.example.oop_warehouse_location.activities.MainActivity

class DashboardFragment : Fragment() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_dashboard, container, false)

        // Hamburger menu button opens the navigation drawer
        val menuButton = view.findViewById<ImageView>(R.id.ivMenuButton)
        menuButton.setOnClickListener {
            (activity as? MainActivity)?.openDrawer()
        }

        return view
    }
}