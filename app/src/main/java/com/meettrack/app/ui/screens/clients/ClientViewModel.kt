package com.meettrack.app.ui.screens.clients

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.meettrack.app.data.model.Client
import com.meettrack.app.data.model.ClientWithMeetings
import com.meettrack.app.data.repository.ClientRepository
import com.meettrack.app.location.LocationService
import com.meettrack.app.location.LocationResult2
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import javax.inject.Inject

sealed class LocationState {
    object Idle : LocationState()
    object Loading : LocationState()
    data class Success(val result: LocationResult2) : LocationState()
    data class Error(val message: String) : LocationState()
}

data class ClientFormState(
    val fullName: String = "",
    val phoneNumber: String = "",
    val email: String = "",
    val company: String = "",
    val address: String = "",
    val notes: String = "",
    val latitude: Double? = null,
    val longitude: Double? = null,
    val formattedAddress: String = "",
    val locationLastUpdated: LocalDateTime? = null,
    val isLoading: Boolean = false,
    val isSaved: Boolean = false,
    val error: String? = null,
    val locationState: LocationState = LocationState.Idle
)

@HiltViewModel
class ClientViewModel @Inject constructor(
    private val clientRepository: ClientRepository,
    private val locationService: LocationService,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _formState = MutableStateFlow(ClientFormState())
    val formState: StateFlow<ClientFormState> = _formState.asStateFlow()

    private val _clientId = MutableStateFlow<Long?>(null)

    val clientWithMeetings: StateFlow<ClientWithMeetings?> = _clientId
        .flatMapLatest { id ->
            if (id != null) clientRepository.getClientWithMeetings(id)
            else flowOf(null)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun loadClient(clientId: Long) {
        _clientId.value = clientId
        viewModelScope.launch {
            clientRepository.getClientById(clientId).collect { client ->
                client?.let { populateForm(it) }
            }
        }
    }

    private fun populateForm(client: Client) {
        _formState.value = ClientFormState(
            fullName = client.fullName,
            phoneNumber = client.phoneNumber,
            email = client.email,
            company = client.company,
            address = client.address,
            notes = client.notes,
            latitude = client.latitude,
            longitude = client.longitude,
            formattedAddress = client.formattedAddress,
            locationLastUpdated = client.locationLastUpdated
        )
    }

    fun updateFullName(value: String) { _formState.value = _formState.value.copy(fullName = value) }
    fun updatePhoneNumber(value: String) { _formState.value = _formState.value.copy(phoneNumber = value) }
    fun updateEmail(value: String) { _formState.value = _formState.value.copy(email = value) }
    fun updateCompany(value: String) { _formState.value = _formState.value.copy(company = value) }
    fun updateAddress(value: String) { _formState.value = _formState.value.copy(address = value) }
    fun updateNotes(value: String) { _formState.value = _formState.value.copy(notes = value) }

    fun captureCurrentLocation() {
        viewModelScope.launch {
            _formState.value = _formState.value.copy(locationState = LocationState.Loading)
            try {
                val location = locationService.getCurrentLocation()
                _formState.value = _formState.value.copy(
                    latitude = location.latitude,
                    longitude = location.longitude,
                    formattedAddress = location.formattedAddress,
                    locationState = LocationState.Success(location),
                    locationLastUpdated = LocalDateTime.now()
                )
            } catch (e: Exception) {
                _formState.value = _formState.value.copy(
                    locationState = LocationState.Error(e.message ?: "Location unavailable")
                )
            }
        }
    }

    fun saveNewClient(onSuccess: (Long) -> Unit) {
        val state = _formState.value
        if (state.fullName.isBlank()) {
            _formState.value = state.copy(error = "Client name is required")
            return
        }
        if (state.phoneNumber.isBlank()) {
            _formState.value = state.copy(error = "Phone number is required")
            return
        }
        viewModelScope.launch {
            _formState.value = state.copy(isLoading = true, error = null)
            try {
                val client = Client(
                    fullName = state.fullName.trim(),
                    phoneNumber = state.phoneNumber.trim(),
                    email = state.email.trim(),
                    company = state.company.trim(),
                    address = state.address.trim(),
                    notes = state.notes.trim(),
                    latitude = state.latitude,
                    longitude = state.longitude,
                    formattedAddress = state.formattedAddress,
                    locationLastUpdated = state.locationLastUpdated
                )
                val id = clientRepository.addClient(client)
                _formState.value = _formState.value.copy(isLoading = false, isSaved = true)
                onSuccess(id)
            } catch (e: Exception) {
                _formState.value = _formState.value.copy(
                    isLoading = false,
                    error = "Failed to save client: ${e.message}"
                )
            }
        }
    }

    fun updateExistingClient(clientId: Long, onSuccess: () -> Unit) {
        val state = _formState.value
        if (state.fullName.isBlank()) {
            _formState.value = state.copy(error = "Client name is required")
            return
        }
        viewModelScope.launch {
            _formState.value = state.copy(isLoading = true, error = null)
            try {
                val client = Client(
                    id = clientId,
                    fullName = state.fullName.trim(),
                    phoneNumber = state.phoneNumber.trim(),
                    email = state.email.trim(),
                    company = state.company.trim(),
                    address = state.address.trim(),
                    notes = state.notes.trim(),
                    latitude = state.latitude,
                    longitude = state.longitude,
                    formattedAddress = state.formattedAddress,
                    locationLastUpdated = state.locationLastUpdated
                )
                clientRepository.updateClient(client)
                _formState.value = _formState.value.copy(isLoading = false, isSaved = true)
                onSuccess()
            } catch (e: Exception) {
                _formState.value = _formState.value.copy(
                    isLoading = false,
                    error = "Failed to update client: ${e.message}"
                )
            }
        }
    }

    fun deleteClient(clientId: Long, onSuccess: () -> Unit) {
        viewModelScope.launch {
            clientRepository.deleteClientById(clientId)
            onSuccess()
        }
    }

    fun clearError() { _formState.value = _formState.value.copy(error = null) }
}
