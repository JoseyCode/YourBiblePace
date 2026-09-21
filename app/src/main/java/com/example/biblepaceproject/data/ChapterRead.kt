package com.example.biblepaceproject.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * One row per chapter the user has ever marked (or un-marked) read. Mirrors the Firestore document
 * users/{uid}/reads/{id}; see docs/data-model.md before changing anything here.
 */
@Entity(tableName = "chapter_reads")
data class ChapterRead(
    @PrimaryKey val id: String,
    val book: Int,
    val chapter: Int,
    /** Epoch millis (UTC) when marked read; null when imported from before dates were tracked. */
    val readAt: Long?,
    /** The user's local calendar day at that moment, "yyyy-MM-dd". */
    val readDay: String?,
    /** Bible version being read at the time. Informational. */
    val version: String?,
    /** Epoch millis (UTC) of the last change; decides conflicts. */
    val updatedAt: Long,
    /** Tombstone: true when the user un-marked it. Kept so the un-mark syncs instead of the chapter reappearing. */
    val deleted: Boolean,
    /** Local only: changed on this device and not yet uploaded. */
    val dirty: Boolean,
)
