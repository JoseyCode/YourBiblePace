package com.example.biblepaceproject.ui

import android.app.Application
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.biblepaceproject.data.BibleRepository
import com.example.biblepaceproject.data.BibleVersion
import com.example.biblepaceproject.data.AppDatabase
import com.example.biblepaceproject.data.BookCatalog
import com.example.biblepaceproject.data.ChapterId
import com.example.biblepaceproject.data.ProgressRepository
import com.example.biblepaceproject.data.ReaderPrefs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class Screen { Reader, Books }

data class ReaderState(
    val versions: List<BibleVersion> = emptyList(),
    val versionId: String = ReaderPrefs.DEFAULT_VERSION,
    val book: Int = 1,
    val chapter: Int = 1,
    val verses: List<String> = emptyList(),
    val loading: Boolean = true,
    val error: String? = null,
    val readChapters: Set<String> = emptySet(),
    val screen: Screen = Screen.Reader,
) {
    val bookName: String get() = BookCatalog.book(book).name
    val version: BibleVersion? get() = versions.firstOrNull { it.id == versionId }
    val isChapterRead: Boolean get() = ChapterId.of(book, chapter) in readChapters
    val hasPrevious: Boolean get() = !(book == 1 && chapter == 1)
    val hasNext: Boolean get() = !(book == BookCatalog.books.size && chapter == BookCatalog.book(book).chapters)
}

class ReaderViewModel(app: Application) : AndroidViewModel(app) {
    private val repository = BibleRepository(app)
    private val prefs = ReaderPrefs(app)
    private val progress = ProgressRepository(AppDatabase.get(app).chapterReadDao())
    private var loadJob: Job? = null

    var state by mutableStateOf(ReaderState())
        private set

    init {
        val versions = repository.versions
        val versionId = prefs.versionId.takeIf { id -> versions.any { it.id == id } } ?: versions.first().id
        val book = prefs.book
        val chapter = prefs.chapter.coerceIn(1, BookCatalog.book(book).chapters)
        state = state.copy(versions = versions, versionId = versionId, book = book, chapter = chapter)
        load()
        viewModelScope.launch {
            if (!prefs.legacyImported) {
                progress.importLegacy(prefs.legacyReadChapters)
                prefs.legacyImported = true
            }
            progress.readChapters.collect { state = state.copy(readChapters = it) }
        }
    }

    fun selectVersion(id: String) {
        if (id == state.versionId) return
        prefs.versionId = id
        state = state.copy(versionId = id)
        load()
    }

    fun openChapter(book: Int, chapter: Int) {
        prefs.book = book
        prefs.chapter = chapter
        state = state.copy(book = book, chapter = chapter, verses = emptyList(), screen = Screen.Reader)
        load()
    }

    fun next() {
        if (!state.hasNext) return
        val current = BookCatalog.book(state.book)
        if (state.chapter < current.chapters) openChapter(state.book, state.chapter + 1) else openChapter(state.book + 1, 1)
    }

    fun previous() {
        if (!state.hasPrevious) return
        if (state.chapter > 1) openChapter(state.book, state.chapter - 1)
        else openChapter(state.book - 1, BookCatalog.book(state.book - 1).chapters)
    }

    fun toggleRead() {
        val (book, chapter, versionId) = Triple(state.book, state.chapter, state.versionId)
        val markRead = !state.isChapterRead
        viewModelScope.launch { progress.setRead(book, chapter, markRead, versionId) }
    }

    fun showBooks() {
        state = state.copy(screen = Screen.Books)
    }

    fun showReader() {
        state = state.copy(screen = Screen.Reader)
    }

    private fun load() {
        loadJob?.cancel()
        val (versionId, book, chapter) = Triple(state.versionId, state.book, state.chapter)
        state = state.copy(loading = true, error = null)
        loadJob = viewModelScope.launch {
            try {
                val verses = withContext(Dispatchers.IO) { repository.loadChapter(versionId, book, chapter) }
                Log.d(TAG, "Loaded $versionId ${BookCatalog.book(book).name} $chapter (${verses.size} verses)")
                state = state.copy(verses = verses, loading = false)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load $versionId $book:$chapter", e)
                state = state.copy(verses = emptyList(), loading = false, error = "Couldn't load this chapter.")
            }
        }
    }

    private companion object {
        const val TAG = "ReaderViewModel"
    }
}

/** Everything the screens can ask for, so composables don't depend on the ViewModel directly (keeps previews and tests simple). */
data class ReaderActions(
    val onShowBooks: () -> Unit = {},
    val onShowReader: () -> Unit = {},
    val onOpenChapter: (book: Int, chapter: Int) -> Unit = { _, _ -> },
    val onSelectVersion: (String) -> Unit = {},
    val onNext: () -> Unit = {},
    val onPrevious: () -> Unit = {},
    val onToggleRead: () -> Unit = {},
)
