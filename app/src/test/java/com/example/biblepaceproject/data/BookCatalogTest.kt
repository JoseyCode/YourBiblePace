package com.example.biblepaceproject.data

import org.junit.Assert.assertEquals
import org.junit.Test

class BookCatalogTest {
    @Test
    fun testamentSplitsAtMatthew() {
        assertEquals("Malachi", BookCatalog.books.last { it.testament == Testament.Old }.name)
        assertEquals("Matthew", BookCatalog.books.first { it.testament == Testament.New }.name)
    }

    @Test
    fun psalmsIsTheLongestBook() {
        assertEquals(150, BookCatalog.book(19).chapters)
        assertEquals("Psalms", BookCatalog.book(19).name)
    }
}
