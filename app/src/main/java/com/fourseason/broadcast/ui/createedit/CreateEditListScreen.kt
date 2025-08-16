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
    onSave: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var emoji by remember { mutableStateOf("❤️") }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Create List") })
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
            // A real emoji picker would be more complex. Using a simple text field for now.
            OutlinedTextField(
                value = emoji,
                onValueChange = { emoji = it },
                label = { Text("Emoji Icon") },
                modifier = Modifier.fillMaxWidth()
            )
            Button(
                onClick = {
                    viewModel.saveList(name, emoji, contacts)
                    onSave()
                },
                enabled = name.isNotBlank() && emoji.isNotBlank()
            ) {
                Text("Save")
            }
        }
    }
}
