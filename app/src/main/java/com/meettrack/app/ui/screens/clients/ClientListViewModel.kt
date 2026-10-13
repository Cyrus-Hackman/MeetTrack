package com.meettrack.app.ui.screens.clients

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.meettrack.app.data.model.Client
import com.meettrack.app.data.repository.ClientRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class ClientFilter { ALL, WITH_LOCATION, RECENT }

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
@HiltViewModel
class ClientListViewModel @Inject constructor(
    private val clientRepository: ClientRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _filter = MutableStateFlow(ClientFilter.ALL)
    val filter: StateFlow<ClientFilter> = _filter.asStateFlow()

    val clients: StateFlow<List<Client>> = _searchQuery
        .debounce(300)
        .flatMapLatest { query ->
            if (query.isBlank()) {
                when (_filter.value) {
                    ClientFilter.WITH_LOCATION -> clientRepository.getClientsWithLocation()
                    ClientFilter.RECENT -> clientRepository.getRecentClients(20)
                    else -> clientRepository.getAllClients()
                }
            } else {
                clientRepository.searchClients(query)
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    fun onFilterChange(filter: ClientFilter) {
        _filter.value = filter
        _searchQuery.value = ""
    }

    fun deleteClient(client: Client) {
        viewModelScope.launch {
            clientRepository.deleteClientAndCancelReminders(client.id)
        }
    }
}
