package com.fourseason.broadcast.ui.contactpicker

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.fourseason.broadcast.data.Contact
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState

@OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ContactPickerScreen(
    viewModel: ContactPickerViewModel,
    listIdToEdit: Long?,
    onContactsSelected: (List<Contact>) -> Unit
) {
    val contactsPermissionState = rememberPermissionState(android.Manifest.permission.READ_CONTACTS)
    val allContacts by viewModel.contacts.collectAsState()
    var selectedContacts by remember { mutableStateOf<Set<Contact>>(emptySet()) }
    var searchQuery by remember { mutableStateOf("") }
    val preSelectedContacts by viewModel.selectedContactsForEdit.collectAsState()

    LaunchedEffect(contactsPermissionState.status) {
        if (contactsPermissionState.status.isGranted) {
            if (listIdToEdit != null) {
                viewModel.loadContactsForEdit(listIdToEdit)
            } else {
                viewModel.loadContacts() // Load all contacts if not editing
            }
        } else {
            // Request permission if not granted. If coming for edit, this means permission was revoked.
            contactsPermissionState.launchPermissionRequest()
        }
    }

    // Initialize selectedContacts once preSelectedContacts are loaded for editing
    LaunchedEffect(preSelectedContacts, listIdToEdit) {
        if (listIdToEdit != null && preSelectedContacts.isNotEmpty()) {
            selectedContacts = preSelectedContacts
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text(if (listIdToEdit != null) "Edit Contacts" else "Select Contacts") })
        },
        bottomBar = {
            Button(
                onClick = { onContactsSelected(selectedContacts.toList()) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                enabled = selectedContacts.isNotEmpty()
            ) {
                Text("Done (${selectedContacts.size})")
            }
        }
    ) { paddingValues ->
        Column(modifier = Modifier.padding(paddingValues)) {
            if (contactsPermissionState.status.isGranted) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    label = { Text("Search Contacts") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                )
                LazyColumn(
                    modifier = Modifier.fillMaxSize()
                ) {
                    val filteredContacts = allContacts.filter {
                        it.name.contains(searchQuery, ignoreCase = true) || 
                        it.phoneNumber.contains(searchQuery)
                    }.sortedByDescending { selectedContacts.contains(it) } // Show selected contacts on top

                    items(filteredContacts) { contact ->
                        ContactItem(
                            contact = contact,
                            isSelected = selectedContacts.contains(contact),
                            onToggle = {
                                selectedContacts = if (selectedContacts.contains(contact)) {
                                    selectedContacts - contact
                                } else {
                                    selectedContacts + contact
                                }
                            }
                        )
                    }
                }
            } else {
                // Show a message or a button to request permission if it was denied.
                // The LaunchedEffect above already tries to request it once.
                // You might want to add a UI element here if permission is permanently denied.
                Text("Contacts permission is required to select contacts. Please grant the permission.", modifier = Modifier.padding(16.dp))
            }
        }
    }
}

@Composable
fun ContactItem(
    contact: Contact,
    isSelected: Boolean,
    onToggle: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = contact.name)
            Text(text = contact.phoneNumber)
        }
        Checkbox(checked = isSelected, onCheckedChange = { onToggle() })
    }
}
