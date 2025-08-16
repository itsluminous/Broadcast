package com.fourseason.broadcast.ui.createedit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fourseason.broadcast.data.BroadcastList
import com.fourseason.broadcast.data.BroadcastRepository
import com.fourseason.broadcast.data.Contact
import kotlinx.coroutines.launch

class CreateEditListViewModel(private val repository: BroadcastRepository) : ViewModel() {

    fun saveList(name: String, emoji: String, contacts: List<Contact>) {
        viewModelScope.launch {
            val list = BroadcastList(name = name, iconEmoji = emoji)
            repository.insertBroadcastListWithContacts(list, contacts)
        }
    }

    fun updateList(list: BroadcastList, contacts: List<Contact>) {
        viewModelScope.launch {
            repository.updateBroadcastListWithContacts(list, contacts)
        }
    }
}
