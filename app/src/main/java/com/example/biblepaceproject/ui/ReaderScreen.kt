package com.example.biblepaceproject.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.clickable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.biblepaceproject.data.BookCatalog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderScreen(state: ReaderState, actions: ReaderActions) {
    var showVersions by remember { mutableStateOf(false) }
    var showAccount by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            Column {
                CenterAlignedTopAppBar(
                    title = {
                        TextButton(onClick = actions.onShowBooks) {
                            Text("${state.bookName} ${state.chapter}", style = MaterialTheme.typography.titleLarge)
                            Icon(Icons.Filled.ArrowDropDown, contentDescription = "Open the library")
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = { showAccount = true }) {
                            Icon(
                                if (state.account.signedIn) Icons.Filled.AccountCircle else Icons.Outlined.AccountCircle,
                                contentDescription = "Account and backup",
                            )
                        }
                    },
                    actions = {
                        TextButton(onClick = { showVersions = true }) {
                            Text(state.version?.abbreviation ?: "—", fontWeight = FontWeight.Bold)
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = MaterialTheme.colorScheme.surface),
                )
                LinearProgressIndicator(
                    progress = { state.readChapters.size.toFloat() / BookCatalog.totalChapters },
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.secondary,
                    trackColor = MaterialTheme.colorScheme.outlineVariant,
                )
                if (state.account.showNudge) {
                    Surface(color = MaterialTheme.colorScheme.primaryContainer) {
                        Row(Modifier.fillMaxWidth().padding(start = 24.dp, end = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "Keep your progress safe if you change phones or reinstall.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.weight(1f).padding(vertical = 8.dp),
                            )
                            TextButton(onClick = { showAccount = true }) { Text("Sign in") }
                            TextButton(onClick = actions.onDismissNudge) { Text("Not now") }
                        }
                    }
                }
            }
        },
        bottomBar = { ChapterBar(state, actions) },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
            when {
                state.loading && state.verses.isEmpty() -> CircularProgressIndicator(color = MaterialTheme.colorScheme.secondary)
                state.error != null -> Text(state.error, color = MaterialTheme.colorScheme.error)
                else -> key(state.book, state.chapter) { ChapterText(state) }
            }
        }
    }

    if (showAccount) AccountSheet(state.account, actions, onDismiss = { showAccount = false })

    if (showVersions) {
        VersionSheet(
            state = state,
            onSelect = {
                actions.onSelectVersion(it)
                showVersions = false
            },
            onDismiss = { showVersions = false },
        )
    }
}

@Composable
private fun ChapterText(state: ReaderState) {
    // The caller keys this on book+chapter, so a new chapter starts at the top while switching versions keeps your scroll position.
    val listState = rememberLazyListState()
    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        item(key = "header") {
            Column(Modifier.fillMaxWidth().padding(bottom = 16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(state.bookName.uppercase(), style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary)
                Text("Chapter ${state.chapter}", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.tertiary)
                Ornament(Modifier.padding(top = 12.dp))
            }
        }
        // Some versions leave a verse empty on purpose (e.g. Matthew 17:21 in the ASV, where manuscripts differ). Skip it but keep numbering.
        itemsIndexed(state.verses, key = { i, _ -> i }) { index, text ->
            if (text.isBlank()) return@itemsIndexed
            val verse = buildAnnotatedString {
                withStyle(SpanStyle(fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary, baselineShift = BaselineShift.Superscript)) {
                    append("${index + 1} ")
                }
                append(text)
            }
            Text(verse, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onBackground)
        }
        item(key = "footer") {
            Box(Modifier.fillMaxWidth().padding(vertical = 24.dp), contentAlignment = Alignment.Center) { Ornament() }
        }
    }
}

/** A short gilt rule, used as a quiet chapter ornament. */
@Composable
private fun Ornament(modifier: Modifier = Modifier) {
    Box(modifier.width(48.dp).height(2.dp).background(MaterialTheme.colorScheme.secondary))
}

@Composable
private fun ChapterBar(state: ReaderState, actions: ReaderActions) {
    Surface(color = MaterialTheme.colorScheme.surfaceContainer, tonalElevation = 2.dp) {
        Column(Modifier.navigationBarsPadding()) {
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedButton(onClick = actions.onPrevious, enabled = state.hasPrevious, modifier = Modifier.semantics { contentDescription = "Previous chapter" }) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = null) }
                if (state.isChapterRead) {
                    OutlinedButton(onClick = actions.onToggleRead) {
                        Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                        Text("Read", modifier = Modifier.padding(start = 6.dp))
                    }
                } else {
                    Button(onClick = actions.onToggleRead) { Text("Mark as read") }
                }
                OutlinedButton(onClick = actions.onNext, enabled = state.hasNext, modifier = Modifier.semantics { contentDescription = "Next chapter" }) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null) }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun VersionSheet(state: ReaderState, onSelect: (String) -> Unit, onDismiss: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = MaterialTheme.colorScheme.surfaceContainer) {
        Column(Modifier.padding(horizontal = 24.dp).padding(bottom = 24.dp)) {
            Text("Choose a version", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
            Text(
                "All bundled texts are in the public domain and work offline.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier.padding(top = 4.dp, bottom = 8.dp),
            )
            state.versions.forEach { version ->
                Row(
                    Modifier.fillMaxWidth().clickable { onSelect(version.id) }.padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RadioButton(selected = version.id == state.versionId, onClick = { onSelect(version.id) })
                    Column(Modifier.padding(start = 8.dp)) {
                        Text("${version.name} (${version.abbreviation})", style = MaterialTheme.typography.titleMedium)
                        Text(version.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

private val previewState = ReaderState(
    versions = listOf(
        com.example.biblepaceproject.data.BibleVersion("kjv", "King James Version", "KJV", "1769 Oxford edition."),
    ),
    book = 43,
    chapter = 3,
    verses = listOf(
        "There was a man of the Pharisees, named Nicodemus, a ruler of the Jews:",
        "The same came to Jesus by night, and said unto him, Rabbi, we know that thou art a teacher come from God.",
    ),
    loading = false,
)

@androidx.compose.ui.tooling.preview.Preview(showBackground = true, name = "Reader (light)")
@Composable
private fun ReaderPreviewLight() = com.example.biblepaceproject.ui.theme.BiblePaceProjectTheme(darkTheme = false) {
    ReaderScreen(previewState, ReaderActions())
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true, name = "Reader (dark)")
@Composable
private fun ReaderPreviewDark() = com.example.biblepaceproject.ui.theme.BiblePaceProjectTheme(darkTheme = true) {
    ReaderScreen(previewState, ReaderActions())
}
