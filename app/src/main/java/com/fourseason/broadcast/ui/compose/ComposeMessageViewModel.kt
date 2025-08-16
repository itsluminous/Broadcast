package com.fourseason.broadcast.ui.compose

import android.net.Uri
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class ComposeMessageViewModel : ViewModel() {

    private val _message = MutableStateFlow("")
    val message: StateFlow<String> = _message.asStateFlow()

    private val _mediaUri = MutableStateFlow<Uri?>(null)
    val mediaUri: StateFlow<Uri?> = _mediaUri.asStateFlow()

    fun onMessageChange(newMessage: String) {
        _message.value = newMessage
    }

    fun onMediaSelected(uri: Uri?) {
        _mediaUri.value = uri
    }

    // Call this method when the ViewModel is no longer needed to clear the shared data
    fun clearSharedData() {
        _message.value = ""
        _mediaUri.value = null
    }
}
