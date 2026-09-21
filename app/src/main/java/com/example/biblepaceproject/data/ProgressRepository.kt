package com.example.biblepaceproject.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.flow.map
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** The only place that changes reading progress. Writes locally first; cloud sync (later) picks up rows flagged dirty. */
class ProgressRepository(
    private val dao: ChapterReadDao,
    private val now: () -> Long = System::currentTimeMillis,
    private val localDay: (Long) -> String = { millis -> SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(millis)) },
) {
    // Guards read-modify-write of a row, so a tap and a sync merge can't interleave. Never held across network calls.
    private val lock = Mutex()

    /** ChapterIds of every chapter currently marked read. */
    val readChapters: Flow<Set<String>> = dao.observeReadIds().map { it.toSet() }

    suspend fun setRead(book: Int, chapter: Int, read: Boolean, version: String?) = lock.withLock {
        val id = ChapterId.of(book, chapter)
        val existing = dao.get(id)
        // Strictly after any earlier change, even if the phone's clock moved backwards, so the newest action always wins.
        val timestamp = maxOf(now(), (existing?.updatedAt ?: 0L) + 1)
        if (read) {
            dao.upsert(
                ChapterRead(id, book, chapter, readAt = timestamp, readDay = localDay(timestamp), version = version, updatedAt = timestamp, deleted = false, dirty = true)
            )
        } else if (existing != null && !existing.deleted) {
            dao.upsert(existing.copy(deleted = true, updatedAt = timestamp, dirty = true))
        }
    }

    /** One-time import of progress saved by the pre-database version ("book:chapter" keys). Dates are unknown, so left null. */
    suspend fun importLegacy(keys: Set<String>) = lock.withLock {
        val stamp = now()
        for (key in keys) {
            val (book, chapter) = key.split(":").mapNotNull { it.toIntOrNull() }.takeIf { it.size == 2 } ?: continue
            if (book !in 1..BookCatalog.books.size || chapter !in 1..BookCatalog.book(book).chapters) continue
            val id = ChapterId.of(book, chapter)
            if (dao.get(id) == null) {
                dao.upsert(ChapterRead(id, book, chapter, readAt = null, readDay = null, version = null, updatedAt = stamp, deleted = false, dirty = true))
            }
        }
    }

    /**
     * Two-way sync: pull what changed in the cloud, merge it in (see [mergeChapterRead]), then upload whatever is still only on this device.
     * Returns the cursor to pass next time. Throws if the cloud can't be reached; rows stay dirty and the next sync retries.
     */
    suspend fun sync(store: RemoteReadStore, cursor: Long): Long {
        val fetched = store.fetchChangedSince(cursor)
        lock.withLock {
            for (remote in fetched.rows) {
                val local = dao.get(remote.id)
                dao.upsert(if (local == null) remote.copy(dirty = false) else mergeChapterRead(local, remote))
            }
        }
        val pending = dao.dirtyRows()
        if (pending.isNotEmpty()) {
            store.push(pending)
            pending.forEach { dao.markClean(it.id, it.updatedAt) }
        }
        return fetched.cursor
    }

    suspend fun hasUnsynced(): Boolean = dao.dirtyRows().isNotEmpty()

    /** Wipes this device's copy. Only for sign-out, after everything has been uploaded, so the next person can't inherit it. */
    suspend fun clearAll() = lock.withLock { dao.deleteAll() }
}
