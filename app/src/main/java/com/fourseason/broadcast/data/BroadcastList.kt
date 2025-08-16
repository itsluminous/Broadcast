package com.fourseason.broadcast.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "broadcast_lists")
data class BroadcastList ( // Changed to data class
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val iconEmoji: String
)
