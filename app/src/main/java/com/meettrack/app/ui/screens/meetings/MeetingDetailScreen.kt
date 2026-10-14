package com.meettrack.app.ui.screens.meetings

import android.content.Intent
import android.net.Uri
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.meettrack.app.R
import com.meettrack.app.data.model.MeetingStatus
import com.meettrack.app.ui.components.InfoCard
import com.meettrack.app.ui.components.InfoRow
import com.meettrack.app.ui.components.MeetingStatusChip
import com.meettrack.app.util.formatMeetingDateTime
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MeetingDetailScreen(
    meetingId: Long,
    onNavigateBack: () -> Unit,
    onNavigateToEdit: () -> Unit,
    onNavigateToClient: (Long) -> Unit,
    viewModel: MeetingViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val meetingDetail by viewModel.meetingDetail.collectAsStateWithLifecycle()
    var showCancelDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showCompleteDialog by remember { mutableStateOf(false) }

    LaunchedEffect(meetingId) {
        viewModel.loadMeeting(meetingId)
    }

    val meeting = meetingDetail?.meeting
    val client = meetingDetail?.client

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.meeting_details), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (meeting?.status == MeetingStatus.UPCOMING) {
                        IconButton(onClick = onNavigateToEdit) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit")
                        }
                    }
                    var showMenu by remember { mutableStateOf(false) }
                    IconButton(onClick = { showMenu = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "More options")
                    }
                    DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                        if (meeting?.status == MeetingStatus.UPCOMING) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.mark_complete)) },
                                onClick = { showMenu = false; showCompleteDialog = true },
                                leadingIcon = { Icon(Icons.Default.CheckCircle, contentDescription = null) }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.cancel_meeting)) },
                                onClick = { showMenu = false; showCancelDialog = true },
                                leadingIcon = { Icon(Icons.Default.Cancel, contentDescription = null) }
                            )
                        }
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.delete), color = MaterialTheme.colorScheme.error) },
                            onClick = { showMenu = false; showDeleteDialog = true },
                            leadingIcon = {
                                Icon(Icons.Default.Delete, contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error)
                            }
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        if (meeting == null || client == null) {
            Box(Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(bottom = 24.dp)
        ) {
            // Hero header
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 20.dp)
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        MeetingStatusChip(status = meeting.status)
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        meeting.title,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CalendarToday, contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(4.dp))
                        Text(
                            meeting.dateTime.formatMeetingDateTime(
                                DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy 'at' h:mm a")
                            ),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            // Quick actions
            if (meeting.status == MeetingStatus.UPCOMING) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onNavigateToEdit,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(stringResource(R.string.reschedule))
                    }
                    if (meeting.location.isNotBlank()) {
                        OutlinedButton(
                            onClick = {
                                val uri = Uri.parse("geo:0,0?q=${Uri.encode(meeting.location)}")
                                context.startActivity(Intent(Intent.ACTION_VIEW, uri))
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Navigation, contentDescription = null,
                                modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(stringResource(R.string.navigate))
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
            }

            // Meeting info
            InfoCard(title = "Meeting Information") {
                InfoRow(icon = Icons.Default.Title, label = "Title", value = meeting.title)
                InfoRow(
                    icon = Icons.Default.CalendarToday,
                    label = "Date & Time",
                    value = meeting.dateTime.formatMeetingDateTime(
                        DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy 'at' h:mm a")
                    )
                )
                if (meeting.location.isNotBlank()) {
                    InfoRow(icon = Icons.Default.LocationOn, label = "Location", value = meeting.location)
                }
                if (meeting.notes.isNotBlank()) {
                    InfoRow(icon = Icons.Default.Notes, label = "Notes", value = meeting.notes)
                }
            }

            Spacer(Modifier.height(12.dp))

            // Client info
            Card(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                shape = RoundedCornerShape(16.dp),
                onClick = { onNavigateToClient(client.id) }
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(stringResource(R.string.client), style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary)
                        Text(client.fullName, style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold)
                        if (client.company.isNotBlank()) {
                            Text(client.company, style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Text(client.phoneNumber, style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Icon(Icons.Default.ChevronRight, contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            // Reminders
            val intervals = try {
                com.google.gson.Gson().fromJson(
                    meeting.reminderIntervals,
                    object : com.google.gson.reflect.TypeToken<List<Int>>() {}.type
                ) as List<Int>
            } catch (e: Exception) { emptyList() }

            if (intervals.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                InfoCard(title = "Reminders") {
                    intervals.forEach { minutes ->
                        val label = REMINDER_OPTIONS.find { it.first == minutes }?.second
                            ?: "$minutes minutes"
                        InfoRow(
                            icon = Icons.Default.Notifications,
                            label = "Alert",
                            value = "$label before"
                        )
                    }
                }
            }
        }
    }

    // Dialogs
    if (showCompleteDialog) {
        AlertDialog(
            onDismissRequest = { showCompleteDialog = false },
            title = { Text(stringResource(R.string.mark_as_complete)) },
            text = { Text(stringResource(R.string.meeting_complete_confirmation)) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.completeMeeting(meetingId) { onNavigateBack() }
                    showCompleteDialog = false
                }) { Text(stringResource(R.string.complete)) }
            },
            dismissButton = { TextButton(onClick = { showCompleteDialog = false }) { Text(stringResource(R.string.cancel)) } }
        )
    }

    if (showCancelDialog) {
        AlertDialog(
            onDismissRequest = { showCancelDialog = false },
            title = { Text(stringResource(R.string.cancel_meeting)) },
            text = { Text(stringResource(R.string.cancel_meeting_confirmation)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.cancelMeeting(meetingId) { onNavigateBack() }
                        showCancelDialog = false
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) { Text(stringResource(R.string.cancel_meeting)) }
            },
            dismissButton = { TextButton(onClick = { showCancelDialog = false }) { Text(stringResource(R.string.back)) } }
        )
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            icon = { Icon(Icons.Default.Warning, contentDescription = null) },
            title = { Text(stringResource(R.string.delete_meeting)) },
            text = { Text(stringResource(R.string.delete_meeting_confirmation)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteMeeting(meetingId) { onNavigateBack() }
                        showDeleteDialog = false
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) { Text(stringResource(R.string.delete)) }
            },
            dismissButton = { TextButton(onClick = { showDeleteDialog = false }) { Text(stringResource(R.string.cancel)) } }
        )
    }
}
