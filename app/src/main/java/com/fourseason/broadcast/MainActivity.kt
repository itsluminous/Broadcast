package com.fourseason.broadcast

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.fourseason.broadcast.data.AppDatabase
import com.fourseason.broadcast.data.BroadcastBackupData
import com.fourseason.broadcast.data.BroadcastList
import com.fourseason.broadcast.data.BroadcastListContactCrossRef
import com.fourseason.broadcast.data.BroadcastListWithContacts
import com.fourseason.broadcast.data.BroadcastRepository
import com.fourseason.broadcast.data.Contact
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.BufferedReader
import java.io.InputStreamReader

class MainActivity : ComponentActivity() {
    private lateinit var repository: BroadcastRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val database = AppDatabase.getDatabase(applicationContext)
        repository = BroadcastRepository(database.broadcastDao())
        val factory = ViewModelFactory(repository, application)

        // Handle incoming share intent
        val sharedText = intent?.getStringExtra(Intent.EXTRA_TEXT)
        val sharedMediaUri = intent?.clipData?.getItemAt(0)?.uri

        setContent {
            BroadcastTheme {
                AppNavigation(
                    factory = factory,
                    sharedText = sharedText,
                    sharedMediaUris = if (sharedMediaUri != null) listOf(sharedMediaUri) else emptyList(),
                    onBackup = { backupBroadcastLists() },
                    onImport = { importBroadcastLists() }
                )
            }
        }
    }

    private fun backupBroadcastLists() {
        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "application/json"
            putExtra(Intent.EXTRA_TITLE, "broadcast_backup.json")
        }
        createDocumentLauncher.launch(intent)
    }

    private fun importBroadcastLists() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "application/json"
        }
        openDocumentLauncher.launch(intent)
    }

    private val createDocumentLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                val scope = (this as? ComponentActivity)?.lifecycleScope ?: return@let
                scope.launch {
                    try {
                        val allLists = repository.getAllBroadcastListsWithContacts()
                        val backupData = BroadcastBackupData(allLists)
                        val jsonString = Json.encodeToString(backupData)

                        contentResolver.openOutputStream(uri)?.use { outputStream ->
                            outputStream.write(jsonString.toByteArray())
                        }
                        withContext(Dispatchers.Main) {
                            Toast.makeText(this@MainActivity, "Backup successful!", Toast.LENGTH_SHORT).show()
                        }
                    } catch (e: Exception) {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(this@MainActivity, "Backup failed: ${e.message}", Toast.LENGTH_LONG).show()
                        }
                        e.printStackTrace()
                    }
                }
            }
        }
    }

    private val openDocumentLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                val scope = (this as? ComponentActivity)?.lifecycleScope ?: return@let
                scope.launch {
                    try {
                        val jsonString = contentResolver.openInputStream(uri)?.use { inputStream ->
                            BufferedReader(InputStreamReader(inputStream)).use { reader ->
                                reader.readText()
                            }
                        }

                        if (jsonString != null) {
                            val backupData = Json.decodeFromString<BroadcastBackupData>(jsonString)
                            // Clear existing data and insert imported data
                            repository.clearAllBroadcastData()
                            backupData.broadcastLists.forEach { listWithContacts ->
                                val listId = repository.insertBroadcastList(listWithContacts.broadcastList)
                                listWithContacts.contacts.forEach { contact ->
                                    repository.insertContact(contact)
                                    repository.insertBroadcastListContactCrossRef(
                                        BroadcastListContactCrossRef(listId, contact.phoneNumber)
                                    )
                                }
                            }
                            withContext(Dispatchers.Main) {
                                Toast.makeText(this@MainActivity, "Import successful!", Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            withContext(Dispatchers.Main) {
                                Toast.makeText(this@MainActivity, "Import failed: Empty file", Toast.LENGTH_LONG).show()
                            }
                        }
                    } catch (e: Exception) {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(this@MainActivity, "Import failed: ${e.message}", Toast.LENGTH_LONG).show()
                        }
                        e.printStackTrace()
                    }
                }
            }
        }
    }
}

@Composable
fun AppNavigation(
    factory: ViewModelFactory,
    sharedText: String? = null,
    sharedMediaUris: List<Uri> = emptyList(),
    onBackup: () -> Unit, // New parameter
    onImport: () -> Unit  // New parameter
) {
    val navController = rememberNavController()
    val mainViewModel: BroadcastListViewModel = viewModel(factory = factory)
    val composeMessageViewModel: ComposeMessageViewModel = viewModel(factory = factory)
    val context = LocalContext.current
    var showSettingsRedirectDialog by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope() // Add coroutine scope

    // Check for Accessibility Service on startup
    LaunchedEffect(Unit) {
        if (!WhatsAppHelper.isAccessibilityServiceEnabled(context)) {
            WhatsAppHelper.requestAccessibilityPermission(context)
        }
    }

    // If shared data is present, navigate to ComposeMessageScreen and populate the ViewModel
    LaunchedEffect(sharedText, sharedMediaUris) {
        if (sharedText != null || sharedMediaUris.isNotEmpty()) {
            composeMessageViewModel.onMessageChange(sharedText ?: "")
            composeMessageViewModel.onMediaSelected(sharedMediaUris)
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
                onComposeMessage = { navController.navigate("compose_message") },
                onBackup = onBackup, // Pass the backup lambda
                onImport = onImport  // Pass the import lambda
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
                onSend = { message, uris ->
                    val phoneNumbers = listWithContacts?.contacts?.map { it.phoneNumber } ?: emptyList()
                    WhatsAppHelper.sendMessage(navController.context, message, uris, phoneNumbers)
                    navController.popBackStack("main", inclusive = false)
                }
            )
        }
        composable("compose_message") {
            var showListPicker by remember { mutableStateOf(false) }
            var messageToSend by remember { mutableStateOf("") }
            var urisToSend by remember { mutableStateOf<List<Uri>>(emptyList()) }

            if (showListPicker) {
                BroadcastListPicker(
                    viewModel = mainViewModel,
                    onDismiss = { showListPicker = false },
                    onConfirm = { lists ->
                        val phoneNumbers = lists.flatMap { it.contacts }.map { it.phoneNumber }.distinct()
                        WhatsAppHelper.sendMessage(navController.context, messageToSend, urisToSend, phoneNumbers)
                        showListPicker = false
                        navController.popBackStack("main", inclusive = false)
                    }
                )
            }

            ComposeMessageScreen(
                viewModel = composeMessageViewModel, // Use the shared ViewModel instance
                onSend = { message, uris ->
                    messageToSend = message
                    urisToSend = uris
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
