package com.connect.medium.ui.main.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.connect.medium.R
import com.connect.medium.data.model.Post
import com.connect.medium.databinding.ItemPostBinding

class PostAdapter(
    private val onLikeClick: (Post) -> Unit,
    private val onCommentClick: (Post) -> Unit,
    private val onProfileClick: (String) -> Unit
) : RecyclerView.Adapter<PostAdapter.PostViewHolder>() {

    private var posts = listOf<Post>()
    private var likedPosts = mutableSetOf<String>()

    fun submitList(newPosts: List<Post>) {
        posts = newPosts
        notifyDataSetChanged()
    }

    fun setLikedPosts(liked: Set<String>) {
        likedPosts = liked.toMutableSet()
        notifyDataSetChanged()
    }

    fun updateLike(postId: String, isLiked: Boolean) {
        if (isLiked) likedPosts.add(postId) else likedPosts.remove(postId)
        val index = posts.indexOfFirst { it.postId == postId }
        if (index != -1) notifyItemChanged(index)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostViewHolder {
        val binding = ItemPostBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return PostViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PostViewHolder, position: Int) {
        holder.bind(posts[position])
    }

    override fun getItemCount() = posts.size

    inner class PostViewHolder(
        private val binding: ItemPostBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(post: Post) {
            // author info
            binding.tvUsername.text = post.authorUsername
            binding.tvCaption.text = post.caption
            binding.tvLikeCount.text = post.likeCount.toString()
            binding.tvCommentCount.text = post.commentCount.toString()

            // post image
            Glide.with(binding.root)
                .load(post.imageUrl)
                .centerCrop()
                .into(binding.ivPostImage)

            // author profile image
            Glide.with(binding.root)
                .load(post.authorProfileImageUrl)
                .placeholder(R.drawable.ic_profile)
                .circleCrop()
                .into(binding.ivProfileImage)

            // like button state
            val isLiked = likedPosts.contains(post.postId)
            binding.btnLike.setIconResource(
                if (isLiked) R.drawable.ic_heart_filled else R.drawable.ic_heart_outline
            )

            // timestamp
            binding.tvTimestamp.text = getRelativeTime(post.createdAt)

            // click listeners
            binding.btnLike.setOnClickListener { onLikeClick(post) }
            binding.btnComment.setOnClickListener { onCommentClick(post) }
            binding.ivProfileImage.setOnClickListener { onProfileClick(post.authorUid) }
            binding.tvUsername.setOnClickListener { onProfileClick(post.authorUid) }
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