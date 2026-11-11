package com.example.oop_warehouse_location.repository.firebase

import com.example.oop_warehouse_location.models.ActivityLog
import com.example.oop_warehouse_location.models.Parcel
import com.example.oop_warehouse_location.repository.ActivityLogRepository
import com.example.oop_warehouse_location.repository.ParcelRepository
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query

/**
 * Concrete implementation of [ParcelRepository] using Cloud Firestore.
 * Demonstrates POLYMORPHISM through interface implementation.
 */
class FirebaseParcelRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance(),
    var warehouseId: String = "",
    private val activityLogRepository: ActivityLogRepository = FirebaseActivityLogRepository(db, warehouseId)
) : ParcelRepository {

    private val completingShipments = mutableSetOf<String>()

    override fun save(item: Parcel): Task<Void> = saveParcel(item)

    override fun getById(id: String): Task<Parcel> = getParcel(id)

    override fun delete(id: String): Task<Void> {
        if (warehouseId.isBlank() || id.isBlank()) {
            return Tasks.forException(IllegalArgumentException("Warehouse or Parcel ID cannot be empty"))
        }
        return parcelDocument(id).delete()
    }

    override fun saveParcel(parcel: Parcel): Task<Void> {
        if (warehouseId.isBlank()) {
            return Tasks.forException(IllegalArgumentException("Warehouse ID cannot be empty"))
        }
        return db.collection(COLLECTION_WAREHOUSES)
            .document(warehouseId)
            .collection(COLLECTION_PARCELS)
            .document(parcel.parcelID)
            .set(parcel)
    }

    override fun getParcel(parcelId: String): Task<Parcel> {
        if (warehouseId.isBlank() || parcelId.isBlank()) {
            return Tasks.forException(IllegalArgumentException("Warehouse or Parcel ID cannot be empty"))
        }
        return db.collection(COLLECTION_WAREHOUSES)
            .document(warehouseId)
            .collection(COLLECTION_PARCELS)
            .document(parcelId)
            .get()
            .continueWithTask { task ->
                if (!task.isSuccessful) {
                    Tasks.forException(task.exception ?: Exception("Failed to load parcel"))
                } else {
                    val parcel = task.result?.toObject(Parcel::class.java)
                    if (parcel != null) {
                        Tasks.forResult(parcel)
                    } else {
                        Tasks.forException(Exception("Parcel not found"))
                    }
                }
            }
    }

    override fun stageForOutbound(parcelId: String): Task<Void> {
        if (warehouseId.isBlank() || parcelId.isBlank()) {
            return Tasks.forException(IllegalArgumentException("Warehouse or Parcel ID cannot be empty"))
        }
        return getParcel(parcelId).continueWithTask { task ->
            if (!task.isSuccessful) {
                Tasks.forException(task.exception ?: Exception("Failed to load parcel"))
            } else {
                val parcel = task.result
                when {
                    parcel.isShipped() -> Tasks.forException(Exception("Parcel already shipped"))
                    parcel.isOutboundStaged() -> Tasks.forResult(null)
                    !parcel.isInStock() ->
                        Tasks.forException(Exception("Parcel cannot be staged from status ${parcel.status}"))
                    else -> parcelDocument(parcelId).update(
                        mapOf(
                            "status" to Parcel.STATUS_OUTBOUND_STAGED,
                            "stagedAt" to System.currentTimeMillis(),
                            "zone" to "Staging",
                            "rack" to "",
                            "binCoordinate" to ""
                        )
                    ).continueWithTask { updateTask ->
                        if (updateTask.isSuccessful) {
                            val userId = FirebaseAuth.getInstance().currentUser?.uid.orEmpty()
                            activityLogRepository.logActivity(
                                ActivityLog(
                                    parcelId = parcelId,
                                    userId = userId,
                                    action = "Staged parcel $parcelId for outbound"
                                )
                            )
                        } else {
                            Tasks.forException(updateTask.exception ?: Exception("Failed to stage parcel"))
                        }
                    }
                }
            }
        }
    }

    override fun completeShipment(parcelId: String, notes: String): Task<Void> {
        if (warehouseId.isBlank() || parcelId.isBlank()) {
            return Tasks.forException(IllegalArgumentException("Warehouse or Parcel ID cannot be empty"))
        }
        if (!completingShipments.add(parcelId)) {
            return Tasks.forResult(null)
        }
        return getParcel(parcelId).continueWithTask { task ->
            if (!task.isSuccessful) {
                completingShipments.remove(parcelId)
                Tasks.forException(task.exception ?: Exception("Failed to load parcel"))
            } else {
                val parcel = task.result
                when {
                    parcel.isShipped() -> {
                        completingShipments.remove(parcelId)
                        Tasks.forResult(null)
                    }
                    !parcel.isOutboundStaged() && !parcel.isInStock() -> {
                        completingShipments.remove(parcelId)
                        Tasks.forException(Exception("Parcel must be in stock or outbound staged to ship"))
                    }
                    else -> {
                        val currentUser = FirebaseAuth.getInstance().currentUser
                        val userId = currentUser?.uid.orEmpty()
                        val userName = currentUser?.displayName.orEmpty()
                        val updates = mutableMapOf<String, Any>(
                            "status" to Parcel.STATUS_SHIPPED,
                            "shippedAt" to System.currentTimeMillis(),
                            "shippedByUserId" to userId,
                            "shippedByUserName" to userName,
                            "zone" to "",
                            "rack" to "",
                            "binCoordinate" to ""
                        )
                        if (notes.isNotBlank()) {
                            updates["dispatchNotes"] = notes
                        }

                        parcelDocument(parcelId).update(updates).continueWithTask { updateTask ->
                            completingShipments.remove(parcelId)
                            if (!updateTask.isSuccessful) {
                                Tasks.forException(updateTask.exception ?: Exception("Failed to ship parcel"))
                            } else {
                                val carrier = parcel.carrier.ifBlank { "carrier" }
                                activityLogRepository.logActivity(
                                    ActivityLog(
                                        parcelId = parcelId,
                                        userId = userId,
                                        action = "Shipped out parcel $parcelId via $carrier"
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    override fun returnToStock(
        parcelId: String,
        targetZone: String,
        targetRack: String,
        targetBin: String
    ): Task<Void> {
        if (warehouseId.isBlank() || parcelId.isBlank()) {
            return Tasks.forException(IllegalArgumentException("Warehouse or Parcel ID cannot be empty"))
        }
        return parcelDocument(parcelId).update(
            mapOf(
                "status" to Parcel.STATUS_IN_STOCK,
                "zone" to targetZone,
                "rack" to targetRack,
                "binCoordinate" to targetBin,
                "stagedAt" to 0L
            )
        ).continueWithTask { task ->
            if (task.isSuccessful) {
                val userId = FirebaseAuth.getInstance().currentUser?.uid.orEmpty()
                activityLogRepository.logActivity(
                    ActivityLog(
                        parcelId = parcelId,
                        userId = userId,
                        action = "Returned parcel $parcelId to stock ($targetZone)"
                    )
                )
            } else {
                Tasks.forException(task.exception ?: Exception("Failed to return parcel to stock"))
            }
        }
    }

    override fun observeParcels(onResult: (List<Parcel>) -> Unit): ListenerRegistration {
        if (warehouseId.isBlank()) {
            onResult(emptyList())
            return ListenerRegistration { }
        }
        return db.collection(COLLECTION_WAREHOUSES)
            .document(warehouseId)
            .collection(COLLECTION_PARCELS)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) {
                    onResult(emptyList())
                    return@addSnapshotListener
                }
                val parcels = snapshot.documents.mapNotNull { it.toObject(Parcel::class.java) }
                onResult(parcels)
            }
    }

    private fun parcelDocument(parcelId: String) = db.collection(COLLECTION_WAREHOUSES)
        .document(warehouseId)
        .collection(COLLECTION_PARCELS)
        .document(parcelId)

    companion object {
        const val COLLECTION_WAREHOUSES = "warehouses"
        const val COLLECTION_PARCELS = "parcels"
    }
}

