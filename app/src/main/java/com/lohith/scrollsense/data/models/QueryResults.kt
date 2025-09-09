package com.lohith.scrollsense.data.models

// We have moved the data classes here, to their own dedicated file.

data class TotalByAppAndType(
    val packageName: String,
    val contentType: String,
    val totalMs: Long
)

data class TotalByType(
    val contentType: String,
    val totalMs: Long
)