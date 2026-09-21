package com.example.biblepaceproject.data

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.io.IOException
import kotlinx.coroutines.flow.first

private class FakeStore : RemoteReadStore {
    val docs = mutableMapOf<String, ChapterRead>()
    var cursor = 0L
    var offline = false
    var onPush: suspend () -> Unit = {}
    val pushed = mutableListOf<ChapterRead>()

    override suspend fun fetchChangedSince(cursor: Long): RemoteFetch {
        if (offline) throw IOException("offline")
        return RemoteFetch(docs.values.toList(), this.cursor)
    }

    override suspend fun push(rows: List<ChapterRead>) {
        if (offline) throw IOException("offline")
        onPush()
        rows.forEach { docs[it.id] = it.copy(dirty = false) }
        pushed += rows
    }
}

class SyncTest {
    private var clock = 1_000L
    private val dao = FakeDao()
    private val store = FakeStore()
    private val repo = ProgressRepository(dao, now = { clock }, localDay = { "2026-09-21" })

    private fun remote(id: String, updatedAt: Long, deleted: Boolean = false): ChapterRead {
        val (book, chapter) = id.split("_").map { it.toInt() }
        return ChapterRead(id, book, chapter, readAt = 1, readDay = "2026-01-01", version = "kjv", updatedAt = updatedAt, deleted = deleted, dirty = false)
    }

    @Test
    fun aFreshDevicePullsEverythingFromTheCloud() = runBlocking {
        store.docs["043_003"] = remote("043_003", 500)
        store.docs["001_001"] = remote("001_001", 600)
        store.cursor = 77

        val cursor = repo.sync(store, 0)

        assertEquals(77L, cursor)
        assertEquals(setOf("043_003", "001_001"), repo.readChapters.kotlinFirst())
        assertFalse(repo.hasUnsynced())
        assertTrue(store.pushed.isEmpty())
    }

    @Test
    fun localReadsAreUploadedAndMarkedClean() = runBlocking {
        repo.setRead(43, 3, read = true, version = "kjv")
        assertTrue(repo.hasUnsynced())

        repo.sync(store, 0)

        assertEquals(listOf("043_003"), store.pushed.map { it.id })
        assertFalse(store.docs["043_003"]!!.deleted)
        assertFalse(repo.hasUnsynced())
    }

    @Test
    fun guestProgressMergesWithAnAccountsExistingProgress() = runBlocking {
        repo.setRead(1, 1, read = true, version = null) // read while signed out
        store.docs["043_003"] = remote("043_003", 500) // already in the account from another phone

        repo.sync(store, 0)

        assertEquals(setOf("001_001", "043_003"), repo.readChapters.kotlinFirst())
        assertEquals(setOf("001_001", "043_003"), store.docs.filterValues { !it.deleted }.keys)
    }

    @Test
    fun aNewerUnmarkFromAnotherDeviceWinsLocally() = runBlocking {
        repo.setRead(43, 3, read = true, version = "kjv") // updatedAt 1000
        store.docs["043_003"] = remote("043_003", 2_000, deleted = true)

        repo.sync(store, 0)

        assertTrue(repo.readChapters.kotlinFirst().isEmpty())
        assertFalse(repo.hasUnsynced())
    }

    @Test
    fun aNewerLocalReadBeatsAnOlderRemoteUnmark() = runBlocking {
        store.docs["043_003"] = remote("043_003", 500, deleted = true)
        repo.setRead(43, 3, read = true, version = "kjv") // updatedAt 1000

        repo.sync(store, 0)

        assertFalse(store.docs["043_003"]!!.deleted)
        assertEquals(setOf("043_003"), repo.readChapters.kotlinFirst())
    }

    @Test
    fun offlineSyncThrowsAndKeepsEverythingForNextTime() = runBlocking {
        repo.setRead(43, 3, read = true, version = "kjv")
        store.offline = true

        try {
            repo.sync(store, 5)
            fail("expected an IOException")
        } catch (_: IOException) {
        }

        assertTrue(repo.hasUnsynced())
        assertEquals(setOf("043_003"), repo.readChapters.kotlinFirst())
    }

    @Test
    fun aTapDuringUploadIsNotMarkedCleanBehindItsBack() = runBlocking {
        repo.setRead(43, 3, read = true, version = "kjv")
        store.onPush = {
            clock = 3_000
            repo.setRead(43, 3, read = false, version = "kjv") // the user un-marks while the upload is in flight
        }

        repo.sync(store, 0)

        assertTrue("the un-mark still has to be uploaded", repo.hasUnsynced())
        assertTrue(dao.get("043_003")!!.deleted)
        store.onPush = {}
        repo.sync(store, 0)
        assertTrue(store.docs["043_003"]!!.deleted)
        assertFalse(repo.hasUnsynced())
    }

    @Test
    fun clearAllWipesThisDevicesCopyOnly() = runBlocking {
        repo.setRead(43, 3, read = true, version = "kjv")
        repo.sync(store, 0)

        repo.clearAll()

        assertNull(dao.get("043_003"))
        assertTrue(store.docs.containsKey("043_003"))
        // Signing back in restores it.
        repo.sync(store, 0)
        assertEquals(setOf("043_003"), repo.readChapters.kotlinFirst())
    }
}

private suspend fun <T> kotlinx.coroutines.flow.Flow<T>.kotlinFirst(): T = first()
