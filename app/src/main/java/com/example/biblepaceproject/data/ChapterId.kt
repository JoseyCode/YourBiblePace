package com.example.biblepaceproject.data

/** Canonical, version-independent chapter identifier shared by Room and Firestore. See docs/data-model.md. */
object ChapterId {
    /** Genesis 1 = "001_001", John 3 = "043_003". Zero padded so ids sort in reading order. */
    fun of(book: Int, chapter: Int): String = "%03d_%03d".format(book, chapter)
}
