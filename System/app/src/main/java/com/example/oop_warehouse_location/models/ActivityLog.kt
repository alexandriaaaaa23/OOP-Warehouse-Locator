package com.example.oop_warehouse_location.models

import com.example.oop_warehouse_location.models.base.BaseModel
import java.util.UUID

/**
 * ActivityLog entity — extends [BaseModel] for shared identity and timestamp.
 * Demonstrates INHERITANCE from the abstract base model.
 * Records an auditable action performed on a parcel within a warehouse.
 */
data class ActivityLog(
    var logId: String = "",
    var parcelId: String = "",
    var userId: String = "",
    var action: String = "",
    var timestamp: Long = 0L
) : BaseModel(id = logId, createdAt = timestamp) {

    /**
     * Initializes the log entry with a unique ID and timestamp if not already set.
     * Overrides [BaseModel.initialize] for ActivityLog-specific initialization.
     */
    fun createLog() {
        if (logId.isBlank()) {
            logId = UUID.randomUUID().toString()
            id = logId
        }
        if (timestamp == 0L) {
            timestamp = System.currentTimeMillis()
            createdAt = timestamp
        }
    }

    override fun displaySummary(): String = "$action (parcel: $parcelId)"
}
