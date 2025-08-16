package com.fourseason.broadcast.ui.main

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.fourseason.broadcast.data.BroadcastListWithContacts

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: BroadcastListViewModel,
    onSelectList: (Long) -> Unit,
    onCreateList: () -> Unit,
    onComposeMessage: () -> Unit
) {
    val lists by viewModel.broadcastLists.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Broadcast Lists") })
        },
        floatingActionButton = {
            Column {
                FloatingActionButton(onClick = onCreateList) {
                    Icon(Icons.Default.Add, contentDescription = "Create List")
                }
                Spacer(modifier = Modifier.padding(8.dp))
                FloatingActionButton(onClick = onComposeMessage) {
                    Icon(Icons.Default.Send, contentDescription = "Compose Message")
                }
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(lists) { list ->
                BroadcastListItem(list = list, onClick = { onSelectList(list.broadcastList.id) })
            }
        }
    }
}

@Composable
fun BroadcastListItem(
    list: BroadcastListWithContacts,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = list.broadcastList.iconEmoji, style = MaterialTheme.typography.headlineMedium)
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(text = list.broadcastList.name, style = MaterialTheme.typography.bodyLarge)
                Text(text = "${list.contacts.size} contacts", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
