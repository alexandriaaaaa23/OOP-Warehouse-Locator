package com.example.oop_warehouse_location.models

data class Warehouse(
    var warehouseId: String = "",
    var name: String = "Main Warehouse",
    var totalCapacity: Int = 500,
    var ownerId: String = "",              // Manager's UID
    var inviteCode: String = "",           // 6-char alphanumeric code
    var members: MutableList<String> = mutableListOf(),  // employee UIDs
    var createdAt: Long = 0L
)
