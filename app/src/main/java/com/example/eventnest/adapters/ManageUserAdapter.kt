package com.example.eventnest.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.eventnest.R
import com.example.eventnest.models.User

class ManageUserAdapter(
    private val currentAdminUid: String,
    private val onRemoveClick: (User) -> Unit
) : ListAdapter<User, ManageUserAdapter.ManageUserViewHolder>(UserDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ManageUserViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_manage_user, parent, false)
        return ManageUserViewHolder(view)
    }

    override fun onBindViewHolder(holder: ManageUserViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ManageUserViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvInitials: TextView = itemView.findViewById(R.id.tvUserAvatarInitials)
        private val tvName: TextView = itemView.findViewById(R.id.tvUserName)
        private val tvEmail: TextView = itemView.findViewById(R.id.tvUserEmail)
        private val tvRoleBadge: TextView = itemView.findViewById(R.id.tvUserRoleBadge)
        private val btnRemove: View = itemView.findViewById(R.id.btnRemoveUser)

        fun bind(user: User) {
            val displayName = when {
                user.name.isNotBlank() -> user.name.trim()
                user.email.isNotBlank() -> user.email.substringBefore("@")
                else -> "User"
            }
            tvName.text = displayName
            tvEmail.text = user.email.ifBlank { "No email associated" }
            tvInitials.text = getInitials(displayName)

            // Role Badge (Show only for admin accounts)
            val isAdmin = user.role.equals("admin", ignoreCase = true)
            tvRoleBadge.visibility = if (isAdmin) View.VISIBLE else View.GONE

            // Self-Removal Guard: The logged-in admin cannot remove themselves
            val isCurrentAdmin = user.uid == currentAdminUid
            btnRemove.visibility = if (isCurrentAdmin) View.GONE else View.VISIBLE

            btnRemove.setOnClickListener {
                onRemoveClick(user)
            }
        }
    }

    companion object {
        fun getInitials(name: String): String {
            val trimmed = name.trim()
            if (trimmed.isBlank()) return "U"
            val parts = trimmed.split("\\s+".toRegex()).filter { it.isNotBlank() }
            return when {
                parts.size >= 2 -> "${parts[0].first()}${parts[1].first()}".uppercase()
                parts.size == 1 && parts[0].length >= 2 -> parts[0].take(2).uppercase()
                parts.size == 1 -> parts[0].take(1).uppercase()
                else -> "U"
            }
        }
    }

    private class UserDiffCallback : DiffUtil.ItemCallback<User>() {
        override fun areItemsTheSame(oldItem: User, newItem: User): Boolean {
            return oldItem.uid == newItem.uid
        }

        override fun areContentsTheSame(oldItem: User, newItem: User): Boolean {
            return oldItem == newItem
        }
    }
}
