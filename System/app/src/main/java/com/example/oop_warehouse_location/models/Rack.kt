package com.example.oop_warehouse_location.models

import com.example.oop_warehouse_location.models.base.BaseModel

/**
 * Rack entity — extends [BaseModel] for shared identity and timestamp.
 * Represents a physical rack within a zone.
 */
data class Rack(
    var rackId: String = "",
    var rackNumber: Int = 0,
    override var createdAt: Long = 0L
) : BaseModel(id = rackId, createdAt = createdAt) {

    override fun displaySummary(): String = "Rack #$rackNumber"
}
