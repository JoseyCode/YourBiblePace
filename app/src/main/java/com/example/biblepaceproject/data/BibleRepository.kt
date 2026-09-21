package com.example.biblepaceproject.data

import android.content.Context
import org.json.JSONArray
import java.io.InputStream

data class BibleVersion(val id: String, val name: String, val abbreviation: String, val description: String)

/**
 * Reads the bundled public domain texts from assets/bibles. Each version is a folder of 66 small files (one per book) so we only
 * ever parse the book being read.
 */
class BibleRepository(private val openAsset: (path: String) -> InputStream) {

    constructor(context: Context) : this({ path -> context.assets.open(path) })

    // Access-ordered LinkedHashMap as a small LRU (plain Java, so it also works in JVM unit tests, unlike android.util.LruCache).
    private val cache = object : LinkedHashMap<String, List<List<String>>>(16, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, List<List<String>>>) = size > MAX_CACHED_BOOKS
    }

    val versions: List<BibleVersion> by lazy {
        val array = JSONArray(read("bibles/versions.json"))
        List(array.length()) { i ->
            val o = array.getJSONObject(i)
            BibleVersion(o.getString("id"), o.getString("name"), o.getString("abbreviation"), o.getString("description"))
        }
    }

    /** Verses of one chapter, in order; verse N is at index N - 1. Blocking: call off the main thread. */
    fun loadChapter(versionId: String, book: Int, chapter: Int): List<String> {
        val chapters = loadBook(versionId, book)
        require(chapter in 1..chapters.size) { "Chapter $chapter is out of range for book $book" }
        return chapters[chapter - 1]
    }

    private fun loadBook(versionId: String, book: Int): List<List<String>> {
        val key = "$versionId/$book"
        synchronized(cache) { cache[key]?.let { return it } }
        val parsed = JSONArray(read("bibles/$versionId/%02d.json".format(book))).let { chapters ->
            List(chapters.length()) { c ->
                val verses = chapters.getJSONArray(c)
                List(verses.length()) { v -> verses.getString(v) }
            }
        }
        synchronized(cache) { cache[key] = parsed }
        return parsed
    }

    private fun read(path: String): String = openAsset(path).bufferedReader(Charsets.UTF_8).use { it.readText() }

    private companion object {
        const val MAX_CACHED_BOOKS = 12
    }
}
