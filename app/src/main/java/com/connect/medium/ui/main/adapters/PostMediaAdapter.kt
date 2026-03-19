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

    override fun onViewRecycled(holder: RecyclerView.ViewHolder) {
        super.onViewRecycled(holder)
        if (holder is VideoViewHolder) {
            holder.release()
        }
    }

    override fun onViewDetachedFromWindow(holder: RecyclerView.ViewHolder) {
        super.onViewDetachedFromWindow(holder)
        if (holder is VideoViewHolder) {
            holder.pause()
        }
    }

    class ImageViewHolder(
        private val binding: ItemPostImageBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(url: String) {
            Glide.with(binding.root)
                .load(url)
                .centerCrop()
                .into(binding.ivPostImage)
        }
    }

    class VideoViewHolder(
        private val binding: ItemPostVideoBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        private var player: ExoPlayer? = null
        private var isMuted = false

        fun bind(url: String) {
            release() // always release before binding new

            isMuted = false
            binding.btnMute.setImageResource(R.drawable.ic_volume_on)
            binding.btnPlayPause.setImageResource(R.drawable.ic_play)

            player = ExoPlayer.Builder(binding.root.context.applicationContext) // use applicationContext
                .build().apply {
                    setMediaItem(MediaItem.fromUri(url))
                    repeatMode = ExoPlayer.REPEAT_MODE_ONE
                    volume = 1f
                    prepare()
                    playWhenReady = false
                }

            binding.playerView.player = player

            binding.btnPlayPause.setOnClickListener(null)
            binding.btnMute.setOnClickListener(null)

            binding.btnPlayPause.setOnClickListener {
                if (player?.isPlaying == true) {
                    player?.pause()
                    binding.btnPlayPause.setImageResource(R.drawable.ic_play)
                } else {
                    player?.play()
                    binding.btnPlayPause.setImageResource(R.drawable.ic_pause)
                }
            }

            binding.btnMute.setOnClickListener {
                isMuted = !isMuted
                player?.volume = if (isMuted) 0f else 1f
                binding.btnMute.setImageResource(
                    if (isMuted) R.drawable.ic_volume_off else R.drawable.ic_volume_on
                )
            }
        }

        fun pause() {
            player?.pause()
            binding.btnPlayPause.setImageResource(R.drawable.ic_play)
        }

        fun release() {

            binding.playerView.player = null
            player?.stop()
            player?.clearMediaItems()
            player?.release()
            player = null
        }
    }
}