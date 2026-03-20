package com.connect.medium.ui.main.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.media3.common.util.UnstableApi
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.connect.medium.R
import com.connect.medium.data.model.Post
import com.connect.medium.databinding.ItemPostBinding
import androidx.viewpager2.widget.ViewPager2
import com.bumptech.glide.load.engine.DiskCacheStrategy


@UnstableApi
class PostAdapter(
    private val onLikeClick: (Post) -> Unit,
    private val onCommentClick: (Post) -> Unit,
    private val onProfileClick: (String) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var posts = listOf<Post>()
    private var likedPosts = mutableSetOf<String>()

    init {
        setHasStableIds(false)
    }

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

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)
        recyclerView.setItemViewCacheSize(5) // Limit cached views
        recyclerView.recycledViewPool.setMaxRecycledViews(0, 10) // Limit VIDEO holders
        recyclerView.recycledViewPool.setMaxRecycledViews(1, 20) // Limit IMAGE holders
    }



    inner class PostViewHolder(
        val binding: ItemPostBinding  // changed to val so onViewRecycled can access it
    ) : RecyclerView.ViewHolder(binding.root) {

        public var pageChangeCallback: ViewPager2.OnPageChangeCallback? = null

        fun bind(post: Post) {
            binding.tvUsername.text = post.authorUsername
            binding.tvTimestamp.text = getRelativeTime(post.createdAt)

            binding.tvUsername.setOnClickListener { onProfileClick(post.authorUid) }

            val captionText = android.text.SpannableStringBuilder()
            val boldSpan = android.text.style.StyleSpan(android.graphics.Typeface.BOLD)
            captionText.append(post.authorUsername)
            captionText.setSpan(boldSpan, 0, post.authorUsername.length, android.text.Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            if (post.caption.isNotEmpty()) {
                captionText.append("  ${post.caption}")
            }
            binding.tvCaption.text = captionText

            // like count
            binding.tvLikeCount.text = when {
                post.likeCount == 0 -> "Be the first to like this"
                post.likeCount == 1 -> "1 like"
                else -> "${post.likeCount} likes"
            }

            // comment count
            binding.tvCommentCount.text = when {
                post.commentCount == 0 -> ""
                post.commentCount == 1 -> "View 1 comment"
                else -> "View all ${post.commentCount} comments"
            }
            binding.tvCommentCount.setOnClickListener { onCommentClick(post) }

            android.util.Log.d("GlideDebug", "Loading profile image: '${post.authorProfileImageUrl}'")

            if (post.authorProfileImageUrl.isNotEmpty()) {
                Glide.with(binding.root)
                    .load(post.authorProfileImageUrl)
                    .placeholder(R.drawable.ic_profile)
                    .error(R.drawable.ic_profile)
                    .diskCacheStrategy(DiskCacheStrategy.NONE)
                    .skipMemoryCache(true)
                    .circleCrop()
                    .into(binding.ivProfileImage)
            } else {
                binding.ivProfileImage.setImageResource(R.drawable.ic_profile)
            }

            binding.viewPagerMedia.offscreenPageLimit = 1

            pageChangeCallback?.let {
                binding.viewPagerMedia.unregisterOnPageChangeCallback(it)
            }
            pageChangeCallback = null

            (binding.viewPagerMedia.adapter as? PostMediaAdapter)?.let {
                val rv = binding.viewPagerMedia.getChildAt(0) as? RecyclerView
                rv?.let { recyclerView ->
                    for (i in 0 until recyclerView.childCount) {
                        val holder = recyclerView.getChildViewHolder(recyclerView.getChildAt(i))
                        when (holder) {
                            is PostMediaAdapter.VideoViewHolder -> holder.release()
                            is PostMediaAdapter.ImageViewHolder -> holder.clear()
                        }
                    }
                }
            }

            binding.viewPagerMedia.adapter = null
            val mediaAdapter = PostMediaAdapter(post.mediaUrls, post.mediaTypes)
            binding.viewPagerMedia.adapter = mediaAdapter

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

            if (post.mediaUrls.size > 1) {
                binding.dotsIndicator.visibility = View.VISIBLE
                binding.dotsIndicator.attachTo(binding.viewPagerMedia)
            } else {
                binding.dotsIndicator.visibility = View.GONE
            }

            // like button state — use ImageButton now
            val isLiked = likedPosts.contains(post.postId)
            binding.btnLike.setImageResource(
                if (isLiked) R.drawable.ic_heart_filled else R.drawable.ic_heart_outline
            )

            binding.btnLike.setOnClickListener { onLikeClick(post) }
            binding.btnComment.setOnClickListener { onCommentClick(post) }
            binding.ivProfileImage.setOnClickListener { onProfileClick(post.authorUid) }
            binding.tvUsername.setOnClickListener { onProfileClick(post.authorUid) }
            binding.btnMore.setOnClickListener {
                // TODO: show post options menu (delete, report, etc.)
            }
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