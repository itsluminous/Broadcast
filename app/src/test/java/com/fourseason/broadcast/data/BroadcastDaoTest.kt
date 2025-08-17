package com.fourseason.broadcast.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.IOException

@RunWith(RobolectricTestRunner::class)
class BroadcastDaoTest {

    private lateinit var broadcastDao: BroadcastDao
    private lateinit var db: AppDatabase

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(
            context, AppDatabase::class.java
        ).allowMainThreadQueries().build()
        broadcastDao = db.broadcastDao()
    }

    @After
    @Throws(IOException::class)
    fun closeDb() {
        db.close()
    }

    @Test
    @Throws(Exception::class)
    fun insertAndGetBroadcastList() = runBlocking {
        val broadcastList = BroadcastList(name = "Test List", iconEmoji = "✅")
        val listId = broadcastDao.insertBroadcastList(broadcastList)
        val retrievedList = broadcastDao.getBroadcastListWithContacts(listId).first()
        assertEquals(retrievedList.broadcastList.name, "Test List")
    }

    @Test
    @Throws(Exception::class)
    fun insertListWithContactsAndGet() = runBlocking {
        val list = BroadcastList(name = "Family", iconEmoji = "👨‍👩‍👧‍👦")
        val listId = broadcastDao.insertBroadcastList(list)

        val contacts = listOf(
            Contact(phoneNumber = "123", name = "John"),
            Contact(phoneNumber = "456", name = "Jane")
        )
        broadcastDao.insertContacts(contacts)

        val crossRefs = contacts.map { BroadcastListContactCrossRef(listId, it.phoneNumber) }
        broadcastDao.insertBroadcastListContactCrossRefs(crossRefs)

        val listWithContacts = broadcastDao.getBroadcastListWithContacts(listId).first()

        assertEquals("Family", listWithContacts.broadcastList.name)
        assertEquals(2, listWithContacts.contacts.size)
        assertTrue(listWithContacts.contacts.contains(Contact(phoneNumber = "123", name = "John")))
    }

    @Test
    @Throws(Exception::class)
    fun deleteBroadcastList() = runBlocking {
        val broadcastList = BroadcastList(name = "To Delete", iconEmoji = "❌")
        val listId = broadcastDao.insertBroadcastList(broadcastList)

        broadcastDao.deleteBroadcastListById(listId)

        val allLists = broadcastDao.getAllBroadcastListsWithContacts().first()
        assertTrue(allLists.none { it.broadcastList.id == listId })
    }
}
