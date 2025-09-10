package com.lohith.scrollsense.adapters

import android.content.res.ColorStateList
import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.lohith.scrollsense.R
import com.lohith.scrollsense.data.models.AppUsageData
import com.lohith.scrollsense.databinding.ItemAppUsageBinding
import java.util.concurrent.TimeUnit
import java.util.Locale

class AppUsageAdapter(
    private val onAppClick: (AppUsageData) -> Unit
) : RecyclerView.Adapter<AppUsageAdapter.AppUsageViewHolder>() {

    private var apps: List<AppUsageData> = emptyList()

    fun updateData(newApps: List<AppUsageData>) {
        val diffCallback = AppUsageDiffCallback(apps, newApps)
        val diffResult = DiffUtil.calculateDiff(diffCallback)
        apps = newApps
        diffResult.dispatchUpdatesTo(this)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AppUsageViewHolder {
        val binding = ItemAppUsageBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        // Pass a function to get the total time for percentage calculation
        return AppUsageViewHolder(binding, onAppClick) { apps.sumOf { it.totalTimeInForeground } }
    }

    override fun onBindViewHolder(holder: AppUsageViewHolder, position: Int) {
        holder.bind(apps[position], position + 1)
    }

    override fun getItemCount(): Int = apps.size

    class AppUsageViewHolder(
        private val binding: ItemAppUsageBinding,
        private val onAppClick: (AppUsageData) -> Unit,
        private val getTotalUsageTime: () -> Long
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(app: AppUsageData, rank: Int) {
            val totalUsage = getTotalUsageTime()

            binding.apply {
                appNameText.text = app.appName
                categoryText.text = app.category // Use the category from data
                usageTimeText.text = formatDuration(app.totalTimeInForeground)
                rankText.text = "#$rank"

                val percentage = if (totalUsage > 0) {
                    (app.totalTimeInForeground.toDouble() / totalUsage) * 100
                } else 0.0
                usagePercentageText.text = String.format(Locale.getDefault(), "%.1f%%", percentage)
                usageProgressBar.progress = percentage.toInt()

                val rankColor = when (rank) {
                    1 -> ContextCompat.getColor(itemView.context, R.color.primary)
                    2 -> ContextCompat.getColor(itemView.context, R.color.secondary)
                    3 -> ContextCompat.getColor(itemView.context, R.color.tertiary)
                    else -> Color.LTGRAY
                }

                // Set colors for the UI elements
                usageProgressBar.progressTintList = ColorStateList.valueOf(rankColor)

                root.setOnClickListener { onAppClick(app) }
            }
        }

        private fun formatDuration(milliseconds: Long): String {
            val hours = TimeUnit.MILLISECONDS.toHours(milliseconds)
            val minutes = TimeUnit.MILLISECONDS.toMinutes(milliseconds) % 60
            return String.format(Locale.getDefault(), "%dh %dm", hours, minutes)
        }
    }

    // DiffUtil helps with efficient list updates
    class AppUsageDiffCallback(
        private val oldList: List<AppUsageData>,
        private val newList: List<AppUsageData>
    ) : DiffUtil.Callback() {
        override fun getOldListSize(): Int = oldList.size
        override fun getNewListSize(): Int = newList.size
        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return oldList[oldItemPosition].packageName == newList[newItemPosition].packageName
        }
        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return oldList[oldItemPosition] == newList[newItemPosition]
        }
    }
}