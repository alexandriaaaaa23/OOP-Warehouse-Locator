package com.example.oop_warehouse_location.repository.firebase

import com.example.oop_warehouse_location.models.ActivityLog
import com.example.oop_warehouse_location.models.Warehouse
import com.example.oop_warehouse_location.repository.WarehouseRepository
import com.example.oop_warehouse_location.utils.InviteCodeGenerator
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

/**
 * Concrete implementation of [WarehouseRepository] using Cloud Firestore.
 * Demonstrates POLYMORPHISM through interface implementation.
 */
class FirebaseWarehouseRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance(),
    var warehouseId: String = ""
) : WarehouseRepository {

    override fun save(item: Warehouse): Task<Void> = createWarehouse(item)

    override fun getById(id: String): Task<Warehouse> = getWarehouse(id)

    override fun delete(id: String): Task<Void> {
        return db.collection(COLLECTION_WAREHOUSES).document(id).delete()
    }

    override fun createWarehouse(warehouse: Warehouse): Task<Void> {
        return db.collection(COLLECTION_WAREHOUSES)
            .document(warehouse.warehouseId)
            .set(warehouse)
    }

    override fun getWarehouse(warehouseId: String): Task<Warehouse> {
        val targetId = warehouseId.ifBlank { this.warehouseId }
        if (targetId.isBlank()) {
            return Tasks.forException(IllegalArgumentException("Warehouse ID cannot be empty"))
        }
        return db.collection(COLLECTION_WAREHOUSES)
            .document(targetId)
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

    override fun getWarehouseByInviteCode(inviteCode: String): Task<Warehouse?> {
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

    override fun addMemberToWarehouse(warehouseId: String, uid: String): Task<Void> {
        val targetId = warehouseId.ifBlank { this.warehouseId }
        if (targetId.isBlank()) {
            return Tasks.forException(IllegalArgumentException("Warehouse ID cannot be empty"))
        }
        return db.collection(COLLECTION_WAREHOUSES)
            .document(targetId)
            .update("members", FieldValue.arrayUnion(uid))
    }

    override fun joinWarehouse(warehouseId: String, uid: String): Task<Void> {
        val targetId = warehouseId.ifBlank { this.warehouseId }
        if (targetId.isBlank() || uid.isBlank()) {
            return Tasks.forException(IllegalArgumentException("Warehouse ID and UID cannot be empty"))
        }
        val batch = db.batch()
        val warehouseRef = db.collection(COLLECTION_WAREHOUSES).document(targetId)
        val userRef = db.collection(COLLECTION_USERS).document(uid)

        batch.update(warehouseRef, "members", FieldValue.arrayUnion(uid))
        batch.update(userRef, "warehouseId", targetId)

        return batch.commit()
    }

    override fun removeMemberFromWarehouse(
        warehouseId: String,
        memberUid: String,
        memberName: String,
        managerUid: String
    ): Task<Void> {
        val targetId = warehouseId.ifBlank { this.warehouseId }
        if (targetId.isBlank() || memberUid.isBlank()) {
            return Tasks.forException(IllegalArgumentException("Warehouse ID and Member UID cannot be empty"))
        }

        val batch = db.batch()
        val warehouseRef = db.collection(COLLECTION_WAREHOUSES).document(targetId)

        // 1. Remove employee UID from warehouse members list
        batch.update(warehouseRef, "members", FieldValue.arrayRemove(memberUid))

        // 2. Record Activity Log
        val log = ActivityLog(
            parcelId = "",
            userId = managerUid,
            action = "Removed employee $memberName from warehouse"
        ).apply { createLog() }

        val logRef = db.collection(COLLECTION_WAREHOUSES)
            .document(targetId)
            .collection(COLLECTION_ACTIVITY_LOGS)
            .document(log.logId)

        batch.set(logRef, log)

        return batch.commit()
    }

    override fun joinWarehouseByInviteCode(inviteCode: String, uid: String): Task<Warehouse> {
        if (inviteCode.isBlank() || uid.isBlank()) {
            return Tasks.forException(IllegalArgumentException("Invite code and UID cannot be empty"))
        }
        return getWarehouseByInviteCode(inviteCode).continueWithTask { task ->
            if (!task.isSuccessful) {
                Tasks.forException(task.exception ?: Exception("Failed to query invite code"))
            } else {
                val warehouse = task.result ?: return@continueWithTask Tasks.forException(Exception("Invalid invite code"))
                joinWarehouse(warehouse.warehouseId, uid).continueWith { joinTask ->
                    if (!joinTask.isSuccessful) {
                        throw joinTask.exception ?: Exception("Failed to join warehouse")
                    }
                    warehouse
                }
            }
        }
    }

    override fun generateUniqueInviteCode(remainingAttempts: Int): Task<String> {
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

    companion object {
        const val COLLECTION_WAREHOUSES = "warehouses"
        const val COLLECTION_USERS = "users"
        const val COLLECTION_ACTIVITY_LOGS = "activityLogs"
    }
}

