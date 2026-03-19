package com.connect.medium.ui.main.adapters

import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.connect.medium.databinding.ItemMediaPreviewBinding

class MediaPreviewAdapter(
    private val onRemoveClick: (Int) -> Unit
) : RecyclerView.Adapter<MediaPreviewAdapter.MediaViewHolder>() {

    private var items = listOf<Pair<Uri, String>>()

    fun submitList(newItems: List<Pair<Uri, String>>) {
        items = newItems
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MediaViewHolder {
        val binding = ItemMediaPreviewBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return MediaViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MediaViewHolder, position: Int) {
        holder.bind(items[position], position)
    }

    override fun getItemCount() = items.size

    inner class MediaViewHolder(
        private val binding: ItemMediaPreviewBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: Pair<Uri, String>, position: Int) {
            val (uri, type) = item

            Glide.with(binding.root)
                .load(uri)
                .centerCrop()
                .into(binding.ivPreview)

            // show video indicator
            binding.ivVideoIndicator.visibility =
                if (type == "video") View.VISIBLE else View.GONE

            binding.btnRemove.setOnClickListener { onRemoveClick(position) }
        }
    }
}