package com.connect.medium.ui.main.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.connect.medium.R
import com.connect.medium.data.model.Post
import com.connect.medium.databinding.ItemPostBinding
import androidx.viewpager2.widget.ViewPager2

class PostAdapter(
    private val onLikeClick: (Post) -> Unit,
    private val onCommentClick: (Post) -> Unit,
    private val onProfileClick: (String) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

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

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == VIEW_TYPE_HEADER) {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_feed_header, parent, false)
            HeaderViewHolder(view)
        } else {
            val binding = ItemPostBinding.inflate(
                LayoutInflater.from(parent.context), parent, false
            )
            PostViewHolder(binding)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is PostViewHolder -> {
                val post = posts[position - 1]
                holder.bind(post)
            }
            is HeaderViewHolder -> {}
        }
    }

    override fun getItemViewType(position: Int): Int {
        return if (position == 0) VIEW_TYPE_HEADER else VIEW_TYPE_POST
    }

    override fun getItemCount() = posts.size + 1

    // release all players when post is recycled
    override fun onViewRecycled(holder: RecyclerView.ViewHolder) {
        super.onViewRecycled(holder)
        if (holder is PostViewHolder) {

            holder.pageChangeCallback?.let {
                holder.binding.viewPagerMedia.unregisterOnPageChangeCallback(it)
            }
            holder.pageChangeCallback = null

            val rv = holder.binding.viewPagerMedia.getChildAt(0) as? RecyclerView
            rv?.let {
                for (i in 0 until it.childCount) {
                    val mediaHolder = it.getChildViewHolder(it.getChildAt(i))
                    if (mediaHolder is PostMediaAdapter.VideoViewHolder) {
                        mediaHolder.release()
                    }
                }
            }
            holder.binding.viewPagerMedia.adapter = null
        }
    }



    inner class PostViewHolder(
        val binding: ItemPostBinding  // changed to val so onViewRecycled can access it
    ) : RecyclerView.ViewHolder(binding.root) {

        public var pageChangeCallback: ViewPager2.OnPageChangeCallback? = null

        fun bind(post: Post) {
            binding.tvUsername.text = post.authorUsername
            binding.tvCaption.text = post.caption
            binding.tvLikeCount.text = post.likeCount.toString()
            binding.tvCommentCount.text = post.commentCount.toString()
            binding.tvTimestamp.text = getRelativeTime(post.createdAt)

            Glide.with(binding.root)
                .load(post.authorProfileImageUrl)
                .placeholder(R.drawable.ic_profile)
                .circleCrop()
                .into(binding.ivProfileImage)

            val mediaAdapter = PostMediaAdapter(post.mediaUrls, post.mediaTypes)
            binding.viewPagerMedia.adapter = mediaAdapter

            pageChangeCallback?.let {
                binding.viewPagerMedia.unregisterOnPageChangeCallback(it)
            }

            pageChangeCallback = object : ViewPager2.OnPageChangeCallback() {
                override fun onPageSelected(position: Int) {
                    super.onPageSelected(position)
                    val rv = binding.viewPagerMedia.getChildAt(0) as? RecyclerView
                    rv?.let {
                        for (i in 0 until it.childCount) {
                            val holder = it.getChildViewHolder(it.getChildAt(i))
                            if (holder is PostMediaAdapter.VideoViewHolder) {
                                holder.pause()
                            }
                        }
                    }
                }
            }
            binding.viewPagerMedia.registerOnPageChangeCallback(pageChangeCallback!!)

            // pause all videos when swiping to next media item
            binding.viewPagerMedia.registerOnPageChangeCallback(
                object : ViewPager2.OnPageChangeCallback() {
                    override fun onPageSelected(position: Int) {
                        super.onPageSelected(position)
                        val rv = binding.viewPagerMedia.getChildAt(0) as? RecyclerView
                        rv?.let {
                            for (i in 0 until it.childCount) {
                                val holder = it.getChildViewHolder(it.getChildAt(i))
                                if (holder is PostMediaAdapter.VideoViewHolder) {
                                    holder.pause()
                                }
                            }
                        }
                    }
                }
            )

            if (post.mediaUrls.size > 1) {
                binding.dotsIndicator.visibility = View.VISIBLE
                binding.dotsIndicator.attachTo(binding.viewPagerMedia)
            } else {
                binding.dotsIndicator.visibility = View.GONE
            }

            val isLiked = likedPosts.contains(post.postId)
            binding.btnLike.setIconResource(
                if (isLiked) R.drawable.ic_heart_filled else R.drawable.ic_heart_outline
            )

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

    companion object {
        private const val VIEW_TYPE_HEADER = 0
        private const val VIEW_TYPE_POST = 1
    }
}

class HeaderViewHolder(view: View) : RecyclerView.ViewHolder(view)