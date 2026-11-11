package com.example.oop_warehouse_location.repository.firebase

import com.example.oop_warehouse_location.models.user.User
import com.example.oop_warehouse_location.models.user.UserFactory
import com.example.oop_warehouse_location.repository.UserRepository
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.firebase.firestore.FirebaseFirestore

/**
 * Concrete implementation of [UserRepository] using Cloud Firestore.
 * Demonstrates POLYMORPHISM through interface implementation.
 */
class FirebaseUserRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) : UserRepository {

    override fun save(item: User): Task<Void> = saveUser(item)

    override fun getById(id: String): Task<User> = getUser(id)

    override fun delete(id: String): Task<Void> {
        return db.collection(COLLECTION_USERS).document(id).delete()
    }

    override fun saveUser(user: User): Task<Void> {
        return db.collection(COLLECTION_USERS)
            .document(user.uid)
            .set(UserFactory.toMap(user))
    }

    override fun getUser(uid: String): Task<User> {
        return db.collection(COLLECTION_USERS)
            .document(uid)
            .get()
            .continueWithTask { task ->
                if (!task.isSuccessful) {
                    Tasks.forException(task.exception ?: Exception("Failed to load user"))
                } else {
                    val user = UserFactory.fromSnapshot(task.result)
                    if (user != null) {
                        Tasks.forResult(user)
                    } else {
                        Tasks.forException(Exception("User not found"))
                    }
                }
            }
    }

    override fun updateUserWarehouseId(uid: String, warehouseId: String): Task<Void> {
        return db.collection(COLLECTION_USERS)
            .document(uid)
            .update("warehouseId", warehouseId)
    }

    override fun getUsersByIds(uids: List<String>): Task<List<User>> {
        val uniqueIds = uids.filter { it.isNotBlank() }.distinct()
        if (uniqueIds.isEmpty()) {
            return Tasks.forResult(emptyList())
        }
        val reads = uniqueIds.map { uid ->
            db.collection(COLLECTION_USERS)
                .document(uid)
                .get()
                .continueWith { task ->
                    if (task.isSuccessful) {
                        UserFactory.fromSnapshot(task.result)
                    } else {
                        null
                    }
                }
        }
        return Tasks.whenAllSuccess<User?>(reads).continueWith { task ->
            task.result.filterNotNull()
        }
    }

    companion object {
        const val COLLECTION_USERS = "users"
    }
}

