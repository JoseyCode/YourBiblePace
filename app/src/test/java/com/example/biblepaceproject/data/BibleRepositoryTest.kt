package com.example.biblepaceproject.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** Runs on the JVM against the real bundled assets, so a bad asset regeneration fails here instead of on a device. */
class BibleRepositoryTest {
    private val repo = BibleRepository { path -> File("src/main/assets/$path").inputStream() }

    @Test
    fun catalogHas66BooksAnd1189Chapters() {
        assertEquals(66, BookCatalog.books.size)
        assertEquals(1189, BookCatalog.totalChapters)
    }

    @Test
    fun threeVersionsAreBundled() {
        assertEquals(listOf("kjv", "asv", "nheb"), repo.versions.map { it.id })
    }

    @Test
    fun everyVersionMatchesTheCatalogChapterForChapter() {
        for (version in repo.versions) {
            var blankVerses = 0
            for (book in BookCatalog.books) for (chapter in 1..book.chapters) {
                val verses = repo.loadChapter(version.id, book.number, chapter)
                assertTrue("${version.id} ${book.name} $chapter has no text", verses.any { it.isNotBlank() })
                blankVerses += verses.count { it.isBlank() }
            }
            // A few verses are intentionally empty in critical-text versions (ASV, NHEB); anything more means corrupt data.
            assertTrue("${version.id} has $blankVerses blank verses", blankVerses <= 20)
        }
    }

    @Test
    fun john316ReadsDifferentlyPerVersion() {
        val kjv = repo.loadChapter("kjv", 43, 3)[15]
        val nheb = repo.loadChapter("nheb", 43, 3)[15]
        assertTrue(kjv.startsWith("For God so loved the world"))
        assertTrue(kjv.contains("only begotten Son"))
        assertTrue(nheb.contains("one and only Son"))
    }

    @Test(expected = IllegalArgumentException::class)
    fun outOfRangeChapterIsRejected() {
        repo.loadChapter("kjv", 1, 51)
    }
}
