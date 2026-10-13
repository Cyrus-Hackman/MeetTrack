package com.meettrack.app.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.meettrack.app.data.backup.BackupManager
import com.meettrack.app.data.preferences.AppSettings
import com.meettrack.app.data.preferences.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val backupManager: BackupManager
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
        viewModelScope.launch { settingsRepository.updateNotificationsEnabled(enabled) }
    }

    fun updateDefaultReminders(intervals: List<Int>) {
        viewModelScope.launch { settingsRepository.updateDefaultReminders(intervals) }
    }

    fun exportToJson(onReady: (String) -> Unit) {
        viewModelScope.launch {
            _isProcessingBackup.value = true
            try {
                val json = backupManager.exportToJson()
                onReady(json)
            } finally {
                _isProcessingBackup.value = false
            }
        }
    }

    fun exportClientsCsv(onReady: (String) -> Unit) {
        viewModelScope.launch {
            _isProcessingBackup.value = true
            try {
                val csv = backupManager.exportClientsToCsv()
                onReady(csv)
            } finally {
                _isProcessingBackup.value = false
            }
        }
    }

    fun exportMeetingsCsv(onReady: (String) -> Unit) {
        viewModelScope.launch {
            _isProcessingBackup.value = true
            try {
                val csv = backupManager.exportMeetingsToCsv()
                onReady(csv)
            } finally {
                _isProcessingBackup.value = false
            }
        }
    }

    fun importFromJson(jsonString: String, onComplete: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            _isProcessingBackup.value = true
            try {
                val result = backupManager.importFromJson(jsonString)
                result.onSuccess { summary ->
                    onComplete(true, "Successfully restored ${summary.clientsCount} clients and ${summary.meetingsCount} meetings.")
                }.onFailure { error ->
                    onComplete(false, "Restore failed: ${error.message ?: "Invalid data"}")
                }
            } finally {
                _isProcessingBackup.value = false
            }
        }
    }
}
