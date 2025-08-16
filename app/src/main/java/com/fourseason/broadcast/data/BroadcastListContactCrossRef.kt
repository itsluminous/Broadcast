package com.fourseason.broadcast.data

import androidx.room.Entity

@Entity(primaryKeys = ["listId", "contactPhoneNumber"])
data class BroadcastListContactCrossRef(
    val listId: Long,
    val contactPhoneNumber: String
)
