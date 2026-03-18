package com.connect.medium.ui.main.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
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
            Glide.with(binding.root)
                .load(post.imageUrl)
                .centerCrop()
                .into(binding.ivPostImage)

            binding.root.setOnClickListener { onPostClick(post) }
        }
    }
}