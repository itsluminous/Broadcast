package com.fourseason.broadcast.ui.createedit

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.fourseason.broadcast.data.Contact

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateEditListScreen(
    viewModel: CreateEditListViewModel,
    contacts: List<Contact>,
    listIdToEdit: Long?,
    onSave: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    val emoji = "📢" // Default emoji

    val listDetails by viewModel.listDetails.collectAsState()

    LaunchedEffect(listIdToEdit) {
        if (listIdToEdit != null) {
            viewModel.loadListDetails(listIdToEdit)
        }
    }

    LaunchedEffect(listDetails?.name) {
        listDetails?.let {
            if (it.id == listIdToEdit) { // Ensure details loaded are for the current listIdToEdit
                name = it.name
                // emoji is now fixed, no need to update from listDetails
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text(if (listIdToEdit != null) "Edit List" else "Create List") })
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("List Name") },
                modifier = Modifier.fillMaxWidth()
            )
            Button(
                onClick = {
                    viewModel.saveList(name, emoji, contacts, listIdToEdit)
                    onSave()
                },
                enabled = name.isNotBlank() && contacts.isNotEmpty()
            ) {
                Text(if (listIdToEdit != null) "Save Changes" else "Create List")
            }
        }
    }
}
