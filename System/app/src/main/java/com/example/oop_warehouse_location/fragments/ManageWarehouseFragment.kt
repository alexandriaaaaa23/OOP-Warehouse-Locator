package com.example.oop_warehouse_location.fragments

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.oop_warehouse_location.R
import com.example.oop_warehouse_location.activities.MainActivity
import com.example.oop_warehouse_location.models.User
import com.example.oop_warehouse_location.repository.FirebaseRepository

class ManageWarehouseFragment : Fragment() {

    private lateinit var repository: FirebaseRepository
    private var warehouseId: String = ""

    companion object {
        private const val ARG_WAREHOUSE_ID = "warehouseId"

        fun newInstance(warehouseId: String): ManageWarehouseFragment {
            return ManageWarehouseFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_WAREHOUSE_ID, warehouseId)
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        warehouseId = arguments?.getString(ARG_WAREHOUSE_ID) ?: ""
        repository = FirebaseRepository(warehouseId = warehouseId)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_manage_warehouse, container, false)

        view.findViewById<ImageView>(R.id.ivMenuButton).setOnClickListener {
            (activity as? MainActivity)?.openDrawer()
        }

        loadWarehouse(view)
        return view
    }

    private fun loadWarehouse(view: View) {
        if (warehouseId.isBlank()) {
            Toast.makeText(requireContext(), "No warehouse assigned", Toast.LENGTH_SHORT).show()
            return
        }

        repository.getWarehouse(warehouseId)
            .addOnSuccessListener { warehouse ->
                if (!isAdded || this.view == null) return@addOnSuccessListener

                view.findViewById<TextView>(R.id.tvWarehouseName).text = warehouse.name
                view.findViewById<TextView>(R.id.tvInviteCode).text = warehouse.inviteCode

                view.findViewById<View>(R.id.inviteCodeCard).setOnClickListener {
                    val clipboard = requireContext()
                        .getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    clipboard.setPrimaryClip(ClipData.newPlainText("Invite Code", warehouse.inviteCode))
                    Toast.makeText(requireContext(), "Invite code copied!", Toast.LENGTH_SHORT).show()
                }

                val memberIds = linkedSetOf<String>()
                if (warehouse.ownerId.isNotBlank()) memberIds.add(warehouse.ownerId)
                memberIds.addAll(warehouse.members)

                view.findViewById<TextView>(R.id.tvMemberCount).text =
                    getString(R.string.member_count, memberIds.size)

                repository.getUsersByIds(memberIds.toList())
                    .addOnSuccessListener { users ->
                        if (isAdded && this.view != null) {
                            bindMembers(view, users, warehouse.ownerId)
                        }
                    }
                    .addOnFailureListener {
                        if (isAdded && this.view != null) {
                            bindMembers(view, emptyList(), warehouse.ownerId)
                        }
                    }
            }
            .addOnFailureListener { e ->
                if (isAdded) {
                    Toast.makeText(requireContext(), e.localizedMessage, Toast.LENGTH_LONG).show()
                }
            }
    }

    private fun bindMembers(view: View, users: List<User>, ownerId: String) {
        val container = view.findViewById<LinearLayout>(R.id.membersContainer)
        val emptyState = view.findViewById<TextView>(R.id.tvNoMembers)
        container.removeAllViews()

        if (users.isEmpty()) {
            emptyState.visibility = View.VISIBLE
            return
        }

        emptyState.visibility = View.GONE
        val inflater = LayoutInflater.from(requireContext())

        users.sortedWith(
            compareByDescending<User> { it.uid == ownerId }
                .thenBy { it.firstName }
        ).forEach { user ->
            val row = inflater.inflate(R.layout.item_member, container, false)
            val fullName = "${user.firstName} ${user.lastName}".trim().ifBlank { "User" }
            row.findViewById<TextView>(R.id.tvMemberName).text = fullName
            row.findViewById<TextView>(R.id.tvMemberEmail).text = user.email
            row.findViewById<TextView>(R.id.tvMemberRole).text =
                if (user.uid == ownerId) getString(R.string.role_owner) else user.accountType.ifBlank { "Employee" }
            container.addView(row)
        }
    }
}
