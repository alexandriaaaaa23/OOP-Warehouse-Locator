package com.example.oop_warehouse_location.models.user

/**
 * INHERITANCE — Employee is a concrete [User] subclass.
 * Demonstrates POLYMORPHISM through method overrides.
 *
 * Employees can add inventory but cannot manage the warehouse or remove members.
 */
class Employee(
    override var uid: String = "",
    override var firstName: String = "",
    override var lastName: String = "",
    override var email: String = "",
    override var warehouseId: String = "",
    override var createdAt: Long = 0L
) : User(uid, firstName, lastName, email, warehouseId, createdAt) {

    override fun getRole(): String = "Employee"
    override fun canManageWarehouse(): Boolean = false
    override fun canRemoveMembers(): Boolean = false
    override fun canAddInventory(): Boolean = true
}

