package com.example.oop_warehouse_location.models

data class Parcel(
    var parcelID: String = "",
    var carrier: String = "",
    var receiptName: String = "",
    var status: String = "In Stock",
    var recipientPhone: String = "",
    var deliveryHub: String = "",
    var zone: String = "",
    var rack: String = "",
    var binCoordinate: String = "",
    var parcelType: String = "",
    var handling: MutableList<String> = mutableListOf(),
    var createdAt: Long = 0L
) {
    fun updateStatus(newStatus: String) {
        status = newStatus
    }
}
