package com.fourseason.broadcast.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface BroadcastDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBroadcastList(broadcastList: BroadcastList): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertContacts(contacts: List<Contact>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBroadcastListContactCrossRefs(crossRefs: List<BroadcastListContactCrossRef>)

    @Transaction
    @Query("SELECT * FROM broadcast_lists")
    fun getAllBroadcastListsWithContacts(): Flow<List<BroadcastListWithContacts>>

    @Transaction
    @Query("SELECT * FROM broadcast_lists WHERE id = :listId")
    fun getBroadcastListWithContacts(listId: Long): Flow<BroadcastListWithContacts>

    @Query("DELETE FROM broadcast_list_contact_cross_refs WHERE listId = :listId")
    suspend fun deleteContactsForList(listId: Long)

    @Transaction
    suspend fun updateBroadcastListWithContacts(list: BroadcastList, contacts: List<Contact>) {
        insertContacts(contacts)
        deleteContactsForList(list.id)
        val crossRefs = contacts.map { BroadcastListContactCrossRef(list.id, it.phoneNumber) }
        insertBroadcastListContactCrossRefs(crossRefs)
    }
}
