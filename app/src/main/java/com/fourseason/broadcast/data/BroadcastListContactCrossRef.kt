package com.fourseason.broadcast.data

import androidx.room.Entity

@Entity(
    tableName = "broadcast_list_contact_cross_refs",
    primaryKeys = ["listId", "contactPhoneNumber"]
)
data class BroadcastListContactCrossRef(
    val listId: Long,
    val contactPhoneNumber: String
)
