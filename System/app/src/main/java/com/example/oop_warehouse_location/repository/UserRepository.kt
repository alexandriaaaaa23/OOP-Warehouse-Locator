package com.example.oop_warehouse_location.repository

import com.example.oop_warehouse_location.models.user.User
import com.example.oop_warehouse_location.repository.base.BaseRepository
import com.google.android.gms.tasks.Task


interface UserRepository : BaseRepository<User> {
    fun getUser(uid: String): Task<User>
    fun saveUser(user: User): Task<Void>
    fun updateUserWarehouseId(uid: String, warehouseId: String): Task<Void>
    fun getUsersByIds(uids: List<String>): Task<List<User>>
}

