package com.example.oop_warehouse_location.repository

import com.example.oop_warehouse_location.models.ActivityLog
import com.example.oop_warehouse_location.models.Parcel
import com.example.oop_warehouse_location.models.User
import com.example.oop_warehouse_location.models.Warehouse
import com.example.oop_warehouse_location.utils.InviteCodeGenerator
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query

class FirebaseRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private var warehouseId: String = ""
) {

    /** Update the warehouse scope at runtime. */
    fun setWarehouseId(id: String) {
        warehouseId = id
    }

    // ── User CRUD ────────────────────────────────────────────────────────

    fun saveUser(user: User): Task<Void> {
        return db.collection(COLLECTION_USERS)
            .document(user.uid)
            .set(user)
    }

    fun getUser(uid: String): Task<User> {
        return db.collection(COLLECTION_USERS)
            .document(uid)
            .get()
            .continueWithTask { task ->
                if (!task.isSuccessful) {
                    Tasks.forException(task.exception ?: Exception("Failed to load user"))
                } else {
                    val user = task.result?.toObject(User::class.java)
                    if (user != null) {
                        Tasks.forResult(user)
                    } else {
                        Tasks.forException(Exception("User not found"))
                    }
                }
            }
    }

    fun updateUserWarehouseId(uid: String, warehouseId: String): Task<Void> {
        return db.collection(COLLECTION_USERS)
            .document(uid)
            .update("warehouseId", warehouseId)
    }

    // ── Warehouse CRUD ───────────────────────────────────────────────────

    fun createWarehouse(warehouse: Warehouse): Task<Void> {
        return db.collection(COLLECTION_WAREHOUSES)
            .document(warehouse.warehouseId)
            .set(warehouse)
    }

    fun getWarehouse(warehouseId: String): Task<Warehouse> {
        if (warehouseId.isBlank()) {
            return Tasks.forException(IllegalArgumentException("Warehouse ID cannot be empty"))
        }
        return db.collection(COLLECTION_WAREHOUSES)
            .document(warehouseId)
            .get()
            .continueWithTask { task ->
                if (!task.isSuccessful) {
                    Tasks.forException(task.exception ?: Exception("Failed to load warehouse"))
                } else {
                    val wh = task.result?.toObject(Warehouse::class.java)
                    if (wh != null) {
                        Tasks.forResult(wh)
                    } else {
                        Tasks.forException(Exception("Warehouse not found"))
                    }
                }
            }
    }

    fun getWarehouseByInviteCode(inviteCode: String): Task<Warehouse?> {
        if (inviteCode.isBlank()) {
            return Tasks.forResult(null)
        }
        return db.collection(COLLECTION_WAREHOUSES)
            .whereEqualTo("inviteCode", inviteCode)
            .limit(1)
            .get()
            .continueWith { task ->
                if (!task.isSuccessful) {
                    throw task.exception ?: Exception("Query failed")
                }
                val doc = task.result?.documents?.firstOrNull() ?: return@continueWith null
                doc.toObject(Warehouse::class.java)?.apply {
                    if (warehouseId.isBlank()) warehouseId = doc.id
                }
            }
    }

    fun addMemberToWarehouse(warehouseId: String, uid: String): Task<Void> {
        if (warehouseId.isBlank()) {
            return Tasks.forException(IllegalArgumentException("Warehouse ID cannot be empty"))
        }
        return db.collection(COLLECTION_WAREHOUSES)
            .document(warehouseId)
            .update("members", FieldValue.arrayUnion(uid))
    }

    /**
     * Generates a 6-character invite code that is not already used by another warehouse.
     */
    fun generateUniqueInviteCode(remainingAttempts: Int = 8): Task<String> {
        if (remainingAttempts <= 0) {
            return Tasks.forException(Exception("Could not generate a unique invite code"))
        }
        val code = InviteCodeGenerator.generate()
        return getWarehouseByInviteCode(code).continueWithTask { task ->
            if (!task.isSuccessful) {
                Tasks.forException(task.exception ?: Exception("Failed to check invite code"))
            } else if (task.result != null) {
                generateUniqueInviteCode(remainingAttempts - 1)
            } else {
                Tasks.forResult(code)
            }
        }
    }

    fun getUsersByIds(uids: List<String>): Task<List<User>> {
        val uniqueIds = uids.filter { it.isNotBlank() }.distinct()
        if (uniqueIds.isEmpty()) {
            return Tasks.forResult(emptyList())
        }
        val reads = uniqueIds.map { uid ->
            db.collection(COLLECTION_USERS)
                .document(uid)
                .get()
                .continueWith { task ->
                    if (task.isSuccessful) {
                        task.result?.toObject(User::class.java)
                    } else {
                        null
                    }
                }
        }
        return Tasks.whenAllSuccess<User?>(reads).continueWith { task ->
            task.result.filterNotNull()
        }
    }

    // ── Parcel CRUD (warehouse-scoped) ───────────────────────────────────

    fun saveParcel(parcel: Parcel): Task<Void> {
        if (warehouseId.isBlank()) {
            return Tasks.forException(IllegalArgumentException("Warehouse ID cannot be empty"))
        }
        return db.collection(COLLECTION_WAREHOUSES)
            .document(warehouseId)
            .collection(COLLECTION_PARCELS)
            .document(parcel.parcelID)
            .set(parcel)
    }

    fun getParcel(parcelId: String): Task<Parcel> {
        if (warehouseId.isBlank() || parcelId.isBlank()) {
            return Tasks.forException(IllegalArgumentException("Warehouse or Parcel ID cannot be empty"))
        }
        return db.collection(COLLECTION_WAREHOUSES)
            .document(warehouseId)
            .collection(COLLECTION_PARCELS)
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

    fun observeParcels(onResult: (List<Parcel>) -> Unit): ListenerRegistration {
        if (warehouseId.isBlank()) {
            onResult(emptyList())
            return ListenerRegistration { }
        }
        return db.collection(COLLECTION_WAREHOUSES)
            .document(warehouseId)
            .collection(COLLECTION_PARCELS)
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

    // ── Activity Log (warehouse-scoped) ──────────────────────────────────

    fun logActivity(activityLog: ActivityLog): Task<Void> {
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

    fun observeActivityLogs(onResult: (List<ActivityLog>) -> Unit): ListenerRegistration {
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
        private const val COLLECTION_USERS = "users"
        private const val COLLECTION_WAREHOUSES = "warehouses"
        private const val COLLECTION_PARCELS = "parcels"
        private const val COLLECTION_ACTIVITY_LOGS = "activityLogs"
    }
}
