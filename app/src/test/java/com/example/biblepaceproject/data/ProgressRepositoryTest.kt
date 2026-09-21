package com.example.biblepaceproject.data

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ProgressRepositoryTest {
    private var clock = 1_000L
    private val dao = FakeDao()
    private val repo = ProgressRepository(dao, now = { clock }, localDay = { "2026-09-21" })

    @Test
    fun markingReadRecordsDateVersionAndFlagsForUpload() = runBlocking {
        repo.setRead(43, 3, read = true, version = "kjv")
        val row = dao.get("043_003")!!
        assertEquals(1_000L, row.readAt)
        assertEquals("2026-09-21", row.readDay)
        assertEquals("kjv", row.version)
        assertTrue(row.dirty)
        assertFalse(row.deleted)
        assertEquals(setOf("043_003"), repo.readChapters.first())
    }

    @Test
    fun unmarkingKeepsATombstoneInsteadOfDeleting() = runBlocking {
        repo.setRead(43, 3, read = true, version = "kjv")
        clock = 2_000
        repo.setRead(43, 3, read = false, version = "kjv")
        val row = dao.get("043_003")!!
        assertTrue(row.deleted)
        assertTrue(row.dirty)
        assertTrue(repo.readChapters.first().isEmpty())
    }

    @Test
    fun unmarkingSomethingNeverReadDoesNothing() = runBlocking {
        repo.setRead(43, 3, read = false, version = null)
        assertNull(dao.get("043_003"))
    }

    @Test
    fun latestActionWinsEvenIfTheClockGoesBackwards() = runBlocking {
        repo.setRead(1, 1, read = true, version = null)
        clock = 500 // phone clock corrected backwards
        repo.setRead(1, 1, read = false, version = null)
        val row = dao.get("001_001")!!
        assertTrue(row.updatedAt > 1_000L)
        assertTrue(row.deleted)
    }

    @Test
    fun legacyImportKeepsProgressWithUnknownDatesAndSkipsGarbage() = runBlocking {
        repo.importLegacy(setOf("1:1", "43:3", "99:1", "1:99", "junk", "1:x"))
        assertEquals(setOf("001_001", "043_003"), repo.readChapters.first())
        val row = dao.get("043_003")!!
        assertNull(row.readAt)
        assertNull(row.readDay)
        assertTrue(row.dirty)
    }

    @Test
    fun legacyImportNeverOverwritesRealData() = runBlocking {
        repo.setRead(43, 3, read = true, version = "asv")
        repo.importLegacy(setOf("43:3"))
        assertEquals(1_000L, dao.get("043_003")!!.readAt)
    }
}
