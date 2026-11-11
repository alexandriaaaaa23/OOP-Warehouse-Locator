package com.example.oop_warehouse_location.repository.firebase

import com.example.oop_warehouse_location.models.ActivityLog
import com.example.oop_warehouse_location.repository.ActivityLogRepository
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query

/**
 * Concrete implementation of [ActivityLogRepository] using Cloud Firestore.
 * Demonstrates POLYMORPHISM through interface implementation.
 */
class FirebaseActivityLogRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance(),
    var warehouseId: String = ""
) : ActivityLogRepository {

    override fun save(item: ActivityLog): Task<Void> = logActivity(item)

    override fun getById(id: String): Task<ActivityLog> {
        if (warehouseId.isBlank() || id.isBlank()) {
            return Tasks.forException(IllegalArgumentException("Warehouse or Log ID cannot be empty"))
        }
        return db.collection(COLLECTION_WAREHOUSES)
            .document(warehouseId)
            .collection(COLLECTION_ACTIVITY_LOGS)
            .document(id)
            .get()
            .continueWithTask { task ->
                if (!task.isSuccessful) {
                    Tasks.forException(task.exception ?: Exception("Failed to load activity log"))
                } else {
                    val log = task.result?.toObject(ActivityLog::class.java)
                    if (log != null) {
                        Tasks.forResult(log)
                    } else {
                        Tasks.forException(Exception("Log not found"))
                    }
                }
            }
    }

    override fun delete(id: String): Task<Void> {
        if (warehouseId.isBlank() || id.isBlank()) {
            return Tasks.forException(IllegalArgumentException("Warehouse or Log ID cannot be empty"))
        }
        return db.collection(COLLECTION_WAREHOUSES)
            .document(warehouseId)
            .collection(COLLECTION_ACTIVITY_LOGS)
            .document(id)
            .delete()
    }

    override fun logActivity(activityLog: ActivityLog): Task<Void> {
        if (warehouseId.isBlank()) {
            return Tasks.forException(IllegalArgumentException("Warehouse ID cannot be empty"))
        }
        activityLog.createLog()
        return db.collection(COLLECTION_WAREHOUSES)
            .document(warehouseId)
            .collection(COLLECTION_ACTIVITY_LOGS)
            .document(activityLog.logId)
            .set(activityLog)
    }

    override fun observeActivityLogs(onResult: (List<ActivityLog>) -> Unit): ListenerRegistration {
        if (warehouseId.isBlank()) {
            onResult(emptyList())
            return ListenerRegistration { }
        }
        return db.collection(COLLECTION_WAREHOUSES)
            .document(warehouseId)
            .collection(COLLECTION_ACTIVITY_LOGS)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) {
                    onResult(emptyList())
                    return@addSnapshotListener
                }
                val logs = snapshot.documents.mapNotNull { it.toObject(ActivityLog::class.java) }
                onResult(logs)
            }
    }

    companion object {
        const val COLLECTION_WAREHOUSES = "warehouses"
        const val COLLECTION_ACTIVITY_LOGS = "activityLogs"
    }
}

