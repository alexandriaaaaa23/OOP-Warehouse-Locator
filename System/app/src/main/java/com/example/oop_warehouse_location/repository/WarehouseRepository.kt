package com.example.oop_warehouse_location.repository

import com.example.oop_warehouse_location.models.Warehouse
import com.example.oop_warehouse_location.repository.base.BaseRepository
import com.google.android.gms.tasks.Task


interface WarehouseRepository : BaseRepository<Warehouse> {
    fun createWarehouse(warehouse: Warehouse): Task<Void>
    fun getWarehouse(warehouseId: String): Task<Warehouse>
    fun getWarehouseByInviteCode(inviteCode: String): Task<Warehouse?>
    fun addMemberToWarehouse(warehouseId: String, uid: String): Task<Void>
    fun joinWarehouse(warehouseId: String, uid: String): Task<Void>
    fun removeMemberFromWarehouse(
        warehouseId: String,
        memberUid: String,
        memberName: String,
        managerUid: String
    ): Task<Void>
    fun joinWarehouseByInviteCode(inviteCode: String, uid: String): Task<Warehouse>
    fun generateUniqueInviteCode(remainingAttempts: Int = 8): Task<String>
}

