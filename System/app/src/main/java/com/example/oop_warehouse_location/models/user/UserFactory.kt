package com.example.oop_warehouse_location.models.user

import com.google.firebase.firestore.DocumentSnapshot

/**
 * Factory for creating the correct [User] subclass based on the accountType field.
 *
 * Firestore's toObject() requires concrete classes with no-arg constructors.
 * Since [User] is abstract, we need this factory to read the accountType field
 * and instantiate the correct subclass ([Manager] or [Employee]).
 */
object UserFactory {

    /**
     * Creates a [User] subclass from a Firestore document snapshot.
     * Reads the "accountType" field to determine whether to create a [Manager] or [Employee].
     *
     * @param snapshot The Firestore document snapshot.
     * @return The correct [User] subclass, or null if the snapshot is invalid.
     */
    fun fromSnapshot(snapshot: DocumentSnapshot?): User? {
        if (snapshot == null || !snapshot.exists()) return null

        val data = snapshot.data ?: return null
        return fromMap(data)
    }

    /**
     * Creates a [User] subclass from a map of field values.
     * Reads the "accountType" field to determine whether to create a [Manager] or [Employee].
     *
     * @param map The field values from Firestore.
     * @return The correct [User] subclass.
     */
    fun fromMap(map: Map<String, Any?>): User {
        val accountType = map["accountType"] as? String ?: "Employee"
        val uid = map["uid"] as? String ?: ""
        val firstName = map["firstName"] as? String ?: ""
        val lastName = map["lastName"] as? String ?: ""
        val email = map["email"] as? String ?: ""
        val warehouseId = map["warehouseId"] as? String ?: ""
        val createdAt = (map["createdAt"] as? Number)?.toLong() ?: 0L

        return when (accountType) {
            "Manager" -> Manager(
                uid = uid,
                firstName = firstName,
                lastName = lastName,
                email = email,
                warehouseId = warehouseId,
                createdAt = createdAt
            )
            else -> Employee(
                uid = uid,
                firstName = firstName,
                lastName = lastName,
                email = email,
                warehouseId = warehouseId,
                createdAt = createdAt
            )
        }
    }

    /**
     * Convenience method to create a [User] subclass by role string.
     * Used during sign-up when the user selects their account type.
     *
     * @param accountType "Manager" or "Employee"
     * @param uid The Firebase Auth UID
     * @param firstName User's first name
     * @param lastName User's last name
     * @param email User's email
     * @return The correct [User] subclass
     */
    fun create(
        accountType: String,
        uid: String = "",
        firstName: String = "",
        lastName: String = "",
        email: String = "",
        warehouseId: String = "",
        createdAt: Long = System.currentTimeMillis()
    ): User {
        return when (accountType) {
            "Manager" -> Manager(uid, firstName, lastName, email, warehouseId, createdAt)
            else -> Employee(uid, firstName, lastName, email, warehouseId, createdAt)
        }
    }

    /**
     * Converts a [User] to a map suitable for Firestore serialization.
     * Includes the accountType field for proper deserialization later.
     */
    fun toMap(user: User): Map<String, Any?> {
        return mapOf(
            "uid" to user.uid,
            "firstName" to user.firstName,
            "lastName" to user.lastName,
            "email" to user.email,
            "accountType" to user.getAccountType(),
            "warehouseId" to user.warehouseId,
            "createdAt" to user.createdAt
        )
    }
}

