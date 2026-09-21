package com.example.biblepaceproject.data

import android.content.Context
import androidx.core.content.edit

/** UI state only: chosen version and where the reader left off. Reading progress lives in Room (see ProgressRepository). */
class ReaderPrefs(context: Context) {
    private val prefs = context.getSharedPreferences("reader", Context.MODE_PRIVATE)

    var versionId: String
        get() = prefs.getString(KEY_VERSION, null) ?: DEFAULT_VERSION
        set(value) = prefs.edit { putString(KEY_VERSION, value) }

    var book: Int
        get() = prefs.getInt(KEY_BOOK, 1).coerceIn(1, BookCatalog.books.size)
        set(value) = prefs.edit { putInt(KEY_BOOK, value) }

    var chapter: Int
        get() = prefs.getInt(KEY_CHAPTER, 1)
        set(value) = prefs.edit { putInt(KEY_CHAPTER, value) }

    /** Progress saved by the first version of the app as "book:chapter" strings; imported into Room once, then ignored. */
    val legacyReadChapters: Set<String>
        get() = prefs.getStringSet(KEY_READ, emptySet()).orEmpty().toSet()

    var legacyImported: Boolean
        get() = prefs.getBoolean(KEY_LEGACY_IMPORTED, false)
        set(value) = prefs.edit { putBoolean(KEY_LEGACY_IMPORTED, value) }

    companion object {
        const val DEFAULT_VERSION = "kjv"
        private const val KEY_VERSION = "version"
        private const val KEY_BOOK = "book"
        private const val KEY_CHAPTER = "chapter"
        private const val KEY_READ = "read"
        private const val KEY_LEGACY_IMPORTED = "legacy_imported"
    }
}
