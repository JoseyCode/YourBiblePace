package com.example.biblepaceproject.data

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** Runs the real Room/SQLite stack on a device, with an in-memory database. */
@RunWith(AndroidJUnit4::class)
class ChapterReadDaoTest {
    private lateinit var db: AppDatabase
    private lateinit var repo: ProgressRepository

    @Before
    fun open() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        repo = ProgressRepository(db.chapterReadDao())
    }

    @After
    fun close() = db.close()

    @Test
    fun markUnmarkAndReadBack() = runBlocking {
        repo.setRead(43, 3, read = true, version = "kjv")
        repo.setRead(1, 1, read = true, version = "asv")
        assertEquals(setOf("001_001", "043_003"), repo.readChapters.first())

        repo.setRead(1, 1, read = false, version = "asv")
        assertEquals(setOf("043_003"), repo.readChapters.first())
        assertEquals(2, db.chapterReadDao().dirtyRows().size) // tombstone is kept and still needs uploading
    }
}
