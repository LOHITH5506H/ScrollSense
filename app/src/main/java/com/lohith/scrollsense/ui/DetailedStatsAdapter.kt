package com.lohith.scrollsense.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.lohith.scrollsense.R

data class DetailedStatItem(
    val title: String,
    val detail: String
)

class DetailedStatsAdapter(private val onClick: (DetailedStatItem) -> Unit = {}) : ListAdapter<DetailedStatItem, DetailedStatsAdapter.VH>(DIFF) {

    object DIFF : DiffUtil.ItemCallback<DetailedStatItem>() {
        override fun areItemsTheSame(oldItem: DetailedStatItem, newItem: DetailedStatItem): Boolean =
            oldItem.title == newItem.title
        override fun areContentsTheSame(oldItem: DetailedStatItem, newItem: DetailedStatItem): Boolean =
            oldItem == newItem
    }

    class VH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val title: TextView = itemView.findViewById(R.id.textTitle)
        private val detail: TextView = itemView.findViewById(R.id.textDetail)
        fun bind(item: DetailedStatItem, onClick: (DetailedStatItem) -> Unit) {
            title.text = item.title
            detail.text = item.detail
            itemView.setOnClickListener { onClick(item) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_detailed_stat, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(getItem(position), onClick)
    }
}
