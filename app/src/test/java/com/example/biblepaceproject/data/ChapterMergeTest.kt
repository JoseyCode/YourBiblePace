package com.example.biblepaceproject.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ChapterMergeTest {
    private fun row(updatedAt: Long, deleted: Boolean = false, dirty: Boolean = false, version: String? = "kjv") =
        ChapterRead("043_003", 43, 3, readAt = 1, readDay = "2026-09-21", version = version, updatedAt = updatedAt, deleted = deleted, dirty = dirty)

    @Test
    fun newerLocalWinsAndStillNeedsUpload() {
        val merged = mergeChapterRead(local = row(200, version = "asv"), remote = row(100))
        assertEquals("asv", merged.version)
        assertTrue(merged.dirty)
    }

    @Test
    fun newerRemoteWinsAndIsClean() {
        val merged = mergeChapterRead(local = row(100, dirty = true), remote = row(200, deleted = true))
        assertTrue(merged.deleted)
        assertFalse(merged.dirty)
    }

    @Test
    fun newerUnmarkBeatsOlderRead() {
        assertTrue(mergeChapterRead(local = row(300, deleted = true), remote = row(200)).deleted)
    }

    @Test
    fun tieKeepsTheReadSoProgressIsNeverLost() {
        assertFalse(mergeChapterRead(local = row(100, deleted = true), remote = row(100)).deleted)
        assertFalse(mergeChapterRead(local = row(100), remote = row(100, deleted = true)).deleted)
    }

    @Test
    fun identicalRowsEndUpClean() {
        assertFalse(mergeChapterRead(row(100, dirty = true), row(100)).dirty)
    }

    @Test(expected = IllegalArgumentException::class)
    fun differentChaptersCannotBeMerged() {
        mergeChapterRead(row(1), row(1).copy(id = "043_004"))
    }
}
