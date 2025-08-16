package com.fourseason.broadcast.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "contacts")
data class Contact(
    @PrimaryKey
    val phoneNumber: String,
    val name: String
)
