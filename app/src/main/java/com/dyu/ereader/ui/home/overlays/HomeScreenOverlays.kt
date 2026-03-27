package com.dyu.ereader.ui.home.overlays

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontWeight
import com.dyu.ereader.data.model.library.BookItem
import com.dyu.ereader.data.model.library.BookType
import com.dyu.ereader.ui.app.theme.UiTokens
import com.dyu.ereader.ui.components.dialogs.appDialogContainerColor
import com.dyu.ereader.ui.home.details.BookDetailsScreen
import com.dyu.ereader.ui.home.overlays.sheets.FilterBottomSheet
import com.dyu.ereader.ui.home.state.HomeUiState

@Composable
internal fun HomeDeleteDialog(
    pendingDeleteBook: BookItem?,
    onDeleteBook: (BookItem) -> Unit,
    onDismiss: () -> Unit
) {
    val book = pendingDeleteBook ?: return
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = UiTokens.SettingsCardShape,
        containerColor = appDialogContainerColor(),
        titleContentColor = MaterialTheme.colorScheme.onSurface,
        textContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        title = { Text("Delete Book") },
        text = { Text("Delete \"${book.title}\" from your library? This will remove the file.") },
        confirmButton = {
            TextButton(
                onClick = {
                    onDeleteBook(book)
                    onDismiss()
                }
            ) {
                Text("Delete", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun HomeFilterSheet(
    show: Boolean,
    uiState: HomeUiState,
    onDismiss: () -> Unit,
    onToggleType: (BookType) -> Unit,
    onToggleGenre: (String) -> Unit,
    onToggleLanguage: (String) -> Unit,
    onToggleYear: (String) -> Unit,
    onToggleCountry: (String) -> Unit,
    onSortOrderChanged: (com.dyu.ereader.ui.home.state.SortOrder) -> Unit,
    onToggleReadingStatus: (com.dyu.ereader.ui.home.state.ReadingStatus) -> Unit,
    onClearAdvancedFilters: () -> Unit
) {
    if (!show) return
    FilterBottomSheet(
        uiState = uiState,
        onDismiss = onDismiss,
        onToggleType = onToggleType,
        onToggleGenre = onToggleGenre,
        onToggleLanguage = onToggleLanguage,
        onToggleYear = onToggleYear,
        onToggleCountry = onToggleCountry,
        onSortOrderChanged = onSortOrderChanged,
        onToggleReadingStatus = onToggleReadingStatus,
        onReset = onClearAdvancedFilters
    )
}

@Composable
internal fun HomeDetailsOverlay(
    book: BookItem?,
    liquidGlassEnabled: Boolean,
    onClose: () -> Unit,
    onRead: () -> Unit
) {
    val detailsBook = book ?: return
    BookDetailsScreen(
        book = detailsBook,
        liquidGlassEnabled = liquidGlassEnabled,
        onClose = onClose,
        onRead = onRead
    )
}
