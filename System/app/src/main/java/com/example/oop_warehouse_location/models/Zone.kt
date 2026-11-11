package com.example.oop_warehouse_location.models

import com.example.oop_warehouse_location.models.base.BaseModel

/**
 * Zone entity — extends [BaseModel] for shared identity and timestamp.
 * Represents a logical area within a warehouse (e.g., "Zone A", "Staging").
 */
data class Zone(
    var zoneId: String = "",
    var type: String = "",
    override var createdAt: Long = 0L
) : BaseModel(id = zoneId, createdAt = createdAt) {

    override fun displaySummary(): String = "Zone: $type"
}
