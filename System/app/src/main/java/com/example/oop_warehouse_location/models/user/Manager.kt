package com.example.oop_warehouse_location.models.user

/**
 * INHERITANCE — Manager is a concrete [User] subclass.
 * Demonstrates POLYMORPHISM through method overrides.
 *
 * Managers have full warehouse control: they can manage the warehouse,
 * remove members, and add inventory.
 */
class Manager(
    override var uid: String = "",
    override var firstName: String = "",
    override var lastName: String = "",
    override var email: String = "",
    override var warehouseId: String = "",
    override var createdAt: Long = 0L
) : User(uid, firstName, lastName, email, warehouseId, createdAt) {

    override fun getRole(): String = "Manager"
    override fun canManageWarehouse(): Boolean = true
    override fun canRemoveMembers(): Boolean = true
    override fun canAddInventory(): Boolean = true
}

