package com.example.ethfinance.data.local.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_settings")

class SettingsDataStore(private val context: Context) {

    companion object {
        val KEY_MODE = stringPreferencesKey("app_mode") // "DEMO" or "LIVE"
        val KEY_THEME = stringPreferencesKey("theme_mode") // "SYSTEM", "LIGHT", "DARK"
        val KEY_ACCENT = stringPreferencesKey("accent_palette")
        val KEY_DYNAMIC_COLOR = booleanPreferencesKey("dynamic_color")
        val KEY_AMOLED_BLACK = booleanPreferencesKey("amoled_black")
        val KEY_FONT_SCALE = floatPreferencesKey("font_scale")
        val KEY_BOLD_NUMBERS = booleanPreferencesKey("bold_numbers")
        val KEY_LANGUAGE = stringPreferencesKey("app_language")
        val KEY_ETHIOPIAN_CALENDAR = booleanPreferencesKey("ethiopian_calendar")
        val KEY_CURRENCY_DISPLAY = stringPreferencesKey("currency_display")
        val KEY_HIDE_AMOUNTS = booleanPreferencesKey("hide_amounts")
        val KEY_KEEP_ORIGINAL_SMS = booleanPreferencesKey("keep_original_sms")
        val KEY_APP_LOCK = booleanPreferencesKey("app_lock")
        val KEY_TRACKED_SENDERS = stringSetPreferencesKey("tracked_senders")
    }

    val modeFlow: Flow<String> = context.dataStore.data.map { it[KEY_MODE] ?: "DEMO" }
    val themeFlow: Flow<String> = context.dataStore.data.map { it[KEY_THEME] ?: "SYSTEM" }
    val fontScaleFlow: Flow<Float> = context.dataStore.data.map { it[KEY_FONT_SCALE] ?: 1.0f }
    val boldNumbersFlow: Flow<Boolean> = context.dataStore.data.map { it[KEY_BOLD_NUMBERS] ?: false }
    val languageFlow: Flow<String> = context.dataStore.data.map { it[KEY_LANGUAGE] ?: "en" }
    val useEthiopianCalendarFlow: Flow<Boolean> = context.dataStore.data.map { it[KEY_ETHIOPIAN_CALENDAR] ?: false }
    val currencyDisplayFlow: Flow<String> = context.dataStore.data.map { it[KEY_CURRENCY_DISPLAY] ?: "ETB" }
    val hideAmountsFlow: Flow<Boolean> = context.dataStore.data.map { it[KEY_HIDE_AMOUNTS] ?: false }
    val keepOriginalSmsFlow: Flow<Boolean> = context.dataStore.data.map { it[KEY_KEEP_ORIGINAL_SMS] ?: false }

    suspend fun setMode(mode: String) {
        context.dataStore.edit { it[KEY_MODE] = mode }
    }

    suspend fun setTheme(theme: String) {
        context.dataStore.edit { it[KEY_THEME] = theme }
    }

    suspend fun setFontScale(scale: Float) {
        context.dataStore.edit { it[KEY_FONT_SCALE] = scale }
    }

    suspend fun setBoldNumbers(bold: Boolean) {
        context.dataStore.edit { it[KEY_BOLD_NUMBERS] = bold }
    }

    suspend fun setLanguage(lang: String) {
        context.dataStore.edit { it[KEY_LANGUAGE] = lang }
    }

    suspend fun setEthiopianCalendar(enabled: Boolean) {
        context.dataStore.edit { it[KEY_ETHIOPIAN_CALENDAR] = enabled }
    }

    suspend fun setCurrencyDisplay(currency: String) {
        context.dataStore.edit { it[KEY_CURRENCY_DISPLAY] = currency }
    }

    suspend fun toggleHideAmounts() {
        context.dataStore.edit {
            val current = it[KEY_HIDE_AMOUNTS] ?: false
            it[KEY_HIDE_AMOUNTS] = !current
        }
    }

    suspend fun setKeepOriginalSms(keep: Boolean) {
        context.dataStore.edit { it[KEY_KEEP_ORIGINAL_SMS] = keep }
    }
}
