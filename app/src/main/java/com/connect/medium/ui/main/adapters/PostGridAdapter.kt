package com.connect.medium.ui.main.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.connect.medium.R
import com.connect.medium.data.model.Post
import com.connect.medium.databinding.ItemPostGridBinding

class PostGridAdapter(
    private val onPostClick: (Post) -> Unit
) : RecyclerView.Adapter<PostGridAdapter.PostViewHolder>() {

    private var posts = listOf<Post>()

    fun submitList(newPosts: List<Post>) {
        posts = newPosts
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostViewHolder {
        val binding = ItemPostGridBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return PostViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PostViewHolder, position: Int) {
        holder.bind(posts[position])
    }

    override fun getItemCount() = posts.size

    inner class PostViewHolder(
        private val binding: ItemPostGridBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(post: Post) {
            val firstMedia = post.mediaUrls.firstOrNull()
            val firstType = post.mediaTypes.firstOrNull()

            if (firstType == "video") {
                // for video show thumbnail using Glide
                Glide.with(binding.root)
                    .load(firstMedia)
                    .centerCrop()
                    .placeholder(R.drawable.ic_play)
                    .into(binding.ivPostImage)

                // show video indicator
                binding.ivVideoIndicator.visibility = View.VISIBLE
            } else {
                Glide.with(binding.root)
                    .load(firstMedia)
                    .centerCrop()
                    .into(binding.ivPostImage)

                binding.ivVideoIndicator.visibility = View.GONE
            }

            // show multiple media indicator
            binding.ivMultipleIndicator.visibility =
                if (post.mediaUrls.size > 1) View.VISIBLE else View.GONE

            binding.root.setOnClickListener { onPostClick(post) }
        }
    }
}