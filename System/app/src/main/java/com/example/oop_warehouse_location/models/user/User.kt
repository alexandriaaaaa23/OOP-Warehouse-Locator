package com.example.oop_warehouse_location.models.user

import com.example.oop_warehouse_location.models.base.BaseModel

/**
 * ABSTRACTION — abstract base for all user types.
 * Demonstrates INHERITANCE + POLYMORPHISM + ABSTRACTION.
 *
 * Cannot be instantiated directly — use [Manager] or [Employee].
 * Firestore deserialization uses [UserFactory.fromMap].
 */
abstract class User(
    open var uid: String = "",
    open var firstName: String = "",
    open var lastName: String = "",
    open var email: String = "",
    open var warehouseId: String = "",
    override var createdAt: Long = 0L
) : BaseModel(id = uid, createdAt = createdAt) {

    /** POLYMORPHISM — each user type returns its role name. */
    abstract fun getRole(): String

    /** POLYMORPHISM — each user type defines what it can do. */
    abstract fun canManageWarehouse(): Boolean
    abstract fun canRemoveMembers(): Boolean
    abstract fun canAddInventory(): Boolean

    /** Returns the user's full name, falling back to email if name is blank. */
    fun fullName(): String = "$firstName $lastName".trim().ifBlank { email }

    override fun displaySummary(): String = "${getRole()}: ${fullName()}"

    /** Returns accountType string for Firestore serialization. */
    fun getAccountType(): String = getRole()
}

