package com.fourseason.broadcast.ui.compose

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ComposeMessageScreen(
    viewModel: ComposeMessageViewModel,
    onSend: (String, Uri?) -> Unit
) {
    val message by viewModel.message.collectAsState()
    val mediaUri by viewModel.mediaUri.collectAsState()

    val pickMediaLauncher = rememberLauncherForActivityResult(
        contract = PickVisualMedia()
    ) { uri: Uri? ->
        viewModel.onMediaSelected(uri)
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Compose Message") })
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            OutlinedTextField(
                value = message,
                onValueChange = { viewModel.onMessageChange(it) },
                label = { Text("Your message") },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            )
            Button(onClick = { pickMediaLauncher.launch(PickVisualMediaRequest(PickVisualMedia.ImageAndVideo)) }) {
                Text(if (mediaUri == null) "Add Image/Video" else "Change Image/Video")
            }
            Button(
                onClick = { onSend(message, mediaUri) },
                enabled = message.isNotBlank()
            ) {
                Text("Send")
            }
        }
    }
}
