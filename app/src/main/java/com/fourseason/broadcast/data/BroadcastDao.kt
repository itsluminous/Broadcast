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
    suspend fun insertContact(contact: Contact)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertContacts(contacts: List<Contact>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBroadcastListContactCrossRef(crossRef: BroadcastListContactCrossRef)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBroadcastListContactCrossRefs(crossRefs: List<BroadcastListContactCrossRef>)

    @androidx.room.Update
    suspend fun updateBroadcastList(broadcastList: BroadcastList)

    @Transaction
    @Query("SELECT * FROM broadcast_lists")
    fun getBroadcastListsWithContactsFlow(): Flow<List<BroadcastListWithContacts>>

    @Transaction
    @Query("SELECT * FROM broadcast_lists")
    suspend fun getAllBroadcastListsWithContacts(): List<BroadcastListWithContacts>

    @Transaction
    @Query("SELECT * FROM broadcast_lists WHERE id = :listId")
    fun getBroadcastListWithContacts(listId: Long): Flow<BroadcastListWithContacts>

    @Query("DELETE FROM broadcast_lists WHERE id = :listId")
    suspend fun deleteBroadcastListById(listId: Long)

    @Query("DELETE FROM broadcast_list_contact_cross_refs WHERE listId = :listId")
    suspend fun deleteCrossRefsByListId(listId: Long)

    @Transaction
    suspend fun updateBroadcastListWithContacts(list: BroadcastList, contacts: List<Contact>) {
        updateBroadcastList(list) // Update the BroadcastList entity itself
        insertContacts(contacts)
        deleteCrossRefsByListId(list.id)
        val crossRefs = contacts.map { BroadcastListContactCrossRef(list.id, it.phoneNumber) }
        insertBroadcastListContactCrossRefs(crossRefs)
    }

    @Query("DELETE FROM broadcast_lists")
    suspend fun clearAllBroadcastLists()

    @Query("DELETE FROM contacts")
    suspend fun clearAllContacts()

    @Query("DELETE FROM broadcast_list_contact_cross_refs")
    suspend fun clearAllBroadcastListContactCrossRefs()
}
