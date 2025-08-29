package com.lohith.scrollsense.data

import androidx.room.TypeConverter

// If AppDatabase.kt already declares an object Converters, remove that one when using this.
object AppTypeConverters {

    // Boolean conversion (not strictly required since Room handles Boolean -> INTEGER automatically;
    // retain only if you want explicit control).
    @TypeConverter
    @JvmStatic
    fun fromBoolean(value: Boolean?): Int? = value?.let { if (it) 1 else 0 }

    @TypeConverter
    @JvmStatic
    fun toBoolean(value: Int?): Boolean? = value?.let { it == 1 }

    // Add more converters here as new complex types are introduced.
    // ...existing code...
}
