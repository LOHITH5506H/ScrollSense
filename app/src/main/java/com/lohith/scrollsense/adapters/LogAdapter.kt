package com.lohith.scrollsense.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.lohith.scrollsense.data.models.ContentSegment
import com.lohith.scrollsense.databinding.ItemLogEntryBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class LogAdapter : ListAdapter<ContentSegment, LogAdapter.LogViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LogViewHolder {
        val binding = ItemLogEntryBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return LogViewHolder(binding)
    }

    override fun onBindViewHolder(holder: LogViewHolder, position: Int) {
        val item = getItem(position)
        holder.bind(item)
    }

    class LogViewHolder(private val binding: ItemLogEntryBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(segment: ContentSegment) {
            binding.tvAppName.text = "App: ${segment.appName}"
            binding.tvScreenContent.text = "Screen: ${segment.contentTitle}"
            binding.tvCategory.text = "Category: ${segment.contentType}"
            binding.tvDuration.text = "Duration: ${segment.duration / 1000}s"
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<ContentSegment>() {
        override fun areItemsTheSame(oldItem: ContentSegment, newItem: ContentSegment) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: ContentSegment, newItem: ContentSegment) = oldItem == newItem
    }
}