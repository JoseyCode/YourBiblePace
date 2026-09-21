package com.example.biblepaceproject.data

/**
 * Resolves the same chapter edited in two places (this device vs the cloud).
 *
 * Newer [ChapterRead.updatedAt] wins. On an exact tie a read beats an un-read, so a conflict can never cost progress.
 * The result's `dirty` flag says whether it still needs uploading: true only when the local copy won.
 */
fun mergeChapterRead(local: ChapterRead, remote: ChapterRead): ChapterRead {
    require(local.id == remote.id) { "Cannot merge ${local.id} with ${remote.id}" }
    val localWins = when {
        local.updatedAt != remote.updatedAt -> local.updatedAt > remote.updatedAt
        local.deleted != remote.deleted -> !local.deleted
        else -> false
    }
    return if (localWins) local.copy(dirty = true) else remote.copy(dirty = false)
}
