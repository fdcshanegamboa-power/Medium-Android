package com.connect.medium.ui.main.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContentProviderCompat.requireContext
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.connect.medium.R
import com.connect.medium.data.model.Notification
import com.connect.medium.data.model.NotificationType
import com.connect.medium.databinding.ItemNotificationBinding

class NotificationAdapter(
    private val onNotificationClick: (Notification) -> Unit
) : RecyclerView.Adapter<NotificationAdapter.NotificationViewHolder>() {

    private var notifications = listOf<Notification>()

    fun submitList(newNotifications: List<Notification>) {
        notifications = newNotifications
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NotificationViewHolder {
        val binding = ItemNotificationBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return NotificationViewHolder(binding)
    }

    override fun onBindViewHolder(holder: NotificationViewHolder, position: Int) {
        holder.bind(notifications[position])
    }

    override fun getItemCount() = notifications.size

    inner class NotificationViewHolder(
        private val binding: ItemNotificationBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(notification: Notification) {
            binding.tvUsername.text = notification.fromUsername
            binding.tvTimestamp.text = getRelativeTime(notification.createdAt)

            // notification message based on type
            binding.tvMessage.text = when (notification.type) {
                NotificationType.LIKE -> "liked your post"
                NotificationType.COMMENT -> "commented on your post"
                NotificationType.FOLLOW -> "started following you"
            }

            // notification icon based on type
            binding.ivNotificationIcon.setImageResource(
                when (notification.type) {
                    NotificationType.LIKE -> R.drawable.ic_heart_filled
                    NotificationType.COMMENT -> R.drawable.ic_comment
                    NotificationType.FOLLOW -> R.drawable.ic_profile
                }
            )
            binding.ivNotificationIcon.drawable?.setTint(
                ContextCompat.getColor(binding.root.context, R.color.foreground)
            )

            Glide.with(binding.root)
                .load(notification.fromProfileImageUrl)
                .placeholder(R.drawable.ic_profile)
                .circleCrop()
                .into(binding.ivProfileImage)

            binding.root.setOnClickListener { onNotificationClick(notification) }
        }

        private fun getRelativeTime(timestamp: Long): String {
            val diff = System.currentTimeMillis() - timestamp
            return when {
                diff < 60_000 -> "just now"
                diff < 3_600_000 -> "${diff / 60_000}m ago"
                diff < 86_400_000 -> "${diff / 3_600_000}h ago"
                else -> "${diff / 86_400_000}d ago"
            }
        }
    }
}