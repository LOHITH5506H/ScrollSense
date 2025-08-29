package com.lohith.scrollsense.util

import android.util.Log
import android.view.accessibility.AccessibilityNodeInfo

object AccessibilityDebugUtils {
    private const val TAG = "AccessibilityDebug"

    /**
     * Print the entire accessibility node tree for debugging
     */
    fun printNodeTree(node: AccessibilityNodeInfo?, prefix: String = "") {
        if (node == null) return

        val nodeText = node.text?.toString() ?: ""
        val contentDesc = node.contentDescription?.toString() ?: ""
        val className = node.className?.toString() ?: ""
        val resourceId = node.viewIdResourceName ?: ""

        Log.d(TAG, "${prefix}Node: class=$className, text='$nodeText', contentDesc='$contentDesc', resourceId='$resourceId'")

        // Print children
        for (i in 0 until node.childCount) {
            val child = node.getChild(i)
            if (child != null) {
                printNodeTree(child, "$prefix  ")
                // Removed child.recycle(); newer platform handles recycling automatically
            }
        }
    }

    /**
     * Find all nodes with text content
     */
    fun findAllTextNodes(node: AccessibilityNodeInfo?): List<String> {
        val textNodes = mutableListOf<String>()
        if (node == null) return textNodes

        // Collect text from current node
        node.text?.let { if (it.isNotBlank()) textNodes.add("getText: $it") }
        node.contentDescription?.let { if (it.isNotBlank()) textNodes.add("contentDesc: $it") }

        // Collect from children
        for (i in 0 until node.childCount) {
            val child = node.getChild(i)
            if (child != null) {
                textNodes.addAll(findAllTextNodes(child))
            }
        }

        return textNodes
    }

    /**
     * Log all text content found in a node tree
     */
    fun logAllTexts(node: AccessibilityNodeInfo?, packageName: String) {
        val allTexts = findAllTextNodes(node)
        Log.d(TAG, "=== All texts for $packageName ===")
        allTexts.forEachIndexed { index, text ->
            Log.d(TAG, "[$index] $text")
        }
        Log.d(TAG, "=== End texts for $packageName ===")
    }
}