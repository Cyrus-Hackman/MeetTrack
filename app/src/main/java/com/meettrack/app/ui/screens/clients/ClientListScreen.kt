package com.meettrack.app.ui.screens.clients

import androidx.compose.animation.*
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.meettrack.app.R
import com.meettrack.app.data.model.Client
import com.meettrack.app.ui.components.ClientAvatar
import com.meettrack.app.ui.components.EmptyState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClientListScreen(
    paddingValues: PaddingValues,
    onNavigateToClient: (Long) -> Unit,
    onNavigateToAddClient: () -> Unit,
    viewModel: ClientListViewModel = hiltViewModel()
) {
    val clients by viewModel.clients.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val filter by viewModel.filter.collectAsStateWithLifecycle()
    var clientToDelete by remember { mutableStateOf<Client?>(null) }

    Scaffold(
        modifier = Modifier.padding(paddingValues),
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        Text(stringResource(R.string.clients), fontWeight = FontWeight.Bold)
                    },
                    actions = {
                        IconButton(onClick = onNavigateToAddClient) {
                            Icon(Icons.Default.PersonAdd, contentDescription = "Add Client")
                        }
                    }
                )
                // Search bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = viewModel::onSearchQueryChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    placeholder = { Text(stringResource(R.string.search_clients_hint)) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        if (searchQuery.isNotBlank()) {
                            IconButton(onClick = { viewModel.onSearchQueryChange("") }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear")
                            }
                        }
                    },
                    shape = RoundedCornerShape(28.dp),
                    singleLine = true
                )
                // Filter chips
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        FilterChip(
                            selected = filter == ClientFilter.ALL,
                            onClick = { viewModel.onFilterChange(ClientFilter.ALL) },
                            label = { Text(stringResource(R.string.all)) },
                            leadingIcon = if (filter == ClientFilter.ALL) {
                                { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                            } else null
                        )
                    }
                    item {
                        FilterChip(
                            selected = filter == ClientFilter.WITH_LOCATION,
                            onClick = { viewModel.onFilterChange(ClientFilter.WITH_LOCATION) },
                            label = { Text(stringResource(R.string.with_location)) },
                            leadingIcon = if (filter == ClientFilter.WITH_LOCATION) {
                                { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                            } else null
                        )
                    }
                    item {
                        FilterChip(
                            selected = filter == ClientFilter.RECENT,
                            onClick = { viewModel.onFilterChange(ClientFilter.RECENT) },
                            label = { Text(stringResource(R.string.recently_added)) },
                            leadingIcon = if (filter == ClientFilter.RECENT) {
                                { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                            } else null
                        )
                    }
                }
                HorizontalDivider()
            }
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onNavigateToAddClient,
                icon = { Icon(Icons.Default.PersonAdd, contentDescription = null) },
                text = { Text(stringResource(R.string.new_client)) }
            )
        }
    ) { innerPadding ->
        if (clients.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                EmptyState(
                    icon = Icons.Default.PeopleOutline,
                    title = if (searchQuery.isNotBlank()) "No clients found" else "No clients yet",
                    subtitle = if (searchQuery.isNotBlank())
                        "Try a different search term"
                    else
                        "Add your first client to get started",
                    actionLabel = if (searchQuery.isBlank()) "Add Client" else null,
                    onAction = if (searchQuery.isBlank()) onNavigateToAddClient else null
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    Text(
                        "${clients.size} client${if (clients.size != 1) "s" else ""}",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                }
                items(clients, key = { it.id }) { client ->
                    ClientListItem(
                        client = client,
                        onClick = { onNavigateToClient(client.id) },
                        onDelete = { clientToDelete = client }
                    )
                }
                item { Spacer(Modifier.height(80.dp)) }
            }
        }
    }

    // Delete confirmation dialog
    clientToDelete?.let { client ->
        AlertDialog(
            onDismissRequest = { clientToDelete = null },
            icon = { Icon(Icons.Default.Warning, contentDescription = null) },
            title = { Text(stringResource(R.string.delete_client)) },
            text = {
                Text("Are you sure you want to delete ${client.fullName}? This will also remove all their meeting records.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteClient(client)
                        clientToDelete = null
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) { Text(stringResource(R.string.delete)) }
            },
            dismissButton = {
                TextButton(onClick = { clientToDelete = null }) { Text(stringResource(R.string.cancel)) }
            }
        )
    }
}

@Composable
private fun ClientListItem(
    client: Client,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ClientAvatar(name = client.fullName, size = 48)
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    client.fullName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                if (client.company.isNotBlank()) {
                    Text(
                        client.company,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Phone,
                        contentDescription = null,
                        modifier = Modifier.size(12.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        client.phoneNumber,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (client.latitude != null) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.LocationOn,
                            contentDescription = null,
                            modifier = Modifier.size(12.dp),
                            tint = MaterialTheme.colorScheme.tertiary
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            client.formattedAddress.ifBlank { "Location saved" },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.tertiary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = "Options")
                }
                DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.delete)) },
                        onClick = { showMenu = false; onDelete() },
                        leadingIcon = {
                            Icon(Icons.Default.Delete, contentDescription = null,
                                tint = MaterialTheme.colorScheme.error)
                        }
                    )
                }
            }
        }
    }
}
