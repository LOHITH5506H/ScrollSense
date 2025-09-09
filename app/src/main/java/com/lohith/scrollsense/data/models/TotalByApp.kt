package com.lohith.scrollsense.data.models

data class TotalByApp(
    // This field must be named 'appName' to match the SQL query result
    val appName: String,
    val totalMs: Long
)