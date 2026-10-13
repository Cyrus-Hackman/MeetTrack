package com.meettrack.app.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "meettrack_settings")

data class AppSettings(
    val defaultReminderMinutes: List<Int> = listOf(60, 30),
    val useDarkTheme: Boolean = false,
    val useSystemTheme: Boolean = true,
    val notificationsEnabled: Boolean = true,
    val defaultMeetingDurationMinutes: Int = 60
)

@Singleton
class SettingsRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private val KEY_DEFAULT_REMINDERS = stringPreferencesKey("default_reminders")
        private val KEY_USE_DARK_THEME = booleanPreferencesKey("use_dark_theme")
        private val KEY_USE_SYSTEM_THEME = booleanPreferencesKey("use_system_theme")
        private val KEY_NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
        private val KEY_DEFAULT_MEETING_DURATION = intPreferencesKey("default_meeting_duration")
    }

    val settings: Flow<AppSettings> = context.dataStore.data.map { prefs ->
        AppSettings(
            defaultReminderMinutes = prefs[KEY_DEFAULT_REMINDERS]
                ?.split(",")
                ?.mapNotNull { it.trim().toIntOrNull() }
                ?: listOf(60, 30),
            useDarkTheme = prefs[KEY_USE_DARK_THEME] ?: false,
            useSystemTheme = prefs[KEY_USE_SYSTEM_THEME] ?: true,
            notificationsEnabled = prefs[KEY_NOTIFICATIONS_ENABLED] ?: true,
            defaultMeetingDurationMinutes = prefs[KEY_DEFAULT_MEETING_DURATION] ?: 60
        )
    }

    suspend fun updateDefaultReminders(intervals: List<Int>) {
        context.dataStore.edit { prefs ->
            prefs[KEY_DEFAULT_REMINDERS] = intervals.joinToString(",")
        }
    }

    suspend fun updateDarkTheme(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[KEY_USE_DARK_THEME] = enabled
            prefs[KEY_USE_SYSTEM_THEME] = false
        }
    }

    suspend fun updateSystemTheme(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[KEY_USE_SYSTEM_THEME] = enabled
        }
    }

    suspend fun updateNotificationsEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[KEY_NOTIFICATIONS_ENABLED] = enabled
        }
    }

    suspend fun updateDefaultMeetingDuration(minutes: Int) {
        context.dataStore.edit { prefs ->
            prefs[KEY_DEFAULT_MEETING_DURATION] = minutes
        }
    }
}
