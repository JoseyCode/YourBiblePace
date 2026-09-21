package com.example.biblepaceproject.data

import com.google.android.gms.tasks.Task
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.MemoryCacheSettings
import com.google.firebase.firestore.Source
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull
import java.io.IOException
import java.util.Date

/**
 * users/{uid}/reads/{chapterId} in Firestore; see docs/data-model.md for the contract.
 *
 * Firestore's own disk cache is switched off: Room is the offline store. Otherwise a write made offline could be queued by Firestore
 * and land much later, on top of newer changes from another device.
 */
class FirestoreReadStore(uid: String, private val db: FirebaseFirestore = configuredFirestore()) : RemoteReadStore {
    private val reads = db.collection("users").document(uid).collection("reads")

    override suspend fun fetchChangedSince(cursor: Long): RemoteFetch {
        // Small overlap so a document committed slightly out of order is never skipped; re-merging a row is harmless.
        val since = Timestamp(Date(maxOf(0L, cursor - OVERLAP_MILLIS)))
        val snapshot = reads.whereGreaterThan(SERVER_UPDATED_AT, since).get(Source.SERVER).awaitOrThrow()
        var newCursor = cursor
        val rows = snapshot.documents.mapNotNull { doc ->
            doc.getTimestamp(SERVER_UPDATED_AT)?.toDate()?.time?.let { newCursor = maxOf(newCursor, it) }
            doc.toChapterRead()
        }
        return RemoteFetch(rows, newCursor)
    }

    override suspend fun push(rows: List<ChapterRead>) {
        for (chunk in rows.chunked(BATCH_SIZE)) {
            val batch = db.batch()
            chunk.forEach { batch.set(reads.document(it.id), it.toFields()) }
            batch.commit().awaitOrThrow()
        }
    }

    private fun ChapterRead.toFields() = mapOf(
        "book" to book,
        "chapter" to chapter,
        "readAt" to readAt,
        "readDay" to readDay,
        "version" to version,
        "updatedAt" to updatedAt,
        "deleted" to deleted,
        "schemaVersion" to SCHEMA_VERSION,
        SERVER_UPDATED_AT to FieldValue.serverTimestamp(),
    )

    /** Tolerant parsing: anything malformed is skipped rather than crashing sync. */
    private fun DocumentSnapshot.toChapterRead(): ChapterRead? {
        val book = getLong("book")?.toInt() ?: return null
        val chapter = getLong("chapter")?.toInt() ?: return null
        if (book !in 1..BookCatalog.books.size || chapter !in 1..BookCatalog.book(book).chapters) return null
        if (id != ChapterId.of(book, chapter)) return null
        return ChapterRead(
            id = id, book = book, chapter = chapter,
            readAt = getLong("readAt"), readDay = getString("readDay"), version = getString("version"),
            updatedAt = getLong("updatedAt") ?: return null,
            deleted = getBoolean("deleted") ?: false,
            dirty = false,
        )
    }

    /** Fails with an IOException instead of hanging forever when offline. (A plain timeout exception would look like cancellation.) */
    private suspend fun <T> Task<T>.awaitOrThrow(): T =
        withTimeoutOrNull(TIMEOUT_MILLIS) { await() } ?: throw IOException("Timed out talking to the cloud")

    private companion object {
        const val SERVER_UPDATED_AT = "serverUpdatedAt"
        const val SCHEMA_VERSION = 1
        const val BATCH_SIZE = 400
        const val TIMEOUT_MILLIS = 20_000L
        const val OVERLAP_MILLIS = 5_000L

        fun configuredFirestore(): FirebaseFirestore {
            val db = FirebaseFirestore.getInstance()
            try {
                db.firestoreSettings = FirebaseFirestoreSettings.Builder().setLocalCacheSettings(MemoryCacheSettings.newBuilder().build()).build()
            } catch (_: IllegalStateException) {
                // Settings can only be applied before Firestore is first used; they were already applied.
            }
            return db
        }
    }
}
