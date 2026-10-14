package com.meettrack.app.ui.screens.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.meettrack.app.R
import com.meettrack.app.data.model.Client
import com.meettrack.app.ui.components.*
import com.meettrack.app.ui.theme.*
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun DashboardScreen(
    paddingValues: PaddingValues,
    onNavigateToClients: () -> Unit,
    onNavigateToAddClient: () -> Unit,
    onNavigateToAddMeeting: () -> Unit,
    onNavigateToMeeting: (Long) -> Unit,
    onNavigateToClient: (Long) -> Unit,
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues),
        contentPadding = PaddingValues(bottom = 16.dp)
    ) {
        // Header Banner
        item {
            DashboardHeader(
                onAddClient = onNavigateToAddClient,
                onAddMeeting = onNavigateToAddMeeting
            )
        }

        // Stats Row
        item {
            Spacer(Modifier.height(8.dp))
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    StatCard(
                        label = "Total Clients",
                        value = uiState.clientCount.toString(),
                        icon = Icons.Default.People,
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.width(160.dp),
                        onClick = onNavigateToClients
                    )
                }
                item {
                    StatCard(
                        label = "Upcoming",
                        value = uiState.upcomingMeetingCount.toString(),
                        icon = Icons.Default.EventAvailable,
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.width(160.dp)
                    )
                }
                item {
                    StatCard(
                        label = "Overdue",
                        value = uiState.overdueMeetings.size.toString(),
                        icon = Icons.Default.Warning,
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.width(160.dp)
                    )
                }
                item {
                    StatCard(
                        label = "Today",
                        value = uiState.todaysMeetings.size.toString(),
                        icon = Icons.Default.Today,
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                        contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                        modifier = Modifier.width(160.dp)
                    )
                }
            }
        }

        // Today's meetings
        if (uiState.todaysMeetings.isNotEmpty()) {
            item {
                Spacer(Modifier.height(24.dp))
                SectionHeader(
                    title = "Today — ${LocalDate.now().format(DateTimeFormatter.ofPattern("MMMM d"))}",
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                Spacer(Modifier.height(8.dp))
            }
            items(uiState.todaysMeetings) { mwc ->
                MeetingCard(
                    meetingWithClient = mwc,
                    onClick = { onNavigateToMeeting(mwc.meeting.id) },
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )
            }
        }

        // Overdue meetings
        if (uiState.overdueMeetings.isNotEmpty()) {
            item {
                Spacer(Modifier.height(24.dp))
                SectionHeader(
                    title = "⚠ Overdue Meetings",
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                Spacer(Modifier.height(8.dp))
            }
            items(uiState.overdueMeetings.take(3)) { mwc ->
                MeetingCard(
                    meetingWithClient = mwc,
                    onClick = { onNavigateToMeeting(mwc.meeting.id) },
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )
            }
        }

        // Upcoming meetings
        if (uiState.upcomingMeetings.isNotEmpty()) {
            item {
                Spacer(Modifier.height(24.dp))
                SectionHeader(
                    title = "Upcoming Meetings",
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                Spacer(Modifier.height(8.dp))
            }
            items(uiState.upcomingMeetings) { mwc ->
                MeetingCard(
                    meetingWithClient = mwc,
                    onClick = { onNavigateToMeeting(mwc.meeting.id) },
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )
            }
        }

        // Recent clients
        if (uiState.recentClients.isNotEmpty()) {
            item {
                Spacer(Modifier.height(24.dp))
                SectionHeader(
                    title = "Recent Clients",
                    actionLabel = "View All",
                    onAction = onNavigateToClients,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                Spacer(Modifier.height(8.dp))
            }
            items(uiState.recentClients) { client ->
                RecentClientRow(
                    client = client,
                    onClick = { onNavigateToClient(client.id) },
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )
            }
        }

        // Empty state
        if (!uiState.isLoading && uiState.clientCount == 0) {
            item {
                Spacer(Modifier.height(32.dp))
                EmptyState(
                    icon = Icons.Default.PeopleOutline,
                    title = "Welcome to MeetTrack!",
                    subtitle = "Add your first client to get started managing meetings and schedules.",
                    actionLabel = "Add First Client",
                    onAction = onNavigateToAddClient
                )
            }
        }
    }
}

@Composable
private fun DashboardHeader(
    onAddClient: () -> Unit,
    onAddMeeting: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    listOf(
                        MaterialTheme.colorScheme.primary,
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.85f)
                    )
                )
            )
            .padding(horizontal = 20.dp, vertical = 28.dp)
    ) {
        Column {
            Text(
                text = "MeetTrack",
                style = MaterialTheme.typography.titleLarge,
                color = Color.White.copy(alpha = 0.8f),
                fontWeight = FontWeight.Normal
            )
            Text(
                text = "Good ${getGreeting()}!",
                style = MaterialTheme.typography.headlineMedium,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(20.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(
                    onClick = onAddClient,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White,
                        contentColor = MaterialTheme.colorScheme.primary
                    ),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(R.string.add_client))
                }
                Button(
                    onClick = onAddMeeting,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White.copy(alpha = 0.2f),
                        contentColor = Color.White
                    ),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(R.string.schedule))
                }
            }
        }
    }
}

@Composable
private fun RecentClientRow(
    client: Client,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ClientAvatar(name = client.fullName, size = 44)
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(client.fullName, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                if (client.company.isNotBlank()) {
                    Text(client.company, style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            if (client.latitude != null) {
                Icon(
                    Icons.Default.LocationOn,
                    contentDescription = "Has location",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
            }
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun getGreeting(): String {
    val hour = java.time.LocalTime.now().hour
    return when {
        hour < 12 -> "morning"
        hour < 17 -> "afternoon"
        else -> "evening"
    }
}
