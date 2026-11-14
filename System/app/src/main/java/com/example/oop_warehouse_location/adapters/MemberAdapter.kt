package com.example.oop_warehouse_location.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.oop_warehouse_location.R
import com.example.oop_warehouse_location.adapters.base.BaseListAdapter
import com.example.oop_warehouse_location.models.user.User

/**
 * Adapter for displaying warehouse members (owner, managers, employees).
 * Demonstrates INHERITANCE by extending BaseListAdapter.
 */
class MemberAdapter(
    private var ownerId: String = "",
    private var currentUserId: String = "",
    private val onRemoveClicked: (User) -> Unit = {}
) : BaseListAdapter<User, MemberAdapter.MemberViewHolder>() {

    fun submitMembers(
        users: List<User>,
        ownerId: String = this.ownerId,
        currentUserId: String = this.currentUserId
    ) {
        this.ownerId = ownerId
        this.currentUserId = currentUserId
        val sortedList = users.sortedWith(
            compareByDescending<User> { it.uid == ownerId }
                .thenBy { it.firstName }
        )
        super.submitList(sortedList)
    }

    override fun submitList(newItems: List<User>) {
        val sortedList = newItems.sortedWith(
            compareByDescending<User> { it.uid == ownerId }
                .thenBy { it.firstName }
        )
        super.submitList(sortedList)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MemberViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_member, parent, false)
        return MemberViewHolder(view)
    }

    override fun onBindViewHolder(holder: MemberViewHolder, position: Int) {
        holder.bind(items[position])
    }

    inner class MemberViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvMemberName: TextView = itemView.findViewById(R.id.tvMemberName)
        private val tvMemberEmail: TextView = itemView.findViewById(R.id.tvMemberEmail)
        private val tvMemberRole: TextView = itemView.findViewById(R.id.tvMemberRole)
        private val btnRemoveMember: ImageView = itemView.findViewById(R.id.btnRemoveMember)

        fun bind(user: User) {
            val fullName = user.fullName()
            tvMemberName.text = fullName
            tvMemberEmail.text = user.email
            tvMemberRole.text = if (user.uid == ownerId) {
                itemView.context.getString(R.string.role_owner)
            } else {
                user.getRole().ifBlank { "Employee" }
            }

            val currentMember = items.find { it.uid == currentUserId }
            val canRemoveMembers = currentUserId == ownerId || currentMember?.canRemoveMembers() == true
            val isTargetOwner = user.uid == ownerId

            if (canRemoveMembers && !isTargetOwner) {
                btnRemoveMember.visibility = View.VISIBLE
                btnRemoveMember.setOnClickListener {
                    onRemoveClicked(user)
                }
            } else {
                btnRemoveMember.visibility = View.GONE
            }
        }
    }
}

