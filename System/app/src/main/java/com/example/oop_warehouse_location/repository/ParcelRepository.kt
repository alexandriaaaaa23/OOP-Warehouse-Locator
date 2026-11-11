package com.example.oop_warehouse_location.repository

import com.example.oop_warehouse_location.models.Parcel
import com.example.oop_warehouse_location.repository.base.BaseRepository
import com.google.android.gms.tasks.Task
import com.google.firebase.firestore.ListenerRegistration


interface ParcelRepository : BaseRepository<Parcel> {
    fun saveParcel(parcel: Parcel): Task<Void>
    fun getParcel(parcelId: String): Task<Parcel>
    fun stageForOutbound(parcelId: String): Task<Void>
    fun completeShipment(parcelId: String, notes: String = ""): Task<Void>
    fun returnToStock(
        parcelId: String,
        targetZone: String = "Zone A",
        targetRack: String = "Rack 01 - Top",
        targetBin: String = ""
    ): Task<Void>
    fun observeParcels(onResult: (List<Parcel>) -> Unit): ListenerRegistration
}

