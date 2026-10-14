package com.meettrack.app.ui.screens.meetings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.meettrack.app.R
import com.meettrack.app.data.model.Client
import com.meettrack.app.data.model.MeetingStatus
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

val REMINDER_OPTIONS = listOf(
    10 to "10 minutes",
    30 to "30 minutes",
    60 to "1 hour",
    120 to "2 hours",
    1440 to "1 day"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddMeetingScreen(
    preselectedClientId: Long?,
    onNavigateBack: () -> Unit,
    onMeetingAdded: (Long) -> Unit,
    viewModel: MeetingViewModel = hiltViewModel()
) {
    LaunchedEffect(preselectedClientId) {
        if (preselectedClientId != null) {
            viewModel.preselectClient(preselectedClientId)
        }
    }

    MeetingFormScreen(
        title = "Schedule Meeting",
        submitLabel = "Schedule",
        onNavigateBack = onNavigateBack,
        viewModel = viewModel,
        onSubmit = { viewModel.saveNewMeeting(onMeetingAdded) }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditMeetingScreen(
    meetingId: Long,
    onNavigateBack: () -> Unit,
    viewModel: MeetingViewModel = hiltViewModel()
) {
    LaunchedEffect(meetingId) {
        viewModel.loadMeeting(meetingId)
    }

    MeetingFormScreen(
        title = "Edit Meeting",
        submitLabel = "Save Changes",
        onNavigateBack = onNavigateBack,
        viewModel = viewModel,
        onSubmit = { viewModel.updateExistingMeeting(meetingId, onNavigateBack) }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MeetingFormScreen(
    title: String,
    submitLabel: String,
    onNavigateBack: () -> Unit,
    viewModel: MeetingViewModel,
    onSubmit: () -> Unit
) {
    val formState by viewModel.formState.collectAsStateWithLifecycle()
    val allClients by viewModel.allClients.collectAsStateWithLifecycle()
    val scrollState = rememberScrollState()
    val snackbarHostState = remember { SnackbarHostState() }

    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var showClientDropdown by remember { mutableStateOf(false) }

    LaunchedEffect(formState.error) {
        formState.error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    TextButton(onClick = onSubmit, enabled = !formState.isLoading) {
                        Text(submitLabel, fontWeight = FontWeight.SemiBold)
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Title
            OutlinedTextField(
                value = formState.title,
                onValueChange = viewModel::updateTitle,
                label = { Text(stringResource(R.string.meeting_title_purpose)) },
                leadingIcon = { Icon(Icons.Default.Title, contentDescription = null) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            // Client selector
            ExposedDropdownMenuBox(
                expanded = showClientDropdown,
                onExpandedChange = { showClientDropdown = it }
            ) {
                OutlinedTextField(
                    value = formState.selectedClient?.fullName ?: "",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text(stringResource(R.string.client_required)) },
                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = showClientDropdown) },
                    modifier = Modifier.fillMaxWidth().menuAnchor()
                )
                ExposedDropdownMenu(
                    expanded = showClientDropdown,
                    onDismissRequest = { showClientDropdown = false }
                ) {
                    if (allClients.isEmpty()) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.no_clients_add_one_first)) },
                            onClick = {}
                        )
                    } else {
                        allClients.forEach { client ->
                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Text(client.fullName, fontWeight = FontWeight.Medium)
                                        if (client.company.isNotBlank()) {
                                            Text(client.company, style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    }
                                },
                                onClick = {
                                    viewModel.updateClient(client)
                                    showClientDropdown = false
                                }
                            )
                        }
                    }
                }
            }

            // Date & Time
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = formState.date.format(DateTimeFormatter.ofPattern("MMM d, yyyy")),
                    onValueChange = {},
                    readOnly = true,
                    label = { Text(stringResource(R.string.date)) },
                    leadingIcon = { Icon(Icons.Default.CalendarToday, contentDescription = null) },
                    modifier = Modifier.weight(1f),
                    trailingIcon = {
                        IconButton(onClick = { showDatePicker = true }) {
                            Icon(Icons.Default.Edit, contentDescription = "Pick date")
                        }
                    }
                )
                OutlinedTextField(
                    value = formState.time.format(DateTimeFormatter.ofPattern("h:mm a")),
                    onValueChange = {},
                    readOnly = true,
                    label = { Text(stringResource(R.string.time)) },
                    leadingIcon = { Icon(Icons.Default.Schedule, contentDescription = null) },
                    modifier = Modifier.weight(1f),
                    trailingIcon = {
                        IconButton(onClick = { showTimePicker = true }) {
                            Icon(Icons.Default.Edit, contentDescription = "Pick time")
                        }
                    }
                )
            }

            // Location
            OutlinedTextField(
                value = formState.location,
                onValueChange = viewModel::updateLocation,
                label = { Text(stringResource(R.string.meeting_location)) },
                leadingIcon = { Icon(Icons.Default.LocationOn, contentDescription = null) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            // Status (for edit)
            if (submitLabel == "Save Changes") {
                Column {
                    Text(stringResource(R.string.status), style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        MeetingStatus.values().forEach { status ->
                            FilterChip(
                                selected = formState.status == status,
                                onClick = { viewModel.updateStatus(status) },
                                label = { Text(status.name.lowercase().replaceFirstChar { it.uppercase() }) }
                            )
                        }
                    }
                }
            }

            // Reminders
            Column {
                Text(
                    "Reminders",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(8.dp))
                REMINDER_OPTIONS.forEach { (minutes, label) ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Checkbox(
                            checked = formState.reminderIntervals.contains(minutes),
                            onCheckedChange = { viewModel.toggleReminderInterval(minutes) }
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("$label before", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }

            // Notes
            OutlinedTextField(
                value = formState.notes,
                onValueChange = viewModel::updateNotes,
                label = { Text(stringResource(R.string.notes)) },
                leadingIcon = { Icon(Icons.Default.Notes, contentDescription = null) },
                modifier = Modifier.fillMaxWidth().height(100.dp)
            )

            Button(
                onClick = onSubmit,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                enabled = !formState.isLoading
            ) {
                if (formState.isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary)
                } else {
                    Icon(Icons.Default.Check, contentDescription = null,
                        modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(submitLabel, style = MaterialTheme.typography.titleSmall)
                }
            }
        }
    }

    // Date Picker Dialog
    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = formState.date
                .atStartOfDay(java.time.ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        val date = java.time.Instant.ofEpochMilli(millis)
                            .atZone(java.time.ZoneId.systemDefault())
                            .toLocalDate()
                        viewModel.updateDate(date)
                    }
                    showDatePicker = false
                }) { Text(stringResource(R.string.ok)) }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text(stringResource(R.string.cancel)) }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    // Time Picker Dialog
    if (showTimePicker) {
        val timePickerState = rememberTimePickerState(
            initialHour = formState.time.hour,
            initialMinute = formState.time.minute,
            is24Hour = false
        )
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            title = { Text(stringResource(R.string.pick_time)) },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    TimePicker(state = timePickerState)
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.updateTime(LocalTime.of(timePickerState.hour, timePickerState.minute))
                    showTimePicker = false
                }) { Text(stringResource(R.string.ok)) }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) { Text(stringResource(R.string.cancel)) }
            }
        )
    }
}
