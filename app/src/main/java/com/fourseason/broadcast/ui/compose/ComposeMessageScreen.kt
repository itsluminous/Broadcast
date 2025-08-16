package com.fourseason.broadcast.ui.compose

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter

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
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            OutlinedTextField(
                value = message,
                onValueChange = { viewModel.onMessageChange(it) },
                label = { Text("Your message") },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            )
            Spacer(modifier = Modifier.height(8.dp))
            mediaUri?.let { uri ->
                Image(
                    painter = rememberAsyncImagePainter(uri),
                    contentDescription = "Selected media",
                    modifier = Modifier
                        .size(200.dp) // You can adjust the size
                        .padding(vertical = 8.dp),
                    contentScale = ContentScale.Fit
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Button(onClick = { pickMediaLauncher.launch(PickVisualMediaRequest(PickVisualMedia.ImageAndVideo)) }) {
                Text(if (mediaUri == null) "Add Image/Video" else "Change Image/Video")
            }
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = { onSend(message, mediaUri) },
                enabled = message.isNotBlank() || mediaUri != null // Enable if message or media is present
            ) {
                Text("Send")
            }
        }
    }
}
