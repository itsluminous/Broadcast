package com.fourseason.broadcast.data

import kotlinx.coroutines.flow.Flow

class BroadcastRepository(private val broadcastDao: BroadcastDao) {

    fun getAllBroadcastListsWithContacts(): Flow<List<BroadcastListWithContacts>> {
        return broadcastDao.getAllBroadcastListsWithContacts()
    }

    fun getBroadcastListWithContacts(listId: Long): Flow<BroadcastListWithContacts> {
        return broadcastDao.getBroadcastListWithContacts(listId)
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
}
