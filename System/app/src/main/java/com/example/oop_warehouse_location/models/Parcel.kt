package com.example.oop_warehouse_location.models

data class Parcel(
    var parcelID: String = "",
    var carrier: String = "",
    var receiptName: String = "",
    var status: String = STATUS_IN_STOCK,
    var recipientPhone: String = "",
    var deliveryHub: String = "",
    var zone: String = "",
    var rack: String = "",
    var binCoordinate: String = "",
    var parcelType: String = "",
    var handling: MutableList<String> = mutableListOf(),
    var createdAt: Long = 0L,
    var stagedAt: Long = 0L,
    var shippedAt: Long = 0L,
    var shippedByUserId: String = "",
    var shippedByUserName: String = "",
    var dispatchNotes: String = ""
) {
    fun updateStatus(newStatus: String) {
        status = newStatus
    }

    fun isInStock(): Boolean = status.equals(STATUS_IN_STOCK, ignoreCase = true)

    fun isOutboundStaged(): Boolean = status.equals(STATUS_OUTBOUND_STAGED, ignoreCase = true)

    fun isShipped(): Boolean =
        status.equals(STATUS_SHIPPED, ignoreCase = true) || status.equals(STATUS_SHIPPED_OUT, ignoreCase = true)

    fun isDueToAutoShip(now: Long = System.currentTimeMillis()): Boolean {
        if (!isOutboundStaged()) return false
        if (stagedAt <= 0L) return true
        return now - stagedAt >= AUTO_SHIP_DELAY_MS
    }

    companion object {
        const val STATUS_IN_STOCK = "In Stock"
        const val STATUS_OUTBOUND_STAGED = "Outbound Staged"
        const val STATUS_SHIPPED = "Shipped"
        const val STATUS_SHIPPED_OUT = "Shipped Out"
        const val AUTO_SHIP_DELAY_MS = 5_000L
    }
}
