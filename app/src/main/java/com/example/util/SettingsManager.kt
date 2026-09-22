package com.example.util

import android.content.Context
import android.content.SharedPreferences
import com.example.model.AppColorTheme
import com.example.model.CloudAccount
import com.example.model.ThemeMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONObject

class SettingsManager private constructor(context: Context) {

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    // Theme Color
    private val _colorTheme = MutableStateFlow(loadColorTheme())
    val colorTheme: StateFlow<AppColorTheme> = _colorTheme.asStateFlow()

    // Theme Mode
    private val _themeMode = MutableStateFlow(loadThemeMode())
    val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()

    // App Lock
    private val _isAppLockEnabled = MutableStateFlow(prefs.getBoolean(KEY_APP_LOCK_ENABLED, false))
    val isAppLockEnabled: StateFlow<Boolean> = _isAppLockEnabled.asStateFlow()

    private val _hasPinSet = MutableStateFlow(!prefs.getString(KEY_PIN_HASH, "").isNullOrEmpty())
    val hasPinSet: StateFlow<Boolean> = _hasPinSet.asStateFlow()

    // Dynamic session unlock state
    private val _isAppUnlocked = MutableStateFlow(!prefs.getBoolean(KEY_APP_LOCK_ENABLED, false))
    val isAppUnlocked: StateFlow<Boolean> = _isAppUnlocked.asStateFlow()

    // Cloud Account
    private val _cloudAccount = MutableStateFlow(loadCloudAccount())
    val cloudAccount: StateFlow<CloudAccount?> = _cloudAccount.asStateFlow()

    companion object {
        private const val PREFS_NAME = "trading_journal_settings"
        private const val KEY_COLOR_THEME = "key_color_theme"
        private const val KEY_THEME_MODE = "key_theme_mode"
        private const val KEY_APP_LOCK_ENABLED = "key_app_lock_enabled"
        private const val KEY_PIN_HASH = "key_pin_hash"
        private const val KEY_CLOUD_ACCOUNT = "key_cloud_account"
        private const val KEY_CLOUD_BACKUP_PAYLOAD = "key_cloud_backup_payload"
        private const val KEY_LAST_BACKUP_TIME = "key_last_backup_time"
        private const val KEY_LAST_BACKUP_COUNT = "key_last_backup_count"

        @Volatile
        private var INSTANCE: SettingsManager? = null

        fun getInstance(context: Context): SettingsManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: SettingsManager(context).also { INSTANCE = it }
            }
        }
    }

    private fun loadColorTheme(): AppColorTheme {
        val name = prefs.getString(KEY_COLOR_THEME, AppColorTheme.EMERALD.name) ?: AppColorTheme.EMERALD.name
        return try {
            AppColorTheme.valueOf(name)
        } catch (_: Exception) {
            AppColorTheme.EMERALD
        }
    }

    fun setColorTheme(theme: AppColorTheme) {
        prefs.edit().putString(KEY_COLOR_THEME, theme.name).apply()
        _colorTheme.value = theme
    }

    private fun loadThemeMode(): ThemeMode {
        val name = prefs.getString(KEY_THEME_MODE, ThemeMode.DARK.name) ?: ThemeMode.DARK.name
        return try {
            ThemeMode.valueOf(name)
        } catch (_: Exception) {
            ThemeMode.DARK
        }
    }

    fun setThemeMode(mode: ThemeMode) {
        prefs.edit().putString(KEY_THEME_MODE, mode.name).apply()
        _themeMode.value = mode
    }

    // --- App Lock Security ---

    fun setAppLockEnabled(enabled: Boolean, pin: String? = null): Boolean {
        if (enabled && pin != null) {
            prefs.edit()
                .putBoolean(KEY_APP_LOCK_ENABLED, true)
                .putString(KEY_PIN_HASH, hashPin(pin))
                .apply()
            _isAppLockEnabled.value = true
            _hasPinSet.value = true
            _isAppUnlocked.value = true
            return true
        } else if (!enabled) {
            prefs.edit()
                .putBoolean(KEY_APP_LOCK_ENABLED, false)
                .apply()
            _isAppLockEnabled.value = false
            _isAppUnlocked.value = true
            return true
        }
        return false
    }

    fun changePin(oldPin: String, newPin: String): Boolean {
        val storedHash = prefs.getString(KEY_PIN_HASH, "") ?: ""
        if (storedHash.isNotEmpty() && hashPin(oldPin) != storedHash) {
            return false
        }
        prefs.edit().putString(KEY_PIN_HASH, hashPin(newPin)).apply()
        _hasPinSet.value = true
        return true
    }

    fun setPin(newPin: String) {
        prefs.edit()
            .putBoolean(KEY_APP_LOCK_ENABLED, true)
            .putString(KEY_PIN_HASH, hashPin(newPin))
            .apply()
        _isAppLockEnabled.value = true
        _hasPinSet.value = true
        _isAppUnlocked.value = true
    }

    fun verifyPin(pin: String): Boolean {
        val storedHash = prefs.getString(KEY_PIN_HASH, "") ?: ""
        if (storedHash.isEmpty()) {
            _isAppUnlocked.value = true
            return true
        }
        val matches = hashPin(pin) == storedHash
        if (matches) {
            _isAppUnlocked.value = true
        }
        return matches
    }

    fun unlockApp() {
        _isAppUnlocked.value = true
    }

    fun lockApp() {
        if (_isAppLockEnabled.value) {
            _isAppUnlocked.value = false
        }
    }

    private fun hashPin(pin: String): String {
        return pin.reversed() + "_salt_tj"
    }

    // --- Cloud Account & Backup ---

    private fun loadCloudAccount(): CloudAccount? {
        val raw = prefs.getString(KEY_CLOUD_ACCOUNT, null) ?: return null
        return try {
            val json = JSONObject(raw)
            CloudAccount(
                provider = json.optString("provider", "google"),
                email = json.optString("email", ""),
                displayName = json.optString("displayName", ""),
                photoUrl = json.optString("photoUrl").takeIf { it.isNotEmpty() },
                lastBackupTimestamp = prefs.getLong(KEY_LAST_BACKUP_TIME, 0L),
                lastBackupTradeCount = prefs.getInt(KEY_LAST_BACKUP_COUNT, 0)
            )
        } catch (_: Exception) {
            null
        }
    }

    fun signInWithGoogle(email: String, displayName: String) {
        val account = CloudAccount(
            provider = "google",
            email = email,
            displayName = displayName,
            lastBackupTimestamp = prefs.getLong(KEY_LAST_BACKUP_TIME, 0L),
            lastBackupTradeCount = prefs.getInt(KEY_LAST_BACKUP_COUNT, 0)
        )
        saveAccountToPrefs(account)
        _cloudAccount.value = account
    }

    fun signInWithFacebook(displayName: String, email: String) {
        val account = CloudAccount(
            provider = "facebook",
            email = email,
            displayName = displayName,
            lastBackupTimestamp = prefs.getLong(KEY_LAST_BACKUP_TIME, 0L),
            lastBackupTradeCount = prefs.getInt(KEY_LAST_BACKUP_COUNT, 0)
        )
        saveAccountToPrefs(account)
        _cloudAccount.value = account
    }

    fun signOutCloud() {
        prefs.edit()
            .remove(KEY_CLOUD_ACCOUNT)
            .apply()
        _cloudAccount.value = null
    }

    private fun saveAccountToPrefs(account: CloudAccount) {
        val json = JSONObject().apply {
            put("provider", account.provider)
            put("email", account.email)
            put("displayName", account.displayName)
            put("photoUrl", account.photoUrl ?: "")
        }
        prefs.edit().putString(KEY_CLOUD_ACCOUNT, json.toString()).apply()
    }

    fun saveCloudBackup(jsonPayload: String, tradeCount: Int) {
        val now = System.currentTimeMillis()
        prefs.edit()
            .putString(KEY_CLOUD_BACKUP_PAYLOAD, jsonPayload)
            .putLong(KEY_LAST_BACKUP_TIME, now)
            .putInt(KEY_LAST_BACKUP_COUNT, tradeCount)
            .apply()

        _cloudAccount.value = _cloudAccount.value?.copy(
            lastBackupTimestamp = now,
            lastBackupTradeCount = tradeCount
        )
    }

    fun getCloudBackupJson(): String? {
        return prefs.getString(KEY_CLOUD_BACKUP_PAYLOAD, null)
    }
}
