package com.example.biblepaceproject.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.biblepaceproject.data.Book
import com.example.biblepaceproject.data.BookCatalog
import com.example.biblepaceproject.data.ChapterId

/** The "card catalog": every book, grouped by testament, with per-chapter read marks. Tap a book to expand its chapters. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BooksScreen(state: ReaderState, actions: ReaderActions) {
    var expanded by rememberSaveable { mutableIntStateOf(state.book) }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("The Library", style = MaterialTheme.typography.titleLarge) },
                navigationIcon = { TextButton(onClick = actions.onShowReader) { Text("‹ Back") } },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = MaterialTheme.colorScheme.surface),
            )
        },
    ) { padding ->
        LazyColumn(Modifier.padding(padding)) {
            item(key = "progress") {
                Column(Modifier.padding(horizontal = 24.dp, vertical = 12.dp)) {
                    Text(
                        "${state.readChapters.size} of ${BookCatalog.totalChapters} chapters read",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    LinearProgressIndicator(
                        progress = { state.readChapters.size.toFloat() / BookCatalog.totalChapters },
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        color = MaterialTheme.colorScheme.secondary,
                        trackColor = MaterialTheme.colorScheme.outlineVariant,
                    )
                }
            }
            items(BookCatalog.books, key = { it.number }) { book ->
                if (book.number == 1 || book.number == 40) TestamentHeader(book.testament.label)
                BookRow(
                    book = book,
                    state = state,
                    isExpanded = expanded == book.number,
                    onToggle = { expanded = if (expanded == book.number) 0 else book.number },
                    onOpen = actions.onOpenChapter,
                )
            }
        }
    }
}

@Composable
private fun TestamentHeader(label: String) {
    Text(
        label.uppercase(),
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.secondary,
        fontWeight = FontWeight.Bold,
        letterSpacing = 2.sp,
        modifier = Modifier.padding(start = 24.dp, top = 20.dp, bottom = 4.dp),
    )
    HorizontalDivider(Modifier.padding(horizontal = 24.dp), color = MaterialTheme.colorScheme.outlineVariant)
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun BookRow(book: Book, state: ReaderState, isExpanded: Boolean, onToggle: () -> Unit, onOpen: (Int, Int) -> Unit) {
    val readCount = (1..book.chapters).count { ChapterId.of(book.number, it) in state.readChapters }
    Column {
        Row(
            Modifier.fillMaxWidth().clickable(onClick = onToggle).padding(horizontal = 24.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                book.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = if (book.number == state.book) FontWeight.Bold else FontWeight.Normal,
                modifier = Modifier.weight(1f),
            )
            Text("$readCount/${book.chapters}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.tertiary)
        }
        if (isExpanded) {
            FlowRow(
                Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                for (chapter in 1..book.chapters) {
                    ChapterCell(
                        chapter = chapter,
                        isRead = ChapterId.of(book.number, chapter) in state.readChapters,
                        isCurrent = book.number == state.book && chapter == state.chapter,
                        onClick = { onOpen(book.number, chapter) },
                    )
                }
            }
        }
    }
}

@Composable
private fun ChapterCell(chapter: Int, isRead: Boolean, isCurrent: Boolean, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(6.dp),
        color = if (isRead) colors.primaryContainer else colors.surface,
        border = BorderStroke(if (isCurrent) 2.dp else 1.dp, if (isCurrent) colors.secondary else colors.outlineVariant),
        modifier = Modifier.size(46.dp),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(chapter.toString(), style = MaterialTheme.typography.bodyMedium, color = colors.onSurface)
        }
    }
}
