package com.example.oop_warehouse_location.models

data class User(
    var uid: String = "",
    var firstName: String = "",
    var lastName: String = "",
    var email: String = "",
    var accountType: String = "",      // "Manager" or "Employee"
    var warehouseId: String = "",
    var createdAt: Long = 0L
)
