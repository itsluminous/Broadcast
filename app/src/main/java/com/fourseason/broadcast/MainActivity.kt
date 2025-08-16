package com.fourseason.broadcast

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
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
import com.fourseason.broadcast.ui.compose.ComposeMessageViewModel
import com.fourseason.broadcast.ui.contactpicker.ContactPickerScreen
import com.fourseason.broadcast.ui.createedit.CreateEditListScreen
import com.fourseason.broadcast.ui.main.BroadcastListViewModel
import com.fourseason.broadcast.ui.main.MainScreen
import com.fourseason.broadcast.ui.theme.BroadcastTheme
import com.fourseason.broadcast.util.WhatsAppHelper

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val database = AppDatabase.getDatabase(applicationContext)
        val repository = BroadcastRepository(database.broadcastDao())
        val factory = ViewModelFactory(repository, application)

        // Handle incoming share intent
        val sharedText = intent?.getStringExtra(Intent.EXTRA_TEXT)
        val sharedMediaUri = intent?.clipData?.getItemAt(0)?.uri

        setContent {
            BroadcastTheme {
                AppNavigation(factory = factory, sharedText = sharedText, sharedMediaUri = sharedMediaUri)
            }
        }
    }
}

@Composable
fun AppNavigation(
    factory: ViewModelFactory,
    sharedText: String? = null,
    sharedMediaUri: Uri? = null
) {
    val navController = rememberNavController()
    val mainViewModel: BroadcastListViewModel = viewModel(factory = factory)
    val composeMessageViewModel: ComposeMessageViewModel = viewModel(factory = factory)
    val context = LocalContext.current
    var showSettingsRedirectDialog by remember { mutableStateOf(false) }

    // Check for Accessibility Service on startup
    LaunchedEffect(Unit) {
        if (!WhatsAppHelper.isAccessibilityServiceEnabled(context)) {
            WhatsAppHelper.requestAccessibilityPermission(context)
        }
    }

    // If shared data is present, navigate to ComposeMessageScreen and populate the ViewModel
    LaunchedEffect(sharedText, sharedMediaUri) {
        if (sharedText != null || sharedMediaUri != null) {
            composeMessageViewModel.onMessageChange(sharedText ?: "")
            composeMessageViewModel.onMediaSelected(sharedMediaUri)
            navController.navigate("compose_message") // Navigate to the general compose_message route
        }
    }

    val requestPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            navController.navigate("contact_picker")
        } else {
            val activity = context as? Activity
            if (activity != null && !ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.READ_CONTACTS)) {
                showSettingsRedirectDialog = true
            } else {
                // User denied the permission
            }
        }
    }

    if (showSettingsRedirectDialog) {
        SettingsRedirectDialog(
            onDismiss = { showSettingsRedirectDialog = false },
            onConfirm = {
                showSettingsRedirectDialog = false
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                val uri = Uri.fromParts("package", context.packageName, null)
                intent.data = uri
                context.startActivity(intent)
            }
        )
    }

    NavHost(navController = navController, startDestination = "main") {
        composable("main") {
            var showListOptionsDialog by remember { mutableStateOf<Long?>(null) }

            if (showListOptionsDialog != null) {
                ListOptionsDialog(
                    onDismiss = { showListOptionsDialog = null },
                    onEdit = {
                        navController.navigate("contact_picker?listId=$showListOptionsDialog")
                    },
                    onSend = {
                        navController.navigate("compose_message/$showListOptionsDialog")
                    }
                )
            }

            MainScreen(
                viewModel = mainViewModel,
                onSelectList = { listId -> showListOptionsDialog = listId },
                onCreateList = {
                    when (ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.READ_CONTACTS
                    )) {
                        PackageManager.PERMISSION_GRANTED -> {
                            navController.navigate("contact_picker")
                        }
                        else -> {
                            requestPermissionLauncher.launch(Manifest.permission.READ_CONTACTS)
                        }
                    }
                },
                onComposeMessage = { navController.navigate("compose_message") }
            )
        }
        composable(
            route = "contact_picker?listId={listId}",
            arguments = listOf(navArgument("listId") {
                type = NavType.LongType
                defaultValue = -1L
            })
        ) { backStackEntry ->
            val listId = backStackEntry.arguments?.getLong("listId")
            ContactPickerScreen(
                viewModel = viewModel(factory = factory),
                listIdToEdit = if (listId == -1L) null else listId,
                onContactsSelected = { contacts ->
                    TemporaryDataHolder.selectedContacts = contacts
                    TemporaryDataHolder.editingListId = if (listId == -1L) null else listId
                    val route = if (listId != null && listId != -1L) {
                        "create_edit_list?listId=$listId"
                    } else {
                        "create_edit_list"
                    }
                    navController.navigate(route)
                }
            )
        }
        composable(
            route = "create_edit_list?listId={listId}",
            arguments = listOf(navArgument("listId") {
                type = NavType.LongType
                defaultValue = -1L
            })
        ) { backStackEntry ->
            val listId = backStackEntry.arguments?.getLong("listId")
            CreateEditListScreen(
                viewModel = viewModel(factory = factory),
                contacts = TemporaryDataHolder.selectedContacts,
                listIdToEdit = if (listId == -1L) null else listId,
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
                viewModel = composeMessageViewModel, // Use the shared ViewModel instance
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
                viewModel = composeMessageViewModel, // Use the shared ViewModel instance
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
fun SettingsRedirectDialog(onDismiss: () -> Unit, onConfirm: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Permission Required") },
        text = { Text("The app needs access to your contacts to create broadcast lists. Since the permission was previously denied with 'Don\'t ask again', you need to enable it manually in the app settings.") },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("Open Settings")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
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
