package com.fourseason.broadcast.ui

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.fourseason.broadcast.data.BroadcastRepository
import com.fourseason.broadcast.ui.compose.ComposeMessageViewModel
import com.fourseason.broadcast.ui.contactpicker.ContactPickerViewModel
import com.fourseason.broadcast.ui.createedit.CreateEditListViewModel
import com.fourseason.broadcast.ui.main.BroadcastListViewModel

class ViewModelFactory(
    private val repository: BroadcastRepository,
    private val application: Application
    ) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(BroadcastListViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return BroadcastListViewModel(repository) as T
        }
        if (modelClass.isAssignableFrom(ContactPickerViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ContactPickerViewModel(application, repository) as T
        }
        if (modelClass.isAssignableFrom(CreateEditListViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return CreateEditListViewModel(repository) as T
        }
        if (modelClass.isAssignableFrom(ComposeMessageViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ComposeMessageViewModel() as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
