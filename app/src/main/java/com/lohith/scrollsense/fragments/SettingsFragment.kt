package com.lohith.scrollsense.fragments

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.lohith.scrollsense.MainActivity
import com.lohith.scrollsense.R
import com.lohith.scrollsense.databinding.FragmentSettingsBinding
import kotlinx.coroutines.launch

class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupClickListeners()
        updateCacheInfo()
    }

    private fun setupClickListeners() {
        binding.apply {
            // Permission Settings
            permissionSettingsCard.setOnClickListener {
                openUsageAccessSettings()
            }

            // Clear Cache
            clearCacheCard.setOnClickListener {
                showClearCacheDialog()
            }

            // Clear All Data
            clearDataCard.setOnClickListener {
                showClearDataDialog()
            }

            // Export Data
            exportDataCard.setOnClickListener {
                exportData()
            }

            // Privacy Policy
            privacyPolicyCard.setOnClickListener {
                showPrivacyPolicy()
            }

            // About App
            aboutAppCard.setOnClickListener {
                showAboutDialog()
            }

            // Rate App
            rateAppCard.setOnClickListener {
                rateApp()
            }
        }
    }

    private fun updateCacheInfo() {
        lifecycleScope.launch {
            val mainActivity = activity as? MainActivity ?: return@launch
            val geminiService = mainActivity.getGeminiAIService()
            val cacheSize = geminiService.getCacheSize()

            binding.cacheInfoText.text = "$cacheSize apps categorized"
        }
    }

    private fun openUsageAccessSettings() {
        val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
        try {
            startActivity(intent)
        } catch (e: Exception) {
            showSnackbar("Unable to open settings")
        }
    }

    private fun showClearCacheDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Clear Cache")
            .setMessage("Clear cached app categorization data? This will re-categorize apps on next refresh.")
            .setPositiveButton("Clear") { _, _ ->
                clearCache()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun clearCache() {
        lifecycleScope.launch {
            try {
                val mainActivity = activity as? MainActivity ?: return@launch
                val geminiService = mainActivity.getGeminiAIService()

                geminiService.clearCache()
                updateCacheInfo()

                showSnackbar("Cache cleared successfully")
            } catch (e: Exception) {
                showSnackbar("Failed to clear cache: ${e.message}")
            }
        }
    }

    private fun showClearDataDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Clear All Data")
            .setMessage("This will permanently delete all usage data and reset the app. This action cannot be undone.")
            .setPositiveButton("Clear All") { _, _ ->
                clearAllData()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun clearAllData() {
        lifecycleScope.launch {
            try {
                val mainActivity = activity as? MainActivity ?: return@launch
                val geminiService = mainActivity.getGeminiAIService()
                val viewModel = mainActivity.getViewModel()

                // Clear cache
                geminiService.clearCache()

                // Clear view model data
                viewModel.clearData()

                updateCacheInfo()
                showSnackbar("All data cleared successfully")

            } catch (e: Exception) {
                showSnackbar("Failed to clear data: ${e.message}")
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

    private fun showPrivacyPolicy() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Privacy Policy")
            .setMessage(
                "ScrollSense Privacy Policy:\n\n" +
                        "• All usage data stays on your device\n" +
                        "• No personal data is collected or transmitted\n" +
                        "• App categorization uses Gemini AI API (anonymous)\n" +
                        "• No advertising or tracking\n" +
                        "• Open source and transparent\n\n" +
                        "Your privacy is our priority."
            )
            .setPositiveButton("OK", null)
            .show()
    }

    private fun showAboutDialog() {
        MaterialAlertDialogBuilder(requireContext())
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
            .setNeutralButton("GitHub") { _, _ ->
                openGitHub()
            }
            .show()
    }

    private fun rateApp() {
        try {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse("market://details?id=${requireContext().packageName}")
            }
            startActivity(intent)
        } catch (e: Exception) {
            // Fallback to web browser
            val intent = Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse("https://play.google.com/store/apps/details?id=${requireContext().packageName}")
            }
            try {
                startActivity(intent)
            } catch (e: Exception) {
                showSnackbar("Unable to open Play Store")
            }
        }
    }

    private fun openGitHub() {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            data = Uri.parse("https://github.com/LOHITH5506H/ScrollSense")
        }
        try {
            startActivity(intent)
        } catch (e: Exception) {
            showSnackbar("Unable to open GitHub")
        }
    }

    private fun showSnackbar(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_SHORT).show()
    }

    override fun onResume() {
        super.onResume()
        updateCacheInfo()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}