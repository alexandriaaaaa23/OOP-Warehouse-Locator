package com.example.oop_warehouse_location.repository

import com.example.oop_warehouse_location.models.ActivityLog
import com.example.oop_warehouse_location.models.Parcel
import com.example.oop_warehouse_location.models.Warehouse
import com.example.oop_warehouse_location.models.user.User
import com.example.oop_warehouse_location.repository.firebase.FirebaseActivityLogRepository
import com.example.oop_warehouse_location.repository.firebase.FirebaseParcelRepository
import com.example.oop_warehouse_location.repository.firebase.FirebaseUserRepository
import com.example.oop_warehouse_location.repository.firebase.FirebaseWarehouseRepository
import com.google.android.gms.tasks.Task
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

class FirebaseRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private var warehouseId: String = ""
) {
    private val userRepo = FirebaseUserRepository(db)
    private val warehouseRepo = FirebaseWarehouseRepository(db, warehouseId)
    private val activityLogRepo = FirebaseActivityLogRepository(db, warehouseId)
    private val parcelRepo = FirebaseParcelRepository(db, warehouseId, activityLogRepo)

    fun setWarehouseId(id: String) {
        warehouseId = id
        warehouseRepo.warehouseId = id
        parcelRepo.warehouseId = id
        activityLogRepo.warehouseId = id
    }

    // ── User CRUD ────────────────────────────────────────────────────────

    fun saveUser(user: User): Task<Void> = userRepo.saveUser(user)

    fun getUser(uid: String): Task<User> = userRepo.getUser(uid)

    fun updateUserWarehouseId(uid: String, warehouseId: String): Task<Void> =
        userRepo.updateUserWarehouseId(uid, warehouseId)

    fun getUsersByIds(uids: List<String>): Task<List<User>> = userRepo.getUsersByIds(uids)

    // ── Warehouse CRUD ───────────────────────────────────────────────────

    fun createWarehouse(warehouse: Warehouse): Task<Void> = warehouseRepo.createWarehouse(warehouse)

    fun getWarehouse(warehouseId: String): Task<Warehouse> = warehouseRepo.getWarehouse(warehouseId)

    fun getWarehouseByInviteCode(inviteCode: String): Task<Warehouse?> =
        warehouseRepo.getWarehouseByInviteCode(inviteCode)

    fun addMemberToWarehouse(warehouseId: String, uid: String): Task<Void> =
        warehouseRepo.addMemberToWarehouse(warehouseId, uid)

    fun joinWarehouse(warehouseId: String, uid: String): Task<Void> =
        warehouseRepo.joinWarehouse(warehouseId, uid)

    fun removeMemberFromWarehouse(
        warehouseId: String,
        memberUid: String,
        memberName: String,
        managerUid: String
    ): Task<Void> = warehouseRepo.removeMemberFromWarehouse(warehouseId, memberUid, memberName, managerUid)

    fun joinWarehouseByInviteCode(inviteCode: String, uid: String): Task<Warehouse> =
        warehouseRepo.joinWarehouseByInviteCode(inviteCode, uid)

    fun generateUniqueInviteCode(remainingAttempts: Int = 8): Task<String> =
        warehouseRepo.generateUniqueInviteCode(remainingAttempts)

    // ── Parcel CRUD (warehouse-scoped) ───────────────────────────────────

    fun saveParcel(parcel: Parcel): Task<Void> = parcelRepo.saveParcel(parcel)

    fun getParcel(parcelId: String): Task<Parcel> = parcelRepo.getParcel(parcelId)

    fun stageForOutbound(parcelId: String): Task<Void> = parcelRepo.stageForOutbound(parcelId)

    fun completeShipment(parcelId: String, notes: String = ""): Task<Void> =
        parcelRepo.completeShipment(parcelId, notes)

    fun returnToStock(
        parcelId: String,
        targetZone: String = "Zone A",
        targetRack: String = "Rack 01 - Top",
        targetBin: String = ""
    ): Task<Void> = parcelRepo.returnToStock(parcelId, targetZone, targetRack, targetBin)

    fun observeParcels(onResult: (List<Parcel>) -> Unit): ListenerRegistration =
        parcelRepo.observeParcels(onResult)

    // ── Activity Log (warehouse-scoped) ──────────────────────────────────

    fun logActivity(activityLog: ActivityLog): Task<Void> = activityLogRepo.logActivity(activityLog)

    fun observeActivityLogs(onResult: (List<ActivityLog>) -> Unit): ListenerRegistration =
        activityLogRepo.observeActivityLogs(onResult)
}
