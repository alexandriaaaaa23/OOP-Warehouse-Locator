package com.example.oop_warehouse_location.models.parcel

/**
 * POLYMORPHISM via sealed class — restricted hierarchy for parcel states.
 *
 * Each status object knows its own transition rules, eliminating scattered
 * string comparisons across the codebase.
 */
sealed class ParcelStatus(val label: String) {

    /** Parcel is stored in the warehouse. */
    object InStock : ParcelStatus("In Stock") {
        override fun canStageForOutbound(): Boolean = true
        override fun canShip(): Boolean = false
        override fun canReturnToStock(): Boolean = false
    }

    /** Parcel is staged in the outbound area, awaiting shipment. */
    object OutboundStaged : ParcelStatus("Outbound Staged") {
        override fun canStageForOutbound(): Boolean = false
        override fun canShip(): Boolean = true
        override fun canReturnToStock(): Boolean = true
    }

    /** Parcel has been shipped out of the warehouse. */
    object Shipped : ParcelStatus("Shipped") {
        override fun canStageForOutbound(): Boolean = false
        override fun canShip(): Boolean = false
        override fun canReturnToStock(): Boolean = true
    }

    /** POLYMORPHISM — each status knows if it allows state transitions. */
    abstract fun canStageForOutbound(): Boolean
    abstract fun canShip(): Boolean
    abstract fun canReturnToStock(): Boolean

    override fun toString(): String = label

    companion object {
        /**
         * Converts a string value (from Firestore) to the corresponding [ParcelStatus].
         * Defaults to [InStock] for unknown values.
         */
        fun fromString(value: String): ParcelStatus = when {
            value.equals("In Stock", true) -> InStock
            value.equals("Outbound Staged", true) -> OutboundStaged
            value.equals("Shipped", true) || value.equals("Shipped Out", true) -> Shipped
            else -> InStock
        }
    }
}

