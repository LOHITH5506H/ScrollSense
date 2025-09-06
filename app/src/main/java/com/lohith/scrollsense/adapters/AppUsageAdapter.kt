package com.lohith.scrollsense.adapters

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.lohith.scrollsense.R
import com.lohith.scrollsense.data.models.AppUsageData
import com.lohith.scrollsense.databinding.ItemAppUsageBinding

class AppUsageAdapter(
    private val onAppClick: (AppUsageData) -> Unit = {}
) : RecyclerView.Adapter<AppUsageAdapter.AppUsageViewHolder>() {

    private var apps: List<AppUsageData> = emptyList()
    private var totalUsageTime: Long = 0L

    fun updateData(newApps: List<AppUsageData>) {
        val oldApps = apps
        apps = newApps
        totalUsageTime = newApps.sumOf { it.totalTimeInForeground }

        val diffCallback = AppUsageDiffCallback(oldApps, newApps)
        val diffResult = DiffUtil.calculateDiff(diffCallback)
        diffResult.dispatchUpdatesTo(this)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AppUsageViewHolder {
        val binding = ItemAppUsageBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return AppUsageViewHolder(binding, onAppClick)
    }

    override fun onBindViewHolder(holder: AppUsageViewHolder, position: Int) {
        holder.bind(apps[position], totalUsageTime, position + 1)
    }

    override fun getItemCount(): Int = apps.size

    class AppUsageViewHolder(
        private val binding: ItemAppUsageBinding,
        private val onAppClick: (AppUsageData) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(app: AppUsageData, totalUsageTime: Long, rank: Int) {
            binding.apply {
                // Set app details
                appNameText.text = app.appName
                categoryText.text = app.category
                usageTimeText.text = app.getFormattedTime()
                launchCountText.text = "${app.launchCount} launches"
                rankText.text = "#$rank"

                // Calculate and show percentage
                val percentage = app.getUsagePercentage(totalUsageTime)
                usagePercentageText.text = "${percentage.format(1)}%"

                // Set progress bar
                usageProgressBar.progress = percentage.toInt()

                // Set category color
                val categoryColor = try {
                    Color.parseColor(getCategoryColor(app.category))
                } catch (e: IllegalArgumentException) {
                    Color.parseColor("#9E9E9E") // Default grey
                }

                categoryIndicator.setBackgroundColor(categoryColor)
                usageProgressBar.progressTintList =
                    android.content.res.ColorStateList.valueOf(categoryColor)

                // Set rank badge color based on position
                val rankColor = when (rank) {
                    1 -> Color.parseColor("#FFD700") // Gold
                    2 -> Color.parseColor("#C0C0C0") // Silver
                    3 -> Color.parseColor("#CD7F32") // Bronze
                    else -> Color.parseColor("#E0E0E0") // Default grey
                }
                rankBadge.setCardBackgroundColor(rankColor)

                // Click listener
                root.setOnClickListener {
                    onAppClick(app)
                }

                // Long click for additional options (future enhancement)
                root.setOnLongClickListener {
                    // TODO: Show context menu with options like "Set app limit", "Block app", etc.
                    true
                }
            }
        }

        private fun getCategoryColor(category: String): String {
            return when (category.lowercase()) {
                "social", "social media" -> "#E91E63"
                "entertainment", "video" -> "#F44336"
                "games", "gaming" -> "#9C27B0"
                "productivity", "work" -> "#2196F3"
                "education", "learning" -> "#4CAF50"
                "shopping" -> "#FF9800"
                "news", "reading", "news & reading" -> "#795548"
                "health", "fitness", "health & fitness" -> "#009688"
                "music", "audio" -> "#607D8B"
                "photography", "photo" -> "#FFC107"
                "travel", "navigation" -> "#3F51B5"
                "communication", "messaging" -> "#00BCD4"
                "finance" -> "#8BC34A"
                "food", "food & drink" -> "#FF5722"
                "lifestyle" -> "#E91E63"
                "business" -> "#37474F"
                "sports" -> "#FF9800"
                "weather" -> "#03A9F4"
                "utilities" -> "#9E9E9E"
                else -> "#9E9E9E"
            }
        }

        private fun Double.format(digits: Int): String = "%.${digits}f".format(this)
    }

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
            val oldItem = oldList[oldItemPosition]
            val newItem = newList[newItemPosition]

            return oldItem.appName == newItem.appName &&
                    oldItem.category == newItem.category &&
                    oldItem.totalTimeInForeground == newItem.totalTimeInForeground &&
                    oldItem.launchCount == newItem.launchCount
        }
    }
}