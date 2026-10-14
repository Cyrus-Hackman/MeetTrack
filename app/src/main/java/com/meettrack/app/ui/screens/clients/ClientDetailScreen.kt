package com.meettrack.app.ui.screens.clients

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
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
import com.meettrack.app.ui.components.*
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClientDetailScreen(
    clientId: Long,
    onNavigateBack: () -> Unit,
    onNavigateToEdit: () -> Unit,
    onNavigateToAddMeeting: () -> Unit,
    onNavigateToMeeting: (Long) -> Unit,
    onNavigateToMap: () -> Unit,
    viewModel: ClientViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val clientWithMeetings by viewModel.clientWithMeetings.collectAsStateWithLifecycle()
    var showDeleteDialog by remember { mutableStateOf(false) }

    LaunchedEffect(clientId) {
        viewModel.loadClient(clientId)
    }

    val client = clientWithMeetings?.client
    val meetings = clientWithMeetings?.meetings ?: emptyList()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(client?.fullName ?: "Client", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateToEdit) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit")
                    }
                    IconButton(onClick = { showDeleteDialog = true }) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete",
                            tint = MaterialTheme.colorScheme.error)
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onNavigateToAddMeeting,
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text(stringResource(R.string.schedule_meeting)) }
            )
        }
    ) { innerPadding ->
        if (client == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            contentPadding = PaddingValues(bottom = 88.dp)
        ) {
            // Avatar & name header
            item {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    ClientAvatar(name = client.fullName, size = 80)
                    Spacer(Modifier.height(16.dp))
                    Text(client.fullName, style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold)
                    if (client.company.isNotBlank()) {
                        Text(client.company, style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.primary)
                    }
                    Spacer(Modifier.height(16.dp))
                    // Quick action row
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedButton(
                            onClick = {
                                val intent = Intent(Intent.ACTION_DIAL,
                                    Uri.parse("tel:${client.phoneNumber}"))
                                context.startActivity(intent)
                            }
                        ) {
                            Icon(Icons.Default.Phone, contentDescription = null,
                                modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(stringResource(R.string.call))
                        }
                        if (client.email.isNotBlank()) {
                            OutlinedButton(
                                onClick = {
                                    val intent = Intent(Intent.ACTION_SENDTO,
                                        Uri.parse("mailto:${client.email}"))
                                    context.startActivity(intent)
                                }
                            ) {
                                Icon(Icons.Default.Email, contentDescription = null,
                                    modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(stringResource(R.string.email))
                            }
                        }
                    }
                }
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
            }

            // Contact Info Card
            item {
                Spacer(Modifier.height(16.dp))
                InfoCard(title = "Contact Information") {
                    InfoRow(icon = Icons.Default.Phone, label = "Phone", value = client.phoneNumber)
                    if (client.email.isNotBlank())
                        InfoRow(icon = Icons.Default.Email, label = "Email", value = client.email)
                    if (client.company.isNotBlank())
                        InfoRow(icon = Icons.Default.Business, label = "Company", value = client.company)
                    InfoRow(
                        icon = Icons.Default.CalendarToday,
                        label = "Added",
                        value = client.dateAdded.format(DateTimeFormatter.ofPattern("MMMM d, yyyy"))
                    )
                }
            }

            // Location Card
            item {
                Spacer(Modifier.height(12.dp))
                InfoCard(title = "Location") {
                    if (client.latitude != null && client.longitude != null) {
                        InfoRow(
                            icon = Icons.Default.MyLocation,
                            label = "GPS",
                            value = "%.6f, %.6f".format(client.latitude, client.longitude)
                        )
                        if (client.formattedAddress.isNotBlank()) {
                            InfoRow(
                                icon = Icons.Default.LocationOn,
                                label = "Address",
                                value = client.formattedAddress
                            )
                        }
                        client.locationLastUpdated?.let {
                            InfoRow(
                                icon = Icons.Default.Update,
                                label = "Updated",
                                value = it.format(DateTimeFormatter.ofPattern("MMM d, yyyy 'at' h:mm a"))
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(horizontal = 16.dp)) {
                            OutlinedButton(onClick = onNavigateToMap, modifier = Modifier.weight(1f)) {
                                Icon(Icons.Default.Map, contentDescription = null,
                                    modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text(stringResource(R.string.view_map))
                            }
                            OutlinedButton(
                                onClick = {
                                    val uri = Uri.parse("geo:${client.latitude},${client.longitude}?q=${client.latitude},${client.longitude}")
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
                    } else if (client.address.isNotBlank()) {
                        InfoRow(icon = Icons.Default.LocationOn, label = "Address", value = client.address)
                    } else {
                        Text(
                            "No location saved",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            }

            // Notes card
            if (client.notes.isNotBlank()) {
                item {
                    Spacer(Modifier.height(12.dp))
                    InfoCard(title = "Notes") {
                        Text(
                            client.notes,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            }

            // Meetings
            item {
                Spacer(Modifier.height(12.dp))
                SectionHeader(
                    title = "Meetings (${meetings.size})",
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                Spacer(Modifier.height(8.dp))
            }

            if (meetings.isEmpty()) {
                item {
                    EmptyState(
                        icon = Icons.Default.EventBusy,
                        title = "No meetings yet",
                        subtitle = "Schedule a meeting with ${client.fullName}",
                        actionLabel = "Schedule Meeting",
                        onAction = onNavigateToAddMeeting
                    )
                }
            } else {
                items(meetings.sortedByDescending { it.dateTime }) { meeting ->
                    // Wrap in MeetingWithClient for the card
                    MeetingCard(
                        meetingWithClient = com.meettrack.app.data.model.MeetingWithClient(meeting, client),
                        onClick = { onNavigateToMeeting(meeting.id) },
                        showClient = false,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                    )
                }
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            icon = { Icon(Icons.Default.Warning, contentDescription = null) },
            title = { Text(stringResource(R.string.delete_client)) },
            text = { Text("Delete ${client?.fullName}? All meeting records will also be removed.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteClient(clientId) { onNavigateBack() }
                        showDeleteDialog = false
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) { Text(stringResource(R.string.delete)) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text(stringResource(R.string.cancel)) }
            }
        )
    }
}

@Composable
fun InfoCard(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column {
            Text(
                title,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
            )
            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
            content()
        }
    }
}

@Composable
fun InfoRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            icon,
            contentDescription = null,
            modifier = Modifier.size(18.dp).padding(top = 2.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.width(12.dp))
        Column {
            Text(label, style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.bodyMedium)
        }
    }
}
