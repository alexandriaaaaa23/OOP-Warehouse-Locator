package com.example.oop_warehouse_location.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.oop_warehouse_location.R
import com.example.oop_warehouse_location.adapters.ActivityLogAdapter
import com.example.oop_warehouse_location.repository.FirebaseRepository
import com.google.firebase.firestore.ListenerRegistration

class ActivityLogsFragment : Fragment() {

    private lateinit var repository: FirebaseRepository
    private var warehouseId: String = ""
    private var logsListener: ListenerRegistration? = null
    private val adapter = ActivityLogAdapter()
    private val cachedUserNames = mutableMapOf<String, String>()

    companion object {
        private const val ARG_WAREHOUSE_ID = "warehouseId"

        fun newInstance(warehouseId: String): ActivityLogsFragment {
            return ActivityLogsFragment().apply {
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
        val view = inflater.inflate(R.layout.fragment_activity_logs, container, false)

        val rv = view.findViewById<RecyclerView>(R.id.rvActivityLogs)
        rv.layoutManager = LinearLayoutManager(requireContext())
        rv.adapter = adapter

        val progressBar = view.findViewById<ProgressBar>(R.id.progressBar)
        val emptyState = view.findViewById<LinearLayout>(R.id.emptyState)
        val tvLogCount = view.findViewById<TextView>(R.id.tvLogCount)

        progressBar.visibility = View.VISIBLE

        logsListener = repository.observeActivityLogs { logs ->
            if (!isAdded || this.view == null) return@observeActivityLogs
            progressBar.visibility = View.GONE
            if (logs.isEmpty()) {
                emptyState.visibility = View.VISIBLE
                rv.visibility = View.GONE
                adapter.submitList(emptyList())
                tvLogCount.text = "0 logs recorded"
            } else {
                emptyState.visibility = View.GONE
                rv.visibility = View.VISIBLE
                tvLogCount.text = "${logs.size} log${if (logs.size == 1) "" else "s"} recorded"

                val userIds = logs.map { it.userId }.filter { it.isNotBlank() }.distinct()
                val missingUserIds = userIds.filter { !cachedUserNames.containsKey(it) }

                if (missingUserIds.isNotEmpty()) {
                    repository.getUsersByIds(missingUserIds).addOnSuccessListener { users ->
                        if (!isAdded) return@addOnSuccessListener
                        users.forEach { user ->
                            val fullName = "${user.firstName} ${user.lastName}".trim()
                            cachedUserNames[user.uid] = if (fullName.isNotBlank()) fullName else user.email
                        }
                        adapter.submitList(logs, cachedUserNames)
                    }.addOnFailureListener {
                        if (isAdded) {
                            adapter.submitList(logs, cachedUserNames)
                        }
                    }
                } else {
                    adapter.submitList(logs, cachedUserNames)
                }
            }
        }

        return view
    }

    override fun onDestroyView() {
        logsListener?.remove()
        logsListener = null
        super.onDestroyView()
    }
}

