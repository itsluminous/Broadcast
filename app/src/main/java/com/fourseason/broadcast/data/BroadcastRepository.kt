package com.fourseason.broadcast.data

import kotlinx.coroutines.flow.Flow

class BroadcastRepository(private val broadcastDao: BroadcastDao) {

    fun getBroadcastListsWithContactsFlow(): Flow<List<BroadcastListWithContacts>> {
        return broadcastDao.getBroadcastListsWithContactsFlow()
    }

    suspend fun getAllBroadcastListsWithContacts(): List<BroadcastListWithContacts> {
        return broadcastDao.getAllBroadcastListsWithContacts()
    }

    fun getBroadcastListWithContacts(listId: Long): Flow<BroadcastListWithContacts> {
        return broadcastDao.getBroadcastListWithContacts(listId)
    }

    suspend fun insertBroadcastList(list: BroadcastList): Long {
        return broadcastDao.insertBroadcastList(list)
    }

    suspend fun insertContact(contact: Contact) {
        return broadcastDao.insertContact(contact)
    }

    suspend fun insertBroadcastListContactCrossRef(crossRef: BroadcastListContactCrossRef) {
        broadcastDao.insertBroadcastListContactCrossRef(crossRef)
    }

    suspend fun insertBroadcastListWithContacts(list: BroadcastList, contacts: List<Contact>) {
        val listId = broadcastDao.insertBroadcastList(list)
        broadcastDao.insertContacts(contacts)
        val crossRefs = contacts.map { BroadcastListContactCrossRef(listId, it.phoneNumber) }
        broadcastDao.insertBroadcastListContactCrossRefs(crossRefs)
    }

    suspend fun updateBroadcastListWithContacts(list: BroadcastList, contacts: List<Contact>) {
        broadcastDao.updateBroadcastListWithContacts(list, contacts)
    }

    suspend fun deleteBroadcastListById(listId: Long) {
        broadcastDao.deleteBroadcastListById(listId)
        // Also delete associated cross-references
        broadcastDao.deleteCrossRefsByListId(listId)
    }

    suspend fun clearAllBroadcastData() {
        broadcastDao.clearAllBroadcastLists()
        broadcastDao.clearAllContacts()
        broadcastDao.clearAllBroadcastListContactCrossRefs()
    }
}
