package com.example.biblepaceproject.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/** In-memory stand-in for the Room DAO so repository and sync rules can be tested on the JVM. */
class FakeDao : ChapterReadDao {
    val rows = MutableStateFlow<Map<String, ChapterRead>>(emptyMap())
    override fun observeReadIds(): Flow<List<String>> = rows.map { m -> m.values.filter { !it.deleted }.map { it.id } }
    override suspend fun get(id: String) = rows.value[id]
    override suspend fun dirtyRows() = rows.value.values.filter { it.dirty }
    override suspend fun upsert(row: ChapterRead) { rows.value = rows.value + (row.id to row) }
    override suspend fun upsertAll(rows: List<ChapterRead>) = rows.forEach { upsert(it) }
    override suspend fun markClean(id: String, updatedAt: Long) {
        rows.value[id]?.takeIf { it.updatedAt == updatedAt }?.let { rows.value = rows.value + (id to it.copy(dirty = false)) }
    }
    override suspend fun deleteAll() { rows.value = emptyMap() }
}
