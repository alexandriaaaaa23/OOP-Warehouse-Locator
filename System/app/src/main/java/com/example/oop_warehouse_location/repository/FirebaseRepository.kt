package com.example.oop_warehouse_location.repository

import com.example.oop_warehouse_location.models.ActivityLog
import com.example.oop_warehouse_location.models.Parcel
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query

class FirebaseRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    fun saveParcel(parcel: Parcel): Task<Void> {
        return db.collection(COLLECTION_PARCELS)
            .document(parcel.parcelID)
            .set(parcel)
    }

    fun getParcel(parcelId: String): Task<Parcel> {
        return db.collection(COLLECTION_PARCELS)
            .document(parcelId)
            .get()
            .continueWithTask { task ->
                if (!task.isSuccessful) {
                    Tasks.forException(task.exception ?: Exception("Failed to load parcel"))
                } else {
                    val parcel = task.result?.toObject(Parcel::class.java)
                    if (parcel != null) {
                        Tasks.forResult(parcel)
                    } else {
                        Tasks.forException(Exception("Parcel not found"))
                    }
                }
            }
    }

    fun logActivity(activityLog: ActivityLog): Task<Void> {
        activityLog.createLog()
        return db.collection(COLLECTION_ACTIVITY_LOGS)
            .document(activityLog.logId)
            .set(activityLog)
    }

    fun observeParcels(onResult: (List<Parcel>) -> Unit): ListenerRegistration {
        return db.collection(COLLECTION_PARCELS)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) {
                    onResult(emptyList())
                    return@addSnapshotListener
                }
                val parcels = snapshot.documents.mapNotNull { it.toObject(Parcel::class.java) }
                onResult(parcels)
            }
    }

    companion object {
        private const val COLLECTION_PARCELS = "parcels"
        private const val COLLECTION_ACTIVITY_LOGS = "activityLogs"
    }
}
