package com.example.oop_warehouse_location.models.base

/**
 * Abstract base for all Firestore-backed domain entities.
 * Demonstrates ABSTRACTION — common identity and timestamp fields
 * are defined once and inherited by all models.
 */
abstract class BaseModel(
    open var id: String = "",
    open var createdAt: Long = 0L
) {
    /** Template method — subclasses may override to add custom init logic. */
    open fun initialize() {
        if (createdAt == 0L) {
            createdAt = System.currentTimeMillis()
        }
    }

    /** Returns a display-friendly summary. Subclasses override for POLYMORPHISM. */
    abstract fun displaySummary(): String
}

