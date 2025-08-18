package com.fourseason.broadcast.ui.main

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.rememberDrawerState
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import com.fourseason.broadcast.data.BroadcastListWithContacts
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun MainScreen(
    viewModel: BroadcastListViewModel,
    onSelectList: (Long) -> Unit,
    onCreateList: () -> Unit,
    onComposeMessage: () -> Unit,
    onBackup: () -> Unit, // New parameter for backup action
    onImport: () -> Unit  // New parameter for import action
) {
    val lists by viewModel.broadcastLists.collectAsState()
    var showDialog by remember { mutableStateOf<BroadcastListWithContacts?>(null) }
    val haptic = LocalHapticFeedback.current
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Spacer(Modifier.padding(top = 16.dp)) // Top padding for the drawer content
                Text("Broadcast App", modifier = Modifier.padding(16.dp), style = MaterialTheme.typography.headlineSmall)
                Spacer(Modifier.padding(bottom = 16.dp))

                NavigationDrawerItem(
                    label = { Text("Backup Broadcast Lists") },
                    selected = false,
                    onClick = {
                        scope.launch { drawerState.close() }
                        onBackup()
                    },
                    modifier = Modifier.padding(16.dp)
                )
                NavigationDrawerItem(
                    label = { Text("Import Broadcast Lists") },
                    selected = false,
                    onClick = {
                        scope.launch { drawerState.close() }
                        onImport()
                    },
                    modifier = Modifier.padding(16.dp)
                )
            }
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Broadcast Lists") },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Filled.Menu, contentDescription = "Menu")
                        }
                    }
                )
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
                    BroadcastListItem(
                        list = list,
                        onClick = { onSelectList(list.broadcastList.id) },
                        onLongClick = {
                            haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                            showDialog = list
                        }
                    )
                }
            }

            showDialog?.let { listToDelete ->
                AlertDialog(
                    onDismissRequest = { showDialog = null },
                    title = { Text("Delete List") },
                    text = { Text("Are you sure you want to delete \"${listToDelete.broadcastList.name}\"?") },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                viewModel.deleteList(listToDelete.broadcastList.id) // Ensure this method exists in your ViewModel
                                showDialog = null
                            }
                        ) {
                            Text("Confirm")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showDialog = null }) {
                            Text("Cancel")
                        }
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun BroadcastListItem(
    list: BroadcastListWithContacts,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
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
