package com.meettrack.app.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.meettrack.app.data.backup.BackupManager
import com.meettrack.app.data.preferences.AppSettings
import com.meettrack.app.data.preferences.SettingsRepository
import com.meettrack.app.data.repository.MeetingRepository
import com.meettrack.app.notifications.ReminderRescheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val backupManager: BackupManager,
    private val meetingRepository: MeetingRepository,
    private val reminderRescheduler: ReminderRescheduler
) : ViewModel() {

    val settings: StateFlow<AppSettings> = settingsRepository.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppSettings())

    private val _isProcessingBackup = MutableStateFlow(false)
    val isProcessingBackup: StateFlow<Boolean> = _isProcessingBackup.asStateFlow()

    fun updateDarkTheme(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.updateDarkTheme(enabled) }
    }

    fun updateSystemTheme(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.updateSystemTheme(enabled) }
    }

    fun updateNotificationsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            val wasEnabled = settingsRepository.settings.first().notificationsEnabled
            settingsRepository.updateNotificationsEnabled(enabled)
            if (enabled && !wasEnabled) {
                reminderRescheduler.rescheduleUpcoming(
                    meetingRepository.getAllMeetingsWithClientsDirect()
                )
            }
        }
    }

    fun updateDefaultReminders(intervals: List<Int>) {
        viewModelScope.launch { settingsRepository.updateDefaultReminders(intervals) }
    }

    fun exportToJson(onReady: (String) -> Unit, onError: (String) -> Unit) =
        exportBackup(onReady, onError, backupManager::exportToJson)

    fun exportClientsCsv(onReady: (String) -> Unit, onError: (String) -> Unit) =
        exportBackup(onReady, onError, backupManager::exportClientsToCsv)

    fun exportMeetingsCsv(onReady: (String) -> Unit, onError: (String) -> Unit) =
        exportBackup(onReady, onError, backupManager::exportMeetingsToCsv)

    fun importFromJson(jsonString: String, onComplete: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            _isProcessingBackup.value = true
            try {
                val result = try {
                    backupManager.importFromJson(jsonString)
                } catch (e: Exception) {
                    Result.failure(e)
                }
                result.fold(
                    onSuccess = { summary ->
                        onComplete(
                            true,
                            "Successfully restored ${summary.clientsCount} clients and ${summary.meetingsCount} meetings."
                        )
                    },
                    onFailure = { error ->
                        onComplete(false, "Restore failed: ${error.message ?: "Invalid data"}")
                    }
                )
            } finally {
                _isProcessingBackup.value = false
            }
        }
    }

    private fun exportBackup(
        onReady: (String) -> Unit,
        onError: (String) -> Unit,
        export: suspend () -> String
    ) {
        viewModelScope.launch {
            _isProcessingBackup.value = true
            try {
                val content = try {
                    export()
                } catch (e: Exception) {
                    onError("Export failed: ${e.message ?: "Unable to generate backup"}")
                    return@launch
                }
                onReady(content)
            } finally {
                _isProcessingBackup.value = false
            }
        }
    }
}
