package com.lohith.scrollsense

import android.Manifest
import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.android.material.snackbar.Snackbar
import com.lohith.scrollsense.data.AppDatabase
import com.lohith.scrollsense.data.CategoryDuration
import com.lohith.scrollsense.data.UsageEvent
import com.lohith.scrollsense.data.models.AppUsageData
import com.lohith.scrollsense.data.models.CategoryData
import com.lohith.scrollsense.databinding.ActivityMainBinding
import com.lohith.scrollsense.fragments.DashboardFragment
import com.lohith.scrollsense.fragments.DetailedStatsFragment
import com.lohith.scrollsense.fragments.SettingsFragment
import com.lohith.scrollsense.services.GeminiAIService
import com.lohith.scrollsense.services.UsageStatsService
import com.lohith.scrollsense.utils.AppCategorizer
import com.lohith.scrollsense.viewmodels.MainViewModel
import kotlinx.coroutines.launch
import java.util.Calendar
import kotlin.math.max
import kotlin.math.min

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val mainViewModel: MainViewModel by viewModels()

    private lateinit var usageStatsService: UsageStatsService
    private lateinit var geminiAIService: GeminiAIService
    private lateinit var database: AppDatabase

    private val dashboardFragment = DashboardFragment()
    private val detailedStatsFragment = DetailedStatsFragment()
    private val settingsFragment = SettingsFragment()

    // Permission launcher for usage stats
    private val usageStatsPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (hasUsageStatsPermission()) {
            showSnackbar("Permission granted! Loading data...")
            refreshData()
        } else {
            showSnackbar("Permission denied. App functionality will be limited.")
        }
    }

    // Notification permission launcher (Android 13+)
    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            Log.d(TAG, "Notification permission granted")
        }
    }

    companion object {
        private const val TAG = "MainActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Apply bottom insets so content isn't hidden behind the sticky bottom nav.
        // Uses max(system bars, IME) + bottom nav height, and reapplies when nav lays out.
        ViewCompat.setOnApplyWindowInsetsListener(binding.fragmentContainer) { v, insets ->
            val sys = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val ime = insets.getInsets(WindowInsetsCompat.Type.ime())
            val insetBottom = maxOf(sys.bottom, ime.bottom)
            val navH = binding.bottomNavigation.height
            v.updatePadding(bottom = insetBottom + navH)
            insets
        }
        // Re-apply insets after the bottom nav has measured, so height is included.
        binding.bottomNavigation.addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
            ViewCompat.requestApplyInsets(binding.fragmentContainer)
        }

        initializeServices()
        setupUI()
        setupObservers()
        checkPermissions()
    }

    private fun initializeServices() {
        usageStatsService = UsageStatsService(this)
        geminiAIService = GeminiAIService(this)
        database = AppDatabase.getDatabase(this)
    }

    private fun setupUI() {
        setupToolbar()
        setupBottomNavigation()
        setupFab()

        // Load initial fragment
        replaceFragment(dashboardFragment)
        binding.bottomNavigation.selectedItemId = R.id.nav_dashboard
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.apply {
            title = "ScrollSense"
            subtitle = "Digital Wellness Dashboard"
        }
    }

    private fun setupBottomNavigation() {
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_dashboard -> {
                    replaceFragment(dashboardFragment)
                    true
                }
                R.id.nav_detailed_stats -> {
                    replaceFragment(detailedStatsFragment)
                    true
                }
                R.id.nav_settings -> {
                    replaceFragment(settingsFragment)
                    true
                }
                else -> false
            }
        }
    }

    private fun setupFab() {
        binding.fabRefresh.setOnClickListener {
            if (hasUsageStatsPermission()) {
                refreshData()
            } else {
                requestUsageStatsPermission()
            }
        }
    }

    private fun setupObservers() {
        // Observe loading state
        mainViewModel.isLoading.observe(this) { isLoading ->
            binding.fabRefresh.isEnabled = !isLoading

            if (isLoading) {
                binding.fabRefresh.animate()
                    .rotationBy(360f)
                    .setDuration(1000)
                    .start()
            }
        }

        // Observe app usage data
        mainViewModel.appUsageData.observe(this) { appUsageData ->
            updateSummaryCard(appUsageData)
        }

        // Observe category data
        mainViewModel.categoryData.observe(this) { categoryData ->
            // Categories updated, fragments will observe this data
        }

        // Observe error messages
        mainViewModel.errorMessage.observe(this) { error ->
            error?.let {
                showSnackbar(it)
                mainViewModel.clearError()
            }
        }
    }

    private fun checkPermissions() {
        if (!hasUsageStatsPermission()) {
            showUsageStatsPermissionDialog()
        } else {
            refreshData()
        }

        // Check notification permission for Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    private fun hasUsageStatsPermission(): Boolean {
        val appOpsManager = getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = appOpsManager.checkOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            android.os.Process.myUid(),
            packageName
        )
        return mode == AppOpsManager.MODE_ALLOWED
    }

    private fun showUsageStatsPermissionDialog() {
        AlertDialog.Builder(this)
            .setTitle("Usage Access Required")
            .setMessage(
                "ScrollSense needs access to usage statistics to track your app usage. " +
                        "This data stays on your device and is not shared with anyone."
            )
            .setPositiveButton("Grant Permission") { _, _ ->
                requestUsageStatsPermission()
            }
            .setNegativeButton("Cancel") { _, _ ->
                showSnackbar("Permission required for app to function")
                finish()
            }
            .setCancelable(false)
            .show()
    }

    private fun requestUsageStatsPermission() {
        val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
        usageStatsPermissionLauncher.launch(intent)
    }

    private fun refreshData() {
        if (!hasUsageStatsPermission()) {
            showSnackbar("Usage access permission required")
            return
        }

        lifecycleScope.launch {
            try {
                mainViewModel.setLoading(true)

                // Get usage statistics for app-level list
                val appUsageList = usageStatsService.getTodayUsageStats()

                if (appUsageList.isEmpty()) {
                    showSnackbar("No usage data found for today")
                    mainViewModel.setLoading(false)
                    return@launch
                }

                // Categorize apps by package/name for the top-apps list (kept as-is)
                val categorizedApps = geminiAIService.categorizeApps(appUsageList)

                // First publish the apps so UI can show summary fallback immediately
                mainViewModel.updateAppUsageData(categorizedApps)

                // Build content-aware categories using captured screen titles across ALL apps
                val contentCategories = buildContentAwareCategories(days = 1, fallbackApps = categorizedApps)

                // Update ViewModel with content-aware categories
                mainViewModel.updateCategoryData(contentCategories)

                showSnackbar("Data refreshed successfully!")

            } catch (e: Exception) {
                Log.e(TAG, "Error refreshing data", e)
                mainViewModel.setError("Error loading data: ${e.message}")
            } finally {
                mainViewModel.setLoading(false)
            }
        }
    }

    private suspend fun buildContentAwareCategories(days: Int, fallbackApps: List<AppUsageData>): List<CategoryData> {
        val (startTime, endTime) = getDayBounds(daysAgo = 0)

        // Load events overlapping today and clamp durations to [start, end)
        val events: List<UsageEvent> = try {
            database.usageEventDao().getEventsOverlapping(startTime, endTime)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load usage events", e)
            emptyList()
        }

        // If we don't have any content-aware events yet, fall back to app-based categories
        if (events.isEmpty()) {
            return AppCategorizer.categorizeApps(fallbackApps)
        }

        val byCategory = mutableMapOf<String, Long>()
        events.forEach { ev ->
            val s = max(ev.startTime, startTime)
            val e = min(ev.endTime, endTime)
            val dur = (e - s).coerceAtLeast(0)
            if (dur > 0) {
                val displayName = if (ev.category.equals("games", ignoreCase = true) && ev.subcategory.isNotBlank()) {
                    "Games: ${ev.subcategory}"
                } else {
                    mapClassifierCategoryToDisplay(ev.category)
                }
                byCategory[displayName] = (byCategory[displayName] ?: 0L) + dur
            }
        }

        val categoryData = byCategory.map { (displayName, total) ->
            CategoryData(
                categoryName = displayName,
                totalTime = total,
                apps = mutableListOf(),
                color = CategoryData.getDefaultColorForCategory(displayName)
            )
        }
        .filter { it.hasSignificantUsage() }
        .sortedByDescending { it.totalTime }

        // If content-derived categories are too small (all filtered out), use app-based categories
        return if (categoryData.isEmpty()) AppCategorizer.categorizeApps(fallbackApps) else categoryData
    }

    private fun mapClassifierCategoryToDisplay(raw: String): String {
        return when (raw.lowercase()) {
            "education" -> "Education"
            "entertainment" -> "Entertainment"
            "comedy" -> "Comedy"
            "fashion" -> "Fashion"
            "technology" -> "Technology"
            "music" -> "Music"
            "sports" -> "Sports"
            "news" -> "News & Reading"
            "food" -> "Food & Drink"
            "business" -> "Business"
            "social" -> "Social Media"
            "adult" -> "Adult"
            "photography" -> "Photography"
            "navigation" -> "Navigation"
            "productivity" -> "Productivity"
            "finance" -> "Finance"
            "games" -> "Games"
            else -> "Other"
        }
    }

    private fun getDayBounds(daysAgo: Int = 0): Pair<Long, Long> {
        val cal = Calendar.getInstance()
        // Set to start of local day
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        cal.add(Calendar.DAY_OF_YEAR, -daysAgo)
        val start = cal.timeInMillis
        cal.add(Calendar.DAY_OF_YEAR, 1)
        val end = cal.timeInMillis
        return start to end
    }

    private fun updateSummaryCard(appUsageData: List<AppUsageData>) {
        // Prefer content-aware total from current categories for consistency
        val categories = mainViewModel.categoryData.value ?: emptyList()
        val totalScreenTime = categories.sumOf { it.totalTime }

        if (categories.isEmpty()) {
            // Fallback to app-level list if content categories not loaded yet
            val fallback = appUsageData.sumOf { it.totalTimeInForeground }
            binding.totalScreenTimeText.text = AppCategorizer.formatTime(fallback)
        } else {
            binding.totalScreenTimeText.text = AppCategorizer.formatTime(totalScreenTime)
        }

        val mostUsedApp = appUsageData.maxByOrNull { it.totalTimeInForeground }
        binding.mostUsedAppText.text = mostUsedApp?.let {
            "${it.appName} (${it.getFormattedTime()})"
        } ?: "No data"

        val productivityScore = AppCategorizer.getProductivityScore(categories)
        binding.productivityScoreText.text = "${productivityScore.toInt()}%"

        val color = when {
            productivityScore >= 70 -> ContextCompat.getColor(this, R.color.productivity_high)
            productivityScore >= 40 -> ContextCompat.getColor(this, R.color.productivity_medium)
            else -> ContextCompat.getColor(this, R.color.productivity_low)
        }
        binding.productivityScoreText.setTextColor(color)
    }

    private fun replaceFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .commit()
    }

    private fun showSnackbar(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_SHORT).show()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_clear_logs -> {
                showClearLogsDialog()
                true
            }
            R.id.action_export_data -> {
                exportData()
                true
            }
            R.id.action_about -> {
                showAboutDialog()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun showClearLogsDialog() {
        AlertDialog.Builder(this)
            .setTitle("Clear Usage Logs")
            .setMessage("Are you sure you want to clear all usage logs? This action cannot be undone.")
            .setPositiveButton("Clear") { _, _ ->
                clearLogs()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun clearLogs() {
        lifecycleScope.launch {
            try {
                // Clear cache and data
                geminiAIService.clearCache()

                // Reset ViewModel data
                mainViewModel.clearData()

                // Reset summary card
                binding.totalScreenTimeText.text = "0h 0m"
                binding.mostUsedAppText.text = "No data"
                binding.productivityScoreText.text = "0%"

                showSnackbar("Logs cleared successfully")

                // Refresh data
                refreshData()

            } catch (e: Exception) {
                Log.e(TAG, "Error clearing logs", e)
                showSnackbar("Error clearing logs: ${e.message}")
            }
        }
    }

    private fun exportData() {
        lifecycleScope.launch {
            try {
                // TODO: Implement data export functionality
                showSnackbar("Export feature coming soon!")
            } catch (e: Exception) {
                showSnackbar("Export failed: ${e.message}")
            }
        }
    }

    private fun showAboutDialog() {
        AlertDialog.Builder(this)
            .setTitle("About ScrollSense")
            .setMessage(
                "ScrollSense v1.0\n\n" +
                        "A digital wellness app that helps you track and understand your mobile usage patterns using AI-powered categorization.\n\n" +
                        "Features:\n" +
                        "• Accurate usage tracking\n" +
                        "• AI-powered app categorization\n" +
                        "• Detailed analytics\n" +
                        "• Productivity insights\n" +
                        "• Clean Material Design UI\n\n" +
                        "Developed with ❤️ for digital wellness."
            )
            .setPositiveButton("OK", null)
            .show()
    }

    // Getters for fragments to access services and data
    fun getUsageStatsService() = usageStatsService
    fun getGeminiAIService() = geminiAIService
    fun getViewModel() = mainViewModel
}