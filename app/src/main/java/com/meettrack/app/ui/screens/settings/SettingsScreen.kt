package com.meettrack.app.ui.screens.settings

import android.content.Context
import android.content.Intent
import android.app.AlarmManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    paddingValues: PaddingValues,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val isProcessingBackup by viewModel.isProcessingBackup.collectAsStateWithLifecycle()
    val scrollState = rememberScrollState()
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val lifecycleOwner = LocalLifecycleOwner.current
    var canScheduleExactAlarms by remember {
        mutableStateOf(hasExactAlarmPermission(context))
    }

    var showRestoreDialog by remember { mutableStateOf(false) }
    var restoreInputText by remember { mutableStateOf("") }

    DisposableEffect(lifecycleOwner, context) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                canScheduleExactAlarms = hasExactAlarmPermission(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Scaffold(
        modifier = Modifier.padding(paddingValues),
        topBar = {
            TopAppBar(title = { Text("Settings", fontWeight = FontWeight.Bold) })
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Appearance
            SettingsSection(title = "Appearance") {
                SwitchSettingsItem(
                    icon = Icons.Default.Brightness4,
                    title = "Use System Theme",
                    subtitle = "Follow system light/dark mode",
                    checked = settings.useSystemTheme,
                    onCheckedChange = viewModel::updateSystemTheme
                )
                if (!settings.useSystemTheme) {
                    SwitchSettingsItem(
                        icon = Icons.Default.DarkMode,
                        title = "Dark Mode",
                        subtitle = "Use dark color scheme",
                        checked = settings.useDarkTheme,
                        onCheckedChange = viewModel::updateDarkTheme
                    )
                }
            }

            // Notifications
            SettingsSection(title = "Notifications") {
                SwitchSettingsItem(
                    icon = Icons.Default.Notifications,
                    title = "Meeting Reminders",
                    subtitle = "Show notifications before meetings",
                    checked = settings.notificationsEnabled,
                    onCheckedChange = viewModel::updateNotificationsEnabled
                )

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                    !canScheduleExactAlarms
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            "Allow exact alarms to receive meeting reminders on time.",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Button(
                            onClick = {
                                context.startActivity(
                                    Intent(
                                        Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
                                        Uri.parse("package:${context.packageName}")
                                    )
                                )
                            }
                        ) {
                            Text("Enable exact alarms")
                        }
                    }
                }

                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                    Text(
                        "Default Reminder Times",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        "Applied to new meetings (can override per meeting)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(8.dp))
                    val reminderOptions = listOf(10 to "10 min", 30 to "30 min", 60 to "1 hour", 1440 to "1 day")
                    reminderOptions.forEach { (minutes, label) ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Checkbox(
                                checked = settings.defaultReminderMinutes.contains(minutes),
                                onCheckedChange = { checked ->
                                    val current = settings.defaultReminderMinutes.toMutableList()
                                    if (checked) current.add(minutes) else current.remove(minutes)
                                    viewModel.updateDefaultReminders(current.sorted())
                                }
                            )
                            Text("$label before meeting", style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }

            // Data & Backup
            SettingsSection(title = "Data & Backup") {
                ActionSettingsItem(
                    icon = Icons.Default.CloudDownload,
                    title = "Backup to JSON",
                    subtitle = "Export all clients and meetings to JSON file",
                    onClick = {
                        viewModel.exportToJson { json ->
                            shareContent(context, json, "meettrack_backup.json", "application/json")
                            scope.launch {
                                snackbarHostState.showSnackbar("Backup generated! Choose where to save or share.")
                            }
                        }
                    }
                )
                ActionSettingsItem(
                    icon = Icons.Default.TableChart,
                    title = "Export Clients to CSV",
                    subtitle = "Spreadsheet of names, phones, addresses and GPS",
                    onClick = {
                        viewModel.exportClientsCsv { csv ->
                            shareContent(context, csv, "clients.csv", "text/csv")
                            scope.launch {
                                snackbarHostState.showSnackbar("Clients CSV generated!")
                            }
                        }
                    }
                )
                ActionSettingsItem(
                    icon = Icons.Default.EventNote,
                    title = "Export Meetings to CSV",
                    subtitle = "Spreadsheet of scheduled appointments and agendas",
                    onClick = {
                        viewModel.exportMeetingsCsv { csv ->
                            shareContent(context, csv, "meetings.csv", "text/csv")
                            scope.launch {
                                snackbarHostState.showSnackbar("Meetings CSV generated!")
                            }
                        }
                    }
                )
                ActionSettingsItem(
                    icon = Icons.Default.Restore,
                    title = "Restore from JSON",
                    subtitle = "Restore clients and meetings from backup JSON",
                    onClick = {
                        restoreInputText = ""
                        showRestoreDialog = true
                    }
                )
            }

            // About
            SettingsSection(title = "About") {
                SimpleSettingsItem(
                    icon = Icons.Default.Info,
                    title = "MeetTrack",
                    subtitle = "Version 1.0.0"
                )
                SimpleSettingsItem(
                    icon = Icons.Default.Storage,
                    title = "Local Database",
                    subtitle = "Room SQLite — Offline-first local persistence"
                )
                SimpleSettingsItem(
                    icon = Icons.Default.LocationOn,
                    title = "Location Privacy",
                    subtitle = "GPS coordinates stored only on your device"
                )
            }
        }
    }

    // Restore Backup Dialog
    if (showRestoreDialog) {
        AlertDialog(
            onDismissRequest = { showRestoreDialog = false },
            icon = { Icon(Icons.Default.Restore, contentDescription = null) },
            title = { Text("Restore Backup") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Paste your MeetTrack JSON backup payload below to restore clients and meetings:",
                        style = MaterialTheme.typography.bodySmall
                    )
                    OutlinedTextField(
                        value = restoreInputText,
                        onValueChange = { restoreInputText = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp),
                        placeholder = { Text("Paste JSON here…") },
                        maxLines = 10
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (restoreInputText.isNotBlank()) {
                            viewModel.importFromJson(restoreInputText) { success, message ->
                                scope.launch {
                                    snackbarHostState.showSnackbar(message)
                                }
                            }
                        }
                        showRestoreDialog = false
                    },
                    enabled = restoreInputText.isNotBlank() && !isProcessingBackup
                ) {
                    Text("Restore")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRestoreDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

private fun hasExactAlarmPermission(context: Context): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return true
    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    return alarmManager.canScheduleExactAlarms()
}

private fun shareContent(context: Context, content: String, subject: String, mimeType: String) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, subject)
        putExtra(Intent.EXTRA_TEXT, content)
    }
    context.startActivity(Intent.createChooser(intent, "Share $subject"))
}

@Composable
private fun SettingsSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column {
        Text(
            title,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(vertical = 4.dp)
        )
        Card(
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column { content() }
        }
    }
}

@Composable
private fun SwitchSettingsItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp))
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
            Text(subtitle, style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun ActionSettingsItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp))
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
            Text(subtitle, style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Icon(Icons.Default.ChevronRight, contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun SimpleSettingsItem(
    icon: ImageVector,
    title: String,
    subtitle: String
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp))
        Spacer(Modifier.width(16.dp))
        Column {
            Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
            Text(subtitle, style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
