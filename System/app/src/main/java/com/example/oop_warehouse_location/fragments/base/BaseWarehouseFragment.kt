package com.example.oop_warehouse_location.fragments.base

import android.os.Bundle
import androidx.fragment.app.Fragment
import com.example.oop_warehouse_location.repository.ActivityLogRepository
import com.example.oop_warehouse_location.repository.ParcelRepository
import com.example.oop_warehouse_location.repository.UserRepository
import com.example.oop_warehouse_location.repository.WarehouseRepository
import com.example.oop_warehouse_location.repository.firebase.FirebaseActivityLogRepository
import com.example.oop_warehouse_location.repository.firebase.FirebaseParcelRepository
import com.example.oop_warehouse_location.repository.firebase.FirebaseUserRepository
import com.example.oop_warehouse_location.repository.firebase.FirebaseWarehouseRepository
import com.google.firebase.firestore.ListenerRegistration

/**
 * ABSTRACTION + INHERITANCE — all warehouse-scoped fragments share:
 *   1. warehouseId extraction from arguments
 *   2. Repository initialization (polymorphism through interfaces)
 *   3. Firestore listener lifecycle cleanup
 */
abstract class BaseWarehouseFragment : Fragment() {

    protected lateinit var warehouseId: String
    protected lateinit var userRepository: UserRepository
    protected lateinit var warehouseRepository: WarehouseRepository
    protected lateinit var parcelRepository: ParcelRepository
    protected lateinit var activityLogRepository: ActivityLogRepository

    /** Subclasses register listeners here; base class cleans them up. */
    protected val listeners = mutableListOf<ListenerRegistration>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        warehouseId = arguments?.getString(ARG_WAREHOUSE_ID) ?: ""
        // Init repositories (using Firebase implementations — polymorphism!)
        userRepository = FirebaseUserRepository()
        warehouseRepository = FirebaseWarehouseRepository(warehouseId = warehouseId)
        parcelRepository = FirebaseParcelRepository(warehouseId = warehouseId)
        activityLogRepository = FirebaseActivityLogRepository(warehouseId = warehouseId)
    }

    override fun onDestroyView() {
        listeners.forEach { it.remove() }
        listeners.clear()
        super.onDestroyView()
    }

    companion object {
        const val ARG_WAREHOUSE_ID = "warehouseId"
    }
}
