package com.fourseason.broadcast.data

import kotlinx.serialization.Serializable

@Serializable
data class BroadcastBackupData(
    val broadcastLists: List<BroadcastListWithContacts>
)
