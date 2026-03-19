package com.connect.medium.ui.main.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.connect.medium.R
import com.connect.medium.databinding.ItemPostVideoBinding
import com.connect.medium.databinding.ItemPostImageBinding

class PostMediaAdapter(
    private val mediaUrls: List<String>,
    private val mediaTypes: List<String>
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val TYPE_IMAGE = 0
        private const val TYPE_VIDEO = 1
    }

    override fun getItemViewType(position: Int): Int {
        return if (mediaTypes.getOrNull(position) == "video") TYPE_VIDEO else TYPE_IMAGE
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == TYPE_VIDEO) {
            val binding = ItemPostVideoBinding.inflate(
                LayoutInflater.from(parent.context), parent, false
            )
            VideoViewHolder(binding)
        } else {
            val binding = ItemPostImageBinding.inflate(
                LayoutInflater.from(parent.context), parent, false
            )
            ImageViewHolder(binding)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val url = mediaUrls.getOrNull(position) ?: return
        when (holder) {
            is ImageViewHolder -> holder.bind(url)
            is VideoViewHolder -> holder.bind(url)
        }
    }

    override fun getItemCount() = mediaUrls.size

    // Image ViewHolder
    inner class ImageViewHolder(
        private val binding: ItemPostImageBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(url: String) {
            Glide.with(binding.root)
                .load(url)
                .centerCrop()
                .into(binding.ivPostImage)
        }
    }

    // Video ViewHolder
    inner class VideoViewHolder(
        private val binding: ItemPostVideoBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        private var player: ExoPlayer? = null

        fun bind(url: String) {
            player?.release()
            player = ExoPlayer.Builder(binding.root.context).build().apply {
                setMediaItem(MediaItem.fromUri(url))
                prepare()
                playWhenReady = false
            }
            binding.playerView.player = player

            binding.btnPlayPause.setOnClickListener {
                if (player?.isPlaying == true) {
                    player?.pause()
                    binding.btnPlayPause.setImageResource(R.drawable.ic_play)
                } else {
                    player?.play()
                    binding.btnPlayPause.setImageResource(R.drawable.ic_pause)
                }
            }
        }

        fun release() {
            player?.release()
            player = null
        }
    }
}