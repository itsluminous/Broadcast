package com.fourseason.broadcast.ui

import com.fourseason.broadcast.data.Contact

object TemporaryDataHolder {
    var selectedContacts: List<Contact> = emptyList()
    var editingListId: Long? = null
}
