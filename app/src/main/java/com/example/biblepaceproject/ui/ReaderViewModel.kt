package com.example.biblepaceproject.ui

import android.app.Activity
import android.app.Application
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.biblepaceproject.data.BibleRepository
import com.example.biblepaceproject.data.BibleVersion
import androidx.credentials.exceptions.GetCredentialCancellationException
import com.example.biblepaceproject.data.AppDatabase
import com.example.biblepaceproject.data.AuthManager
import com.example.biblepaceproject.data.FirestoreReadStore
import com.example.biblepaceproject.data.BookCatalog
import com.example.biblepaceproject.data.ChapterId
import com.example.biblepaceproject.data.ProgressRepository
import com.example.biblepaceproject.data.ReaderPrefs
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class Screen { Reader, Books }

enum class SyncStatus { Idle, Syncing, Synced, Failed }

data class AccountState(
    /** False in builds without google-services.json: the app works fully offline and hides all account UI. */
    val configured: Boolean = false,
    val signedIn: Boolean = false,
    val email: String? = null,
    val status: SyncStatus = SyncStatus.Idle,
    val message: String? = null,
    val busy: Boolean = false,
    val nudgeDismissed: Boolean = false,
) {
    val showNudge: Boolean get() = configured && !signedIn && !nudgeDismissed
}

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
    val account: AccountState = AccountState(),
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
    private val auth = AuthManager(app)
    private var loadJob: Job? = null
    private var syncJob: Job? = null
    private var resync = false
    private var uid: String? = null

    var state by mutableStateOf(ReaderState())
        private set

    init {
        val versions = repository.versions
        val versionId = prefs.versionId.takeIf { id -> versions.any { it.id == id } } ?: versions.first().id
        val book = prefs.book
        val chapter = prefs.chapter.coerceIn(1, BookCatalog.book(book).chapters)
        state = state.copy(
            versions = versions, versionId = versionId, book = book, chapter = chapter,
            account = AccountState(configured = auth.isConfigured, nudgeDismissed = prefs.nudgeDismissed),
        )
        load()
        viewModelScope.launch {
            auth.user().collect { user ->
                uid = user?.uid
                setAccount { copy(signedIn = user != null, email = user?.email, status = SyncStatus.Idle, message = null) }
                requestSync()
            }
        }
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
        viewModelScope.launch {
            progress.setRead(book, chapter, markRead, versionId)
            requestSync()
        }
    }

    fun dismissNudge() {
        prefs.nudgeDismissed = true
        setAccount { copy(nudgeDismissed = true) }
    }

    fun signIn(activity: Activity) {
        viewModelScope.launch {
            setAccount { copy(busy = true, message = null) }
            try {
                auth.signIn(activity) // the auth listener above kicks off the first sync
            } catch (_: GetCredentialCancellationException) {
                // The person closed the Google sheet; nothing to report.
            } catch (e: Exception) {
                Log.e(TAG, "Sign-in failed", e)
                setAccount { copy(message = "Couldn't sign in. Check your connection and try again.") }
            } finally {
                setAccount { copy(busy = false) }
            }
        }
    }

    /** Signing out wipes this device's copy, so first make certain everything is safely in the cloud. */
    fun signOut() {
        val current = uid ?: return
        viewModelScope.launch {
            setAccount { copy(busy = true, message = null) }
            try {
                syncJob?.join()
                prefs.setSyncCursor(current, progress.sync(FirestoreReadStore(current), prefs.syncCursor(current)))
                check(!progress.hasUnsynced())
                auth.signOut()
                progress.clearAll()
                prefs.clearSyncCursor(current)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "Sign-out blocked", e)
                setAccount { copy(message = "Can't sign out yet: your latest progress hasn't reached the cloud. Connect to the internet and try again.") }
            } finally {
                setAccount { copy(busy = false) }
            }
        }
    }

    /** Runs one sync at a time; a request made mid-sync triggers exactly one more pass afterwards. No-op when signed out. */
    fun requestSync() {
        val current = uid ?: return
        if (syncJob?.isActive == true) {
            resync = true
            return
        }
        syncJob = viewModelScope.launch {
            do {
                resync = false
                setAccount { copy(status = SyncStatus.Syncing, message = null) }
                try {
                    prefs.setSyncCursor(current, progress.sync(FirestoreReadStore(current), prefs.syncCursor(current)))
                    setAccount { copy(status = SyncStatus.Synced) }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Log.w(TAG, "Sync failed", e)
                    setAccount { copy(status = SyncStatus.Failed, message = "Couldn't reach the cloud. Your progress is safe on this phone and will sync later.") }
                }
            } while (resync)
        }
    }

    private fun setAccount(change: AccountState.() -> AccountState) {
        state = state.copy(account = state.account.change())
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
    val onSignIn: () -> Unit = {},
    val onSignOut: () -> Unit = {},
    val onSyncNow: () -> Unit = {},
    val onDismissNudge: () -> Unit = {},
)
