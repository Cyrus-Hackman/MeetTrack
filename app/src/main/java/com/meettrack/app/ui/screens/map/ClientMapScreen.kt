package com.meettrack.app.ui.screens.map

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import com.meettrack.app.ui.screens.clients.ClientViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClientMapScreen(
    clientId: Long,
    onNavigateBack: () -> Unit,
    viewModel: ClientViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val clientWithMeetings by viewModel.clientWithMeetings.collectAsStateWithLifecycle()

    LaunchedEffect(clientId) {
        viewModel.loadClient(clientId)
    }

    val client = clientWithMeetings?.client
    val lat = client?.latitude
    val lng = client?.longitude

    val cameraPositionState = rememberCameraPositionState {
        if (lat != null && lng != null) {
            position = CameraPosition.fromLatLngZoom(LatLng(lat, lng), 16f)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(client?.fullName ?: "Map", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (lat != null && lng != null) {
                        IconButton(onClick = {
                            val uri = Uri.parse("geo:$lat,$lng?q=$lat,$lng(${client?.fullName})")
                            context.startActivity(Intent(Intent.ACTION_VIEW, uri))
                        }) {
                            Icon(Icons.Default.Navigation, contentDescription = "Open in Maps")
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        if (lat == null || lng == null) {
            Box(
                Modifier.fillMaxSize().padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.LocationOff,
                        contentDescription = null,
                        modifier = Modifier.size(72.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(
                        "No location saved",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        "Edit the client to capture their GPS location",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
                GoogleMap(
                    modifier = Modifier.fillMaxSize(),
                    cameraPositionState = cameraPositionState,
                    properties = MapProperties(
                        isMyLocationEnabled = false,
                        mapType = MapType.NORMAL
                    ),
                    uiSettings = MapUiSettings(
                        zoomControlsEnabled = true,
                        myLocationButtonEnabled = false
                    )
                ) {
                    Marker(
                        state = MarkerState(position = LatLng(lat, lng)),
                        title = client?.fullName ?: "Client",
                        snippet = client?.formattedAddress?.ifBlank { null }
                            ?: "%.5f, %.5f".format(lat, lng)
                    )
                }

                // Info card at bottom
                client?.let { c ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.BottomCenter)
                            .padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.LocationOn,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    c.fullName,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    c.formattedAddress.ifBlank { "%.5f, %.5f".format(lat, lng) },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            IconButton(onClick = {
                                val uri = Uri.parse("geo:$lat,$lng?q=$lat,$lng(${c.fullName})")
                                context.startActivity(Intent(Intent.ACTION_VIEW, uri))
                            }) {
                                Icon(Icons.Default.Navigation, contentDescription = "Navigate",
                                    tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
            }
        }
    }
}
