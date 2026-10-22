package com.example.oop_warehouse_location.models

data class Warehouse(
    var warehouseId: String = "WH-01",
    var name: String = "Main Warehouse",
    var totalCapacity: Int = 500
)
