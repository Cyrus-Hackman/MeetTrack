package com.meettrack.app.ui.screens.clients

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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.meettrack.app.R
import com.google.accompanist.permissions.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun AddClientScreen(
    onNavigateBack: () -> Unit,
    onClientAdded: (Long) -> Unit,
    viewModel: ClientViewModel = hiltViewModel()
) {
    ClientFormScreen(
        title = "New Client",
        submitLabel = "Add Client",
        onNavigateBack = onNavigateBack,
        viewModel = viewModel,
        onSubmit = { viewModel.saveNewClient(onClientAdded) }
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun EditClientScreen(
    clientId: Long,
    onNavigateBack: () -> Unit,
    viewModel: ClientViewModel = hiltViewModel()
) {
    LaunchedEffect(clientId) {
        viewModel.loadClient(clientId)
    }

    ClientFormScreen(
        title = "Edit Client",
        submitLabel = "Save Changes",
        onNavigateBack = onNavigateBack,
        viewModel = viewModel,
        onSubmit = { viewModel.updateExistingClient(clientId, onNavigateBack) }
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
private fun ClientFormScreen(
    title: String,
    submitLabel: String,
    onNavigateBack: () -> Unit,
    viewModel: ClientViewModel,
    onSubmit: () -> Unit
) {
    val formState by viewModel.formState.collectAsStateWithLifecycle()
    val scrollState = rememberScrollState()

    val locationPermissionsState = rememberMultiplePermissionsState(
        listOf(
            android.Manifest.permission.ACCESS_FINE_LOCATION,
            android.Manifest.permission.ACCESS_COARSE_LOCATION
        )
    )

    // Show error snackbar
    val snackbarHostState = remember { SnackbarHostState() }
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
                    if (formState.isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp).padding(end = 16.dp)
                        )
                    } else {
                        TextButton(onClick = onSubmit) {
                            Text(submitLabel, fontWeight = FontWeight.SemiBold)
                        }
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
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Required fields
            FormSection("Basic Information") {
                OutlinedTextField(
                    value = formState.fullName,
                    onValueChange = viewModel::updateFullName,
                    label = { Text(stringResource(R.string.full_name_required)) },
                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    isError = formState.error?.contains("name", ignoreCase = true) == true
                )
                OutlinedTextField(
                    value = formState.phoneNumber,
                    onValueChange = viewModel::updatePhoneNumber,
                    label = { Text(stringResource(R.string.phone_number_required)) },
                    leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Phone
                    )
                )
            }

            FormSection("Optional Details") {
                OutlinedTextField(
                    value = formState.email,
                    onValueChange = viewModel::updateEmail,
                    label = { Text(stringResource(R.string.email_address)) },
                    leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Email
                    )
                )
                OutlinedTextField(
                    value = formState.company,
                    onValueChange = viewModel::updateCompany,
                    label = { Text(stringResource(R.string.company_organization)) },
                    leadingIcon = { Icon(Icons.Default.Business, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = formState.address,
                    onValueChange = viewModel::updateAddress,
                    label = { Text(stringResource(R.string.address_location_description)) },
                    leadingIcon = { Icon(Icons.Default.LocationOn, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3
                )
            }

            FormSection("GPS Location") {
                LocationSection(
                    formState = formState,
                    locationPermissionsState = locationPermissionsState,
                    onCaptureLocation = {
                        if (locationPermissionsState.allPermissionsGranted) {
                            viewModel.captureCurrentLocation()
                        } else {
                            locationPermissionsState.launchMultiplePermissionRequest()
                        }
                    }
                )
            }

            FormSection("Notes") {
                OutlinedTextField(
                    value = formState.notes,
                    onValueChange = viewModel::updateNotes,
                    label = { Text(stringResource(R.string.client_notes)) },
                    leadingIcon = { Icon(Icons.Default.Notes, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth().height(120.dp)
                )
            }

            Spacer(Modifier.height(16.dp))
            Button(
                onClick = onSubmit,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                enabled = !formState.isLoading
            ) {
                if (formState.isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = MaterialTheme.colorScheme.onPrimary)
                } else {
                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(submitLabel, style = MaterialTheme.typography.titleSmall)
                }
            }
        }
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
private fun LocationSection(
    formState: ClientFormState,
    locationPermissionsState: MultiplePermissionsState,
    onCaptureLocation: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (formState.latitude != null && formState.longitude != null) {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.LocationOn, contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Location Captured",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            formState.formattedAddress.ifBlank {
                                "%.6f, %.6f".format(formState.latitude, formState.longitude)
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        formState.locationLastUpdated?.let {
                            Text(
                                "Updated: ${it.format(java.time.format.DateTimeFormatter.ofPattern("MMM d, h:mm a"))}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
            }
        }

        when (val locState = formState.locationState) {
            is LocationState.Loading -> {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.getting_gps_location), style = MaterialTheme.typography.bodyMedium)
                }
            }
            is LocationState.Error -> {
                Text(
                    "⚠ ${locState.message}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
            else -> {}
        }

        val buttonLabel = if (formState.latitude != null) "Update Location" else "Capture Current Location"
        Button(
            onClick = onCaptureLocation,
            modifier = Modifier.fillMaxWidth(),
            enabled = formState.locationState !is LocationState.Loading
        ) {
            Icon(Icons.Default.MyLocation, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(buttonLabel)
        }

        if (!locationPermissionsState.allPermissionsGranted) {
            Text(
                "Location permission required to capture GPS coordinates",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun FormSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            title,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(top = 4.dp)
        )
        content()
    }
}
