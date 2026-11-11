package com.example.oop_warehouse_location.repository

import com.example.oop_warehouse_location.models.ActivityLog
import com.example.oop_warehouse_location.repository.base.BaseRepository
import com.google.android.gms.tasks.Task
import com.google.firebase.firestore.ListenerRegistration

interface ActivityLogRepository : BaseRepository<ActivityLog> {
    fun logActivity(activityLog: ActivityLog): Task<Void>
    fun observeActivityLogs(onResult: (List<ActivityLog>) -> Unit): ListenerRegistration
}

