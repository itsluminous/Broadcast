package com.fourseason.broadcast.data

import androidx.room.Embedded
import androidx.room.Junction
import androidx.room.Relation

data class BroadcastListWithContacts(
    @Embedded val broadcastList: BroadcastList,
    @Relation(
        parentColumn = "id",
        entityColumn = "phoneNumber",
        associateBy = Junction(BroadcastListContactCrossRef::class)
    )
    val contacts: List<Contact>
)
