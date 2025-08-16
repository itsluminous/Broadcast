package com.fourseason.broadcast.ui.createedit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fourseason.broadcast.data.BroadcastList
import com.fourseason.broadcast.data.BroadcastRepository
import com.fourseason.broadcast.data.Contact
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull // Add this import
import kotlinx.coroutines.launch

class CreateEditListViewModel(private val repository: BroadcastRepository) : ViewModel() {

    private val _listDetails = MutableStateFlow<BroadcastList?>(null)
    val listDetails: StateFlow<BroadcastList?> = _listDetails.asStateFlow()

    fun loadListDetails(listId: Long) {
        viewModelScope.launch {
            val listWithContacts = repository.getBroadcastListWithContacts(listId).firstOrNull() // Use firstOrNull
            _listDetails.value = listWithContacts?.broadcastList
        }
    }

    fun saveList(name: String, emoji: String, contacts: List<Contact>, listId: Long? = null) {
        viewModelScope.launch {
            if (listId == null) {
                val newList = BroadcastList(name = name, iconEmoji = emoji)
                repository.insertBroadcastListWithContacts(newList, contacts)
            } else {
                // Fetch the latest version of the list before updating
                val listToUpdate = repository.getBroadcastListWithContacts(listId).firstOrNull()?.broadcastList
                if (listToUpdate != null) {
                    val updatedList = listToUpdate.copy(name = name, iconEmoji = emoji)
                    repository.updateBroadcastListWithContacts(updatedList, contacts)
                } else {
                    // Handle case where listId is not found, perhaps log an error or inform the user
                }
            }
            _listDetails.value = null // Reset after save
        }
    }
}
