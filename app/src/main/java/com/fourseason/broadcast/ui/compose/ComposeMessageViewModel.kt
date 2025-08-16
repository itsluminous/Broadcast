package com.fourseason.broadcast.ui.compose

import android.net.Uri
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class ComposeMessageViewModel : ViewModel() {

    private val _message = MutableStateFlow("")
    val message: StateFlow<String> = _message

    private val _mediaUri = MutableStateFlow<Uri?>(null)
    val mediaUri: StateFlow<Uri?> = _mediaUri

    fun onMessageChange(newMessage: String) {
        _message.value = newMessage
    }

    fun onMediaSelected(uri: Uri?) {
        _mediaUri.value = uri
    }
}
