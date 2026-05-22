package com.example.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AppSettings(private val context: Context) {

    private val sharedPrefs: SharedPreferences = context.getSharedPreferences("volege_prefs", Context.MODE_PRIVATE)

    companion object {
        const val KEY_SIDEBAR_ENABLED = "sidebar_enabled"
        const val KEY_SIDEBAR_POSITION = "sidebar_position" // "LEFT" or "RIGHT"
        const val KEY_SIDEBAR_TRANSPARENCY = "sidebar_transparency" // 0.1f to 1.0f
        const val KEY_SIDEBAR_SIZE = "sidebar_size" // "SMALL", "MEDIUM", "LARGE"
        const val KEY_AUTO_HIDE_DURATION = "auto_hide_duration" // in seconds: 2, 3, 5, 8, 10
        const val KEY_THEME = "theme" // "DARK_NAVY", "ELECTRIC_BLUE", "BLACK_PREMIUM", "CHARCOAL"
        const val KEY_VIBRATION_ENABLED = "vibration_enabled"
        const val KEY_STARTUP_AT_BOOT = "startup_at_boot"
    }

    // MutableStateFlows for reactive observation
    private val _isSidebarEnabled = MutableStateFlow(getSidebarEnabled())
    val isSidebarEnabled: StateFlow<Boolean> = _isSidebarEnabled.asStateFlow()

    private val _sidebarPosition = MutableStateFlow(getSidebarPosition())
    val sidebarPosition: StateFlow<String> = _sidebarPosition.asStateFlow()

    private val _sidebarTransparency = MutableStateFlow(getSidebarTransparency())
    val sidebarTransparency: StateFlow<Float> = _sidebarTransparency.asStateFlow()

    private val _sidebarSize = MutableStateFlow(getSidebarSize())
    val sidebarSize: StateFlow<String> = _sidebarSize.asStateFlow()

    private val _autoHideDuration = MutableStateFlow(getAutoHideDuration())
    val autoHideDuration: StateFlow<Int> = _autoHideDuration.asStateFlow()

    private val _theme = MutableStateFlow(getTheme())
    val theme: StateFlow<String> = _theme.asStateFlow()

    private val _vibrationEnabled = MutableStateFlow(getVibrationEnabled())
    val vibrationEnabled: StateFlow<Boolean> = _vibrationEnabled.asStateFlow()

    private val _startupAtBoot = MutableStateFlow(getStartupAtBoot())
    val startupAtBoot: StateFlow<Boolean> = _startupAtBoot.asStateFlow()

    private val preferenceChangeListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        when (key) {
            KEY_SIDEBAR_ENABLED -> _isSidebarEnabled.value = getSidebarEnabled()
            KEY_SIDEBAR_POSITION -> _sidebarPosition.value = getSidebarPosition()
            KEY_SIDEBAR_TRANSPARENCY -> _sidebarTransparency.value = getSidebarTransparency()
            KEY_SIDEBAR_SIZE -> _sidebarSize.value = getSidebarSize()
            KEY_AUTO_HIDE_DURATION -> _autoHideDuration.value = getAutoHideDuration()
            KEY_THEME -> _theme.value = getTheme()
            KEY_VIBRATION_ENABLED -> _vibrationEnabled.value = getVibrationEnabled()
            KEY_STARTUP_AT_BOOT -> _startupAtBoot.value = getStartupAtBoot()
        }
    }

    init {
        sharedPrefs.registerOnSharedPreferenceChangeListener(preferenceChangeListener)
    }

    fun cleanup() {
        sharedPrefs.unregisterOnSharedPreferenceChangeListener(preferenceChangeListener)
    }

    // Getters and Setters
    fun getSidebarEnabled(): Boolean = sharedPrefs.getBoolean(KEY_SIDEBAR_ENABLED, false)
    fun setSidebarEnabled(enabled: Boolean) {
        sharedPrefs.edit().putBoolean(KEY_SIDEBAR_ENABLED, enabled).apply()
    }

    fun getSidebarPosition(): String = sharedPrefs.getString(KEY_SIDEBAR_POSITION, "RIGHT") ?: "RIGHT"
    fun setSidebarPosition(position: String) {
        sharedPrefs.edit().putString(KEY_SIDEBAR_POSITION, position).apply()
    }

    fun getSidebarTransparency(): Float = sharedPrefs.getFloat(KEY_SIDEBAR_TRANSPARENCY, 0.85f)
    fun setSidebarTransparency(transparency: Float) {
        sharedPrefs.edit().putFloat(KEY_SIDEBAR_TRANSPARENCY, transparency).apply()
    }

    fun getSidebarSize(): String = sharedPrefs.getString(KEY_SIDEBAR_SIZE, "MEDIUM") ?: "MEDIUM"
    fun setSidebarSize(size: String) {
        sharedPrefs.edit().putString(KEY_SIDEBAR_SIZE, size).apply()
    }

    fun getAutoHideDuration(): Int = sharedPrefs.getInt(KEY_AUTO_HIDE_DURATION, 5)
    fun setAutoHideDuration(duration: Int) {
        sharedPrefs.edit().putInt(KEY_AUTO_HIDE_DURATION, duration).apply()
    }

    fun getTheme(): String = sharedPrefs.getString(KEY_THEME, "DARK_NAVY") ?: "DARK_NAVY"
    fun setTheme(theme: String) {
        sharedPrefs.edit().putString(KEY_THEME, theme).apply()
    }

    fun getVibrationEnabled(): Boolean = sharedPrefs.getBoolean(KEY_VIBRATION_ENABLED, true)
    fun setVibrationEnabled(enabled: Boolean) {
        sharedPrefs.edit().putBoolean(KEY_VIBRATION_ENABLED, enabled).apply()
    }

    fun getStartupAtBoot(): Boolean = sharedPrefs.getBoolean(KEY_STARTUP_AT_BOOT, false)
    fun setStartupAtBoot(enabled: Boolean) {
        sharedPrefs.edit().putBoolean(KEY_STARTUP_AT_BOOT, enabled).apply()
    }
}
