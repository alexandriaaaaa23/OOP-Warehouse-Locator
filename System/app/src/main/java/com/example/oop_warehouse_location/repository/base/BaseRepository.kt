package com.example.oop_warehouse_location.repository.base

import com.google.android.gms.tasks.Task

interface BaseRepository<T> {
    fun save(item: T): Task<Void>
    fun getById(id: String): Task<T>
    fun delete(id: String): Task<Void>
}

