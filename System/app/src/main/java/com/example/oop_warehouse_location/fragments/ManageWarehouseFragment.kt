package com.example.oop_warehouse_location.fragments

import android.app.AlertDialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.oop_warehouse_location.R
import com.example.oop_warehouse_location.activities.MainActivity
import com.example.oop_warehouse_location.adapters.MemberAdapter
import com.example.oop_warehouse_location.fragments.base.BaseWarehouseFragment
import com.example.oop_warehouse_location.models.user.User
import com.google.firebase.auth.FirebaseAuth

class ManageWarehouseFragment : BaseWarehouseFragment() {

    private lateinit var memberAdapter: MemberAdapter
    private var currentOwnerId: String = ""

    companion object {
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
        memberAdapter = MemberAdapter(
            currentUserId = FirebaseAuth.getInstance().currentUser?.uid.orEmpty(),
            onRemoveClicked = { user ->
                showRemoveConfirmationDialog(user, user.fullName(), currentOwnerId)
            }
        )
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

        val rvMembers = view.findViewById<RecyclerView>(R.id.rvMembers)
        rvMembers.layoutManager = LinearLayoutManager(requireContext())
        rvMembers.adapter = memberAdapter

        loadWarehouse(view)
        return view
    }

    private fun loadWarehouse(view: View) {
        if (warehouseId.isBlank()) {
            Toast.makeText(requireContext(), "No warehouse assigned", Toast.LENGTH_SHORT).show()
            return
        }

        warehouseRepository.getWarehouse(warehouseId)
            .addOnSuccessListener { warehouse ->
                if (!isAdded || this.view == null) return@addOnSuccessListener

                currentOwnerId = warehouse.ownerId
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

                userRepository.getUsersByIds(memberIds.toList())
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
        val rvMembers = view.findViewById<RecyclerView>(R.id.rvMembers)
        val emptyState = view.findViewById<TextView>(R.id.tvNoMembers)

        if (users.isEmpty()) {
            emptyState.visibility = View.VISIBLE
            rvMembers.visibility = View.GONE
            memberAdapter.submitList(emptyList())
            return
        }

        emptyState.visibility = View.GONE
        rvMembers.visibility = View.VISIBLE
        memberAdapter.submitMembers(users, ownerId = ownerId)
    }

    private fun showRemoveConfirmationDialog(user: User, fullName: String, ownerId: String) {
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.remove_employee)
            .setMessage(getString(R.string.confirm_remove_employee, fullName))
            .setPositiveButton(R.string.remove) { _, _ ->
                val managerUid = FirebaseAuth.getInstance().currentUser?.uid ?: ownerId
                warehouseRepository.removeMemberFromWarehouse(
                    warehouseId = warehouseId,
                    memberUid = user.uid,
                    memberName = fullName,
                    managerUid = managerUid
                ).addOnSuccessListener {
                    if (isAdded) {
                        Toast.makeText(requireContext(), R.string.employee_removed_success, Toast.LENGTH_SHORT).show()
                        view?.let { loadWarehouse(it) }
                    }
                }.addOnFailureListener { e ->
                    if (isAdded) {
                        Toast.makeText(
                            requireContext(),
                            "${getString(R.string.failed_to_remove_employee)}: ${e.localizedMessage}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }
}
