package com.fourseason.broadcast.ui.compose

import android.net.Uri
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class ComposeMessageViewModel : ViewModel() {

    private val _message = MutableStateFlow("")
    val message: StateFlow<String> = _message.asStateFlow()

    private val _mediaUris = MutableStateFlow<List<Uri>>(emptyList())
    val mediaUris: StateFlow<List<Uri>> = _mediaUris.asStateFlow()

    fun onMessageChange(newMessage: String) {
        _message.value = newMessage
    }

    fun onMediaSelected(uris: List<Uri>) {
        _mediaUris.value = _mediaUris.value + uris
    }

    fun removeMedia(uri: Uri) {
        _mediaUris.value = _mediaUris.value.toMutableList().apply { remove(uri) }
    }

    // Call this method when the ViewModel is no longer needed to clear the shared data
    fun clearSharedData() {
        _message.value = ""
        _mediaUris.value = emptyList()
    }
}
