package com.fourseason.broadcast.ui.contactpicker

import android.app.Application
import android.provider.ContactsContract
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.fourseason.broadcast.data.BroadcastRepository
import com.fourseason.broadcast.data.Contact
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ContactPickerViewModel(application: Application, private val repository: BroadcastRepository) : AndroidViewModel(application) {

    private val _contacts = MutableStateFlow<List<Contact>>(emptyList())
    val contacts: StateFlow<List<Contact>> = _contacts.asStateFlow()

    private val _selectedContactsForEdit = MutableStateFlow<Set<Contact>>(emptySet())
    val selectedContactsForEdit: StateFlow<Set<Contact>> = _selectedContactsForEdit.asStateFlow()

    fun loadContacts() {
        viewModelScope.launch {
            val contactList = mutableListOf<Contact>()
            val contentResolver = getApplication<Application>().contentResolver
            val cursor = contentResolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                null,
                null,
                null,
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " ASC"
            )

            cursor?.use {
                val nameIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                val numberIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)

                while (it.moveToNext()) {
                    val name = it.getString(nameIndex)
                    val number = it.getString(numberIndex)
                    // Ensure that we have valid indices before trying to access them.
                    if (nameIndex != -1 && numberIndex != -1) {
                        contactList.add(Contact(phoneNumber = number, name = name))
                    }
                }
            }
            _contacts.value = contactList.distinctBy { it.phoneNumber } // Ensure all contacts are loaded
        }
    }

    fun loadContactsForEdit(listId: Long) {
        viewModelScope.launch {
            loadContacts() // Load all contacts first
            repository.getBroadcastListWithContacts(listId).collect { listWithContacts -> // Collect the flow
                _selectedContactsForEdit.value = listWithContacts.contacts.toSet()
            }
        }
    }
}
