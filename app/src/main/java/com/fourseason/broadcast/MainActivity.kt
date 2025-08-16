package com.fourseason.broadcast

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.fourseason.broadcast.data.AppDatabase
import com.fourseason.broadcast.data.BroadcastListWithContacts
import com.fourseason.broadcast.data.BroadcastRepository
import com.fourseason.broadcast.ui.TemporaryDataHolder
import com.fourseason.broadcast.ui.ViewModelFactory
import com.fourseason.broadcast.ui.compose.ComposeMessageScreen
import com.fourseason.broadcast.ui.contactpicker.ContactPickerScreen
import com.fourseason.broadcast.ui.createedit.CreateEditListScreen
import com.fourseason.broadcast.ui.main.BroadcastListViewModel
import com.fourseason.broadcast.ui.main.MainScreen
import com.fourseason.broadcast.ui.theme.BroadcastTheme
import com.fourseason.broadcast.util.WhatsAppHelper
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val database = AppDatabase.getDatabase(applicationContext)
        val repository = BroadcastRepository(database.broadcastDao())
        val factory = ViewModelFactory(repository, application)

        setContent {
            BroadcastTheme {
                AppNavigation(factory = factory)
            }
        }
    }
}

@Composable
fun AppNavigation(factory: ViewModelFactory) {
    val navController = rememberNavController()
    val coroutineScope = rememberCoroutineScope()
    val mainViewModel: BroadcastListViewModel = viewModel(factory = factory)

    NavHost(navController = navController, startDestination = "main") {
        composable("main") {
            var showDialog by remember { mutableStateOf<Long?>(null) }

            if (showDialog != null) {
                ListOptionsDialog(
                    onDismiss = { showDialog = null },
                    onEdit = { /* TODO */ },
                    onSend = {
                        navController.navigate("compose_message/$showDialog")
                    }
                )
            }

            MainScreen(
                viewModel = mainViewModel,
                onSelectList = { listId -> showDialog = listId },
                onCreateList = { navController.navigate("contact_picker") },
                onComposeMessage = { navController.navigate("compose_message") }
            )
        }
        composable("contact_picker") {
            ContactPickerScreen(
                viewModel = viewModel(factory = factory),
                onContactsSelected = { contacts ->
                    TemporaryDataHolder.selectedContacts = contacts
                    navController.navigate("create_edit_list")
                }
            )
        }
        composable("create_edit_list") {
            CreateEditListScreen(
                viewModel = viewModel(factory = factory),
                contacts = TemporaryDataHolder.selectedContacts,
                onSave = { navController.popBackStack("main", inclusive = false) }
            )
        }
        composable(
            "compose_message/{listId}",
            arguments = listOf(navArgument("listId") { type = NavType.LongType })
        ) { backStackEntry ->
            val listId = backStackEntry.arguments?.getLong("listId")
            var listWithContacts by remember { mutableStateOf<BroadcastListWithContacts?>(null) }

            LaunchedEffect(listId) {
                if (listId != null) {
                    listWithContacts = mainViewModel.getListWithContacts(listId)
                }
            }

            ComposeMessageScreen(
                viewModel = viewModel(factory = factory),
                onSend = { message, uri ->
                    val phoneNumbers = listWithContacts?.contacts?.map { it.phoneNumber } ?: emptyList()
                    WhatsAppHelper.sendMessage(navController.context, message, uri, phoneNumbers)
                    navController.popBackStack("main", inclusive = false)
                }
            )
        }
        composable("compose_message") {
            var showListPicker by remember { mutableStateOf(false) }
            var messageToSend by remember { mutableStateOf("") }
            var uriToSend by remember { mutableStateOf<Uri?>(null) }

            if (showListPicker) {
                BroadcastListPicker(
                    viewModel = mainViewModel,
                    onDismiss = { showListPicker = false },
                    onConfirm = { lists ->
                        val phoneNumbers = lists.flatMap { it.contacts }.map { it.phoneNumber }.distinct()
                        WhatsAppHelper.sendMessage(navController.context, messageToSend, uriToSend, phoneNumbers)
                        showListPicker = false
                        navController.popBackStack("main", inclusive = false)
                    }
                )
            }

            ComposeMessageScreen(
                viewModel = viewModel(factory = factory),
                onSend = { message, uri ->
                    messageToSend = message
                    uriToSend = uri
                    showListPicker = true
                }
            )
        }
    }
}

@Composable
fun ListOptionsDialog(
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
    onSend: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Options") },
        text = { Text("What would you like to do?") },
        confirmButton = {
            TextButton(onClick = { onSend(); onDismiss() }) {
                Text("Send Message")
            }
        },
        dismissButton = {
            TextButton(onClick = { onEdit(); onDismiss() }) {
                Text("Edit")
            }
        }
    )
}

@Composable
fun BroadcastListPicker(
    viewModel: BroadcastListViewModel,
    onDismiss: () -> Unit,
    onConfirm: (List<BroadcastListWithContacts>) -> Unit
) {
    val lists by viewModel.broadcastLists.collectAsState()
    var selectedLists by remember { mutableStateOf<Set<BroadcastListWithContacts>>(emptySet()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select Lists") },
        text = {
            LazyColumn {
                items(lists) { list ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                selectedLists = if (selectedLists.contains(list)) {
                                    selectedLists - list
                                } else {
                                    selectedLists + list
                                }
                            }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = selectedLists.contains(list),
                            onCheckedChange = null
                        )
                        Text(text = list.broadcastList.name, modifier = Modifier.padding(start = 8.dp))
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(selectedLists.toList()) }) {
                Text("Confirm")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
