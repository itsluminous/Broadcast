package com.fourseason.broadcast.ui.contactpicker

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
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
    onContactsSelected: (List<Contact>) -> Unit
) {
    val contactsPermissionState = rememberPermissionState(android.Manifest.permission.READ_CONTACTS)
    val contacts by viewModel.contacts.collectAsState()
    var selectedContacts by remember { mutableStateOf<Set<Contact>>(emptySet()) }

    LaunchedEffect(contactsPermissionState.status) {
        if (contactsPermissionState.status.isGranted) {
            viewModel.loadContacts()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Select Contacts") })
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
        if (contactsPermissionState.status.isGranted) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                items(contacts) { contact ->
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
            Button(onClick = { contactsPermissionState.launchPermissionRequest() }) {
                Text("Request Contacts Permission")
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
        Text(text = contact.name, modifier = Modifier.weight(1f))
        Checkbox(checked = isSelected, onCheckedChange = { onToggle() })
    }
}
