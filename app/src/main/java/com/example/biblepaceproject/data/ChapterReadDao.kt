package com.example.biblepaceproject.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ChapterReadDao {
    /** Ids of chapters currently marked read (tombstones excluded). */
    @Query("SELECT id FROM chapter_reads WHERE deleted = 0")
    fun observeReadIds(): Flow<List<String>>

    @Query("SELECT * FROM chapter_reads WHERE id = :id")
    suspend fun get(id: String): ChapterRead?

    @Query("SELECT * FROM chapter_reads WHERE dirty = 1")
    suspend fun dirtyRows(): List<ChapterRead>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(row: ChapterRead)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(rows: List<ChapterRead>)

    /** Marks a row uploaded, but only if it hasn't changed since it was read for upload (so a tap made mid-sync isn't lost). */
    @Query("UPDATE chapter_reads SET dirty = 0 WHERE id = :id AND updatedAt = :updatedAt")
    suspend fun markClean(id: String, updatedAt: Long)

    @Query("DELETE FROM chapter_reads")
    suspend fun deleteAll()
}
