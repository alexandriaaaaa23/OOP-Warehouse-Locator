package com.example.oop_warehouse_location.activities

import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.oop_warehouse_location.R
import com.example.oop_warehouse_location.adapters.ActivityLogAdapter
import com.example.oop_warehouse_location.repository.FirebaseRepository
import com.google.firebase.firestore.ListenerRegistration

class ActivityLogsActivity : AppCompatActivity() {

    private lateinit var repository: FirebaseRepository
    private var logsListener: ListenerRegistration? = null
    private val adapter = ActivityLogAdapter()
    private val cachedUserNames = mutableMapOf<String, String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_activity_logs)

        val root = findViewById<View>(android.R.id.content)
        ViewCompat.setOnApplyWindowInsetsListener(root) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(0, bars.top, 0, bars.bottom)
            insets
        }

        val warehouseId = intent.getStringExtra("warehouseId").orEmpty()
        repository = FirebaseRepository(warehouseId = warehouseId)

        findViewById<ImageView>(R.id.btnBack).setOnClickListener {
            finish()
        }

        val rv = findViewById<RecyclerView>(R.id.rvActivityLogs)
        rv.layoutManager = LinearLayoutManager(this)
        rv.adapter = adapter

        val progressBar = findViewById<ProgressBar>(R.id.progressBar)
        val tvEmpty = findViewById<TextView>(R.id.tvEmptyLogs)
        val tvLogCount = findViewById<TextView>(R.id.tvLogCount)

        progressBar.visibility = View.VISIBLE

        logsListener = repository.observeActivityLogs { logs ->
            progressBar.visibility = View.GONE
            if (logs.isEmpty()) {
                tvEmpty.visibility = View.VISIBLE
                adapter.submitList(emptyList())
                tvLogCount.text = "0 logs recorded"
            } else {
                tvEmpty.visibility = View.GONE
                tvLogCount.text = "${logs.size} log${if (logs.size == 1) "" else "s"} recorded"

                val userIds = logs.map { it.userId }.filter { it.isNotBlank() }.distinct()
                val missingUserIds = userIds.filter { !cachedUserNames.containsKey(it) }

                if (missingUserIds.isNotEmpty()) {
                    repository.getUsersByIds(missingUserIds).addOnSuccessListener { users ->
                        users.forEach { user ->
                            val fullName = "${user.firstName} ${user.lastName}".trim()
                            cachedUserNames[user.uid] = if (fullName.isNotBlank()) fullName else user.email
                        }
                        adapter.submitList(logs, cachedUserNames)
                    }.addOnFailureListener {
                        adapter.submitList(logs, cachedUserNames)
                    }
                } else {
                    adapter.submitList(logs, cachedUserNames)
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        logsListener?.remove()
    }
}
