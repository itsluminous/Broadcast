package com.fourseason.broadcast.data

import androidx.room.Embedded
import androidx.room.Junction
import androidx.room.Relation

import kotlinx.serialization.Serializable

@Serializable
data class BroadcastListWithContacts(
    @Embedded val broadcastList: BroadcastList,
    @Relation(
        parentColumn = "id", // PK of BroadcastList
        entityColumn = "phoneNumber", // PK of Contact
        associateBy = Junction(
            value = BroadcastListContactCrossRef::class,
            parentColumn = "listId", // FK in BroadcastListContactCrossRef pointing to BroadcastList
            entityColumn = "contactPhoneNumber" // FK in BroadcastListContactCrossRef pointing to Contact
        )
    )
    val contacts: List<Contact> = emptyList()
)
