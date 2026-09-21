package com.example.biblepaceproject

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.example.biblepaceproject.ui.BooksScreen
import com.example.biblepaceproject.ui.ReaderActions
import com.example.biblepaceproject.ui.ReaderScreen
import com.example.biblepaceproject.ui.ReaderViewModel
import com.example.biblepaceproject.ui.Screen
import com.example.biblepaceproject.ui.theme.BiblePaceProjectTheme

class MainActivity : ComponentActivity() {
    private val viewModel: ReaderViewModel by viewModels()

    override fun onStart() {
        super.onStart()
        viewModel.requestSync()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            BiblePaceProjectTheme {
                val state = viewModel.state
                val actions = ReaderActions(
                    onShowBooks = viewModel::showBooks,
                    onShowReader = viewModel::showReader,
                    onOpenChapter = viewModel::openChapter,
                    onSelectVersion = viewModel::selectVersion,
                    onNext = viewModel::next,
                    onPrevious = viewModel::previous,
                    onToggleRead = viewModel::toggleRead,
                    onSignIn = { viewModel.signIn(this@MainActivity) },
                    onSignOut = viewModel::signOut,
                    onSyncNow = viewModel::requestSync,
                    onDismissNudge = viewModel::dismissNudge,
                )
                BackHandler(enabled = state.screen == Screen.Books) { viewModel.showReader() }
                when (state.screen) {
                    Screen.Reader -> ReaderScreen(state, actions)
                    Screen.Books -> BooksScreen(state, actions)
                }
            }
        }
    }
}
