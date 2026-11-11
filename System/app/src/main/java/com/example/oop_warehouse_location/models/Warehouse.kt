package com.example.oop_warehouse_location.models

import com.example.oop_warehouse_location.models.base.BaseModel

/**
 * Warehouse entity — extends [BaseModel] for shared identity and timestamp.
 * Demonstrates INHERITANCE from the abstract base model.
 */
data class Warehouse(
    var warehouseId: String = "",
    var name: String = "Main Warehouse",
    var totalCapacity: Int = 500,
    var ownerId: String = "",              // Manager's UID
    var inviteCode: String = "",           // 6-char alphanumeric code
    var members: MutableList<String> = mutableListOf(),  // employee UIDs
    override var createdAt: Long = 0L
) : BaseModel(id = warehouseId, createdAt = createdAt) {

    override fun displaySummary(): String = "Warehouse: $name (capacity: $totalCapacity)"
}
