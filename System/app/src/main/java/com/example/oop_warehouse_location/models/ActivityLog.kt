package com.example.oop_warehouse_location.models

import java.util.UUID

data class ActivityLog(
    var logId: String = "",
    var parcelId: String = "",
    var userId: String = "",
    var action: String = "",
    var timestamp: Long = 0L
) {
    fun createLog() {
        if (logId.isBlank()) {
            logId = UUID.randomUUID().toString()
        }
        if (timestamp == 0L) {
            timestamp = System.currentTimeMillis()
        }
    }
}
