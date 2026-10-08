package com.example.flowerid.security

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/** Stores the OpenCode Go API key and user preferences in encrypted shared prefs. */
class ApiKeyStore(context: Context) {

    private val prefs = EncryptedSharedPreferences.create(
        context,
        FILE_NAME,
        MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build(),
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
    )

    fun getApiKey(): String? = prefs.getString(KEY_API, null)?.takeIf { it.isNotBlank() }

    fun setApiKey(value: String) {
        prefs.edit().putString(KEY_API, value.trim()).apply()
    }

    fun getModel(): String = prefs.getString(KEY_MODEL, null) ?: DEFAULT_MODEL

    fun setModel(value: String) {
        prefs.edit().putString(KEY_MODEL, value.trim().ifBlank { DEFAULT_MODEL }).apply()
    }

    fun getQuality(): Int = prefs.getInt(KEY_QUALITY, 85)

    fun setQuality(value: Int) {
        prefs.edit().putInt(KEY_QUALITY, value.coerceIn(40, 100)).apply()
    }

    fun getMaxEdge(): Int = prefs.getInt(KEY_MAX_EDGE, 1280)

    fun setMaxEdge(value: Int) {
        prefs.edit().putInt(KEY_MAX_EDGE, value.coerceIn(480, 2048)).apply()
    }

    companion object {
        const val DEFAULT_MODEL = "deepseek-v4.1-flash"
        const val FALLBACK_MODEL = "deepseek-v4-flash-vision-exp"

        private const val FILE_NAME = "flowerid_secure"
        private const val KEY_API = "opencode_api_key"
        private const val KEY_MODEL = "model"
        private const val KEY_QUALITY = "quality"
        private const val KEY_MAX_EDGE = "max_edge"
    }
}
