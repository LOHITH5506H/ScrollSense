package com.lohith.scrollsense.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.lohith.scrollsense.MainActivity
import com.lohith.scrollsense.databinding.FragmentDetailedStatsBinding
import com.lohith.scrollsense.utils.AppCategorizer

class DetailedStatsFragment : Fragment() {

    private var _binding: FragmentDetailedStatsBinding? = null
    private val binding get() = _binding!!

    private lateinit var categoryStatsAdapter: CategoryStatsAdapter
    private var bottomNavLayoutListener: View.OnLayoutChangeListener? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDetailedStatsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val rootView = binding.root
        val originalBottomPadding = rootView.paddingBottom

        // Prevent sticky bottom navbar from covering content
        ViewCompat.setOnApplyWindowInsetsListener(rootView) { v, insets ->
            val sys = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val ime = insets.getInsets(WindowInsetsCompat.Type.ime())
            val insetBottom = maxOf(sys.bottom, ime.bottom)
            val bottomNav = (activity as? com.lohith.scrollsense.MainActivity)?.findViewById<com.google.android.material.bottomnavigation.BottomNavigationView>(com.lohith.scrollsense.R.id.bottomNavigation)
            val navH = bottomNav?.height ?: 0
            v.updatePadding(bottom = originalBottomPadding + insetBottom + navH)
            insets
        }
        val bottomNav = (activity as? com.lohith.scrollsense.MainActivity)?.findViewById<com.google.android.material.bottomnavigation.BottomNavigationView>(com.lohith.scrollsense.R.id.bottomNavigation)
        bottomNavLayoutListener = View.OnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
            if (rootView.isAttachedToWindow) {
                ViewCompat.requestApplyInsets(rootView)
            }
        }
        bottomNav?.addOnLayoutChangeListener(bottomNavLayoutListener)

        setupRecyclerView()
        observeData()
    }

    private fun setupRecyclerView() {
        categoryStatsAdapter = CategoryStatsAdapter { category ->
            // Handle category click - show apps in category
        }

        binding.categoryStatsRecyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = categoryStatsAdapter
            // Ensure last items are not hidden under sticky bottom nav
            clipToPadding = false
            val extra = (16 * resources.displayMetrics.density).toInt() * 2 // ~32dp
            updatePadding(bottom = paddingBottom + extra)
        }
    }

    private fun observeData() {
        val mainActivity = activity as? MainActivity ?: return
        val viewModel = mainActivity.getViewModel()

        viewModel.categoryData.observe(viewLifecycleOwner) { categories ->
            if (categories.isEmpty()) {
                showNoDataState()
                return@observe
            }

            hideNoDataState()

            // Update detailed stats
            val detailedStats = AppCategorizer.getDetailedCategoryStats(categories)
            categoryStatsAdapter.updateData(detailedStats.values.toList())

            // Update summary information
            updateSummaryStats(categories)
        }
    }

    private fun updateSummaryStats(categories: List<com.lohith.scrollsense.data.models.CategoryData>) {
        val totalTime = AppCategorizer.getTotalUsageTime(categories)
        val productivityScore = AppCategorizer.getProductivityScore(categories)

        binding.apply {
            totalScreenTimeText.text = AppCategorizer.formatTime(totalTime)
            categoriesCountText.text = "${categories.size} categories"
            productivityScoreText.text = "${productivityScore.toInt()}%"

            // Set productivity color
            val color = when {
                productivityScore >= 70 -> android.graphics.Color.parseColor("#4CAF50")
                productivityScore >= 40 -> android.graphics.Color.parseColor("#FF9800")
                else -> android.graphics.Color.parseColor("#F44336")
            }
            productivityScoreText.setTextColor(color)
        }
    }

    private fun showNoDataState() {
        binding.noDataLayout.visibility = View.VISIBLE
        binding.contentLayout.visibility = View.GONE
    }

    private fun hideNoDataState() {
        binding.noDataLayout.visibility = View.GONE
        binding.contentLayout.visibility = View.VISIBLE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        val bottomNav = (activity as? com.lohith.scrollsense.MainActivity)?.findViewById<com.google.android.material.bottomnavigation.BottomNavigationView>(com.lohith.scrollsense.R.id.bottomNavigation)
        bottomNavLayoutListener?.let { bottomNav?.removeOnLayoutChangeListener(it) }
        bottomNavLayoutListener = null
        _binding = null
    }

    // Placeholder adapter - you can implement this later
    class CategoryStatsAdapter(
        private val onCategoryClick: (com.lohith.scrollsense.utils.CategoryStats) -> Unit
    ) : androidx.recyclerview.widget.RecyclerView.Adapter<androidx.recyclerview.widget.RecyclerView.ViewHolder>() {

        private var stats: List<com.lohith.scrollsense.utils.CategoryStats> = emptyList()

        fun updateData(newStats: List<com.lohith.scrollsense.utils.CategoryStats>) {
            stats = newStats
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
            object : androidx.recyclerview.widget.RecyclerView.ViewHolder(
                android.widget.TextView(parent.context).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                    )
                    setPadding(16, 16, 16, 16)
                }
            ) {}

        override fun onBindViewHolder(holder: androidx.recyclerview.widget.RecyclerView.ViewHolder, position: Int) {
            val stat = stats[position]
            (holder.itemView as android.widget.TextView).text =
                "${stat.categoryName}: ${AppCategorizer.formatTime(stat.totalTime)} (${stat.percentage.format(1)}%)"
        }

        override fun getItemCount() = stats.size

        private fun Double.format(digits: Int): String = "%.${digits}f".format(this)
    }
}