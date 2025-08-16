package com.fourseason.broadcast.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fourseason.broadcast.data.BroadcastListWithContacts
import com.fourseason.broadcast.data.BroadcastRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class BroadcastListViewModel(private val repository: BroadcastRepository) : ViewModel() {

    val broadcastLists: StateFlow<List<BroadcastListWithContacts>> =
        repository.getAllBroadcastListsWithContacts()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )

    suspend fun getListWithContacts(listId: Long): BroadcastListWithContacts {
        return repository.getBroadcastListWithContacts(listId).first()
    }
}
