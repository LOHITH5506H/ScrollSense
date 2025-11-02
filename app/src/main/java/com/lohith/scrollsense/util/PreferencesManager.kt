package com.lohith.scrollsense.util

import android.content.Context
import android.content.SharedPreferences
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Singleton for managing SharedPreferences.
 *
 * This version includes encryption for sensitive keys (like parental passwords).
 */
class PreferencesManager private constructor(private val context: Context) {

    companion object {
        @Volatile private var INSTANCE: PreferencesManager? = null
        private const val PREFS_NAME = "ScrollSensePrefs"
        private const val KEY_RETENTION_DAYS = "retention_days"
        private const val KEY_PARENT_PWD_HASH = "parent_pwd_hash_v2"
        private const val KEY_PARENT_LIMITS = "parent_limits_v2"

        // Encryption constants
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val KEY_ALIAS = "ScrollSenseParentalKey"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val IV_SEPARATOR = "]"

        fun get(context: Context): PreferencesManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: PreferencesManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val gson = Gson()

    // --- Encryption setup ---
    private val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }

    private fun getSecretKey(): SecretKey {
        val existingKey = keyStore.getKey(KEY_ALIAS, null) as? SecretKey
        if (existingKey != null) {
            return existingKey
        }
        val keyGen = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        val spec = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .build()
        keyGen.init(spec)
        return keyGen.generateKey()
    }

    private fun encrypt(data: String): String {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getSecretKey())
        val iv = cipher.iv
        val encryptedData = cipher.doFinal(data.toByteArray())
        // Combine IV and encrypted data for storage
        val ivString = Base64.encodeToString(iv, Base64.DEFAULT)
        val dataString = Base64.encodeToString(encryptedData, Base64.DEFAULT)
        return "$ivString$IV_SEPARATOR$dataString"
    }

    private fun decrypt(encryptedString: String): String {
        try {
            val parts = encryptedString.split(IV_SEPARATOR)
            if (parts.size != 2) throw SecurityException("Invalid encrypted data format")

            val iv = Base64.decode(parts[0], Base64.DEFAULT)
            val encryptedData = Base64.decode(parts[1], Base64.DEFAULT)

            val cipher = Cipher.getInstance(TRANSFORMATION)
            val spec = GCMParameterSpec(128, iv)
            cipher.init(Cipher.DECRYPT_MODE, getSecretKey(), spec)
            return String(cipher.doFinal(encryptedData))
        } catch (e: Exception) {
            // If decryption fails (e.g., key invalidated), treat as logged out
            return ""
        }
    }

    // --- Retention ---
    fun setRetentionDays(days: Int) {
        prefs.edit().putInt(KEY_RETENTION_DAYS, days).apply()
    }

    fun getRetentionDays(): Int {
        return prefs.getInt(KEY_RETENTION_DAYS, 15) // Default 15 days
    }

    // --- Parental Controls ---
    fun setParentPassword(password: String) {
        // We encrypt the password itself (or a hash, but encrypting is fine)
        val encryptedPwd = encrypt(password)
        prefs.edit().putString(KEY_PARENT_PWD_HASH, encryptedPwd).apply()
    }

    fun isParentPasswordSet(): Boolean {
        return prefs.contains(KEY_PARENT_PWD_HASH)
    }

    fun verifyParentPassword(password: String): Boolean {
        val encryptedPwd = prefs.getString(KEY_PARENT_PWD_HASH, null) ?: return false
        val decryptedPwd = decrypt(encryptedPwd)
        return decryptedPwd == password
    }

    // --- Parental Limits (Encrypted) ---
    fun setParentalLimit(packageName: String, minutes: Int) {
        val limits = getParentalLimits().toMutableMap()
        limits[packageName] = minutes
        val json = gson.toJson(limits)
        prefs.edit().putString(KEY_PARENT_LIMITS, encrypt(json)).apply()
    }

    fun removeParentalLimit(packageName: String) {
        val limits = getParentalLimits().toMutableMap()
        limits.remove(packageName)
        val json = gson.toJson(limits)
        prefs.edit().putString(KEY_PARENT_LIMITS, encrypt(json)).apply()
    }

    fun getParentalLimits(): Map<String, Int> {
        val encryptedJson = prefs.getString(KEY_PARENT_LIMITS, null) ?: return emptyMap()
        val json = decrypt(encryptedJson)
        if (json.isBlank()) return emptyMap()

        val type = object : TypeToken<Map<String, Int>>() {}.type
        return gson.fromJson(json, type) ?: emptyMap()
    }

    fun getLimitForPackage(packageName: String): Int {
        return getParentalLimits()[packageName] ?: -1 // -1 means no limit
    }
}