package com.example.biblepaceproject.data

import org.junit.Assert.assertEquals
import org.junit.Test

class ChapterIdTest {
    @Test
    fun idsMatchTheDocumentedFormat() {
        assertEquals("001_001", ChapterId.of(1, 1))
        assertEquals("043_003", ChapterId.of(43, 3))
        assertEquals("019_119", ChapterId.of(19, 119))
        assertEquals("066_022", ChapterId.of(66, 22))
    }
}
