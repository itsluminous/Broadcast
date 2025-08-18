package com.fourseason.broadcast.data

import androidx.room.Entity
import androidx.room.Index

@Entity(
    tableName = "broadcast_list_contact_cross_refs",
    primaryKeys = ["listId", "contactPhoneNumber"],
    indices = [Index(value = ["contactPhoneNumber"])]
)
data class BroadcastListContactCrossRef(
    val listId: Long,
    val contactPhoneNumber: String
)
