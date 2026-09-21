package com.example.biblepaceproject.data

/** Rows changed in the cloud since the caller's cursor, plus the cursor to use next time. */
data class RemoteFetch(val rows: List<ChapterRead>, val cursor: Long)

/** The cloud side of sync, kept behind an interface so the sync rules can be unit tested without Firebase. */
interface RemoteReadStore {
    /** Rows whose server-side change time is after [cursor] (epoch millis; 0 = everything). Throws if the cloud can't be reached. */
    suspend fun fetchChangedSince(cursor: Long): RemoteFetch

    /** Writes [rows] to the cloud. Returns normally only once the server has accepted them; throws otherwise. */
    suspend fun push(rows: List<ChapterRead>)
}
