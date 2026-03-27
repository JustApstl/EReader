package com.dyu.ereader.ui.reader.overlays.menus

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.dyu.ereader.data.local.db.HighlightEntity
import com.dyu.ereader.data.local.db.MarginNoteEntity
import com.dyu.ereader.ui.reader.controls.menus.ReaderDropdownHeader
import com.dyu.ereader.ui.reader.controls.menus.ReaderDropdownSectionLabel
import com.dyu.ereader.ui.reader.controls.menus.ReaderDropdownSurface
import com.dyu.ereader.ui.reader.controls.menus.SelectionActionRow
import com.dyu.ereader.ui.reader.controls.menus.dropdownWidthForLabels
import com.dyu.ereader.ui.reader.state.MarginNoteMenuState
import com.dyu.ereader.ui.reader.state.SelectionMenuState

@Composable
internal fun ReaderMarginNoteMenu(
    menu: MarginNoteMenuState?,
    marginNotes: List<MarginNoteEntity>,
    highlights: List<HighlightEntity>,
    screenWidthDp: Dp,
    screenHeightDp: Dp,
    onRequestEditNote: (SelectionMenuState) -> Unit,
    onRemoveMarginNote: (MarginNoteEntity) -> Unit,
    onRemoveHighlight: (Long) -> Unit,
    onDismissMenus: () -> Unit
) {
    val menuState = menu ?: return
    val popupPositionProvider = rememberReaderMenuPositionProvider(
        x = menuState.x,
        y = menuState.y,
        screenWidthDp = screenWidthDp,
        screenHeightDp = screenHeightDp
    )
    val noteEntity = marginNotes.firstOrNull { it.id == menuState.noteId }
    val noteContent = noteEntity?.content?.trim().orEmpty()
    val relatedHighlight = noteEntity?.let { note ->
        highlights.firstOrNull { highlight -> highlight.selectionJson == note.cfi }
    }
    val noteLabels = buildList {
        if (noteEntity != null) add("Edit Note")
        if (noteEntity != null) add("Remove Note")
        if (relatedHighlight != null) add("Remove Highlight")
    }
    val noteMenuWidth = maxOf(
        dropdownWidthForLabels(if (noteLabels.isNotEmpty()) noteLabels else listOf("Note")),
        244.dp
    )

    Popup(
        popupPositionProvider = popupPositionProvider,
        onDismissRequest = onDismissMenus,
        properties = PopupProperties(focusable = true, dismissOnClickOutside = true)
    ) {
        ReaderDropdownSurface(width = noteMenuWidth, maxHeight = 260.dp) {
            ReaderDropdownHeader(
                title = "Note",
                subtitle = if (noteContent.isNotBlank()) "Attached to this highlight" else "No note content found",
                onClose = onDismissMenus
            )
            AnnotationPreviewCard(
                text = if (noteContent.isNotBlank()) noteContent else "Note not found."
            )
            ReaderDropdownSectionLabel(label = "Actions")
            val softError = MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
            val softErrorContainer = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.22f)
            val softErrorIconContainer = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.6f)
            if (noteEntity != null) {
                SelectionActionRow(
                    label = "Edit Note",
                    icon = Icons.Rounded.Edit,
                    supportingText = "Update the saved note"
                ) {
                    onRequestEditNote(
                        SelectionMenuState(
                            chapterAnchor = noteEntity.chapterAnchor,
                            selectionJson = noteEntity.cfi,
                            text = noteEntity.content,
                            x = menuState.x,
                            y = menuState.y
                        )
                    )
                    onDismissMenus()
                }
            }
            if (noteEntity != null) {
                SelectionActionRow(
                    label = "Remove Note",
                    icon = Icons.Rounded.Delete,
                    iconTint = softError,
                    textColor = softError,
                    containerColor = softErrorContainer,
                    iconContainerColor = softErrorIconContainer,
                    supportingText = "Delete this note only"
                ) {
                    onRemoveMarginNote(noteEntity)
                    onDismissMenus()
                }
            }
            if (relatedHighlight != null) {
                SelectionActionRow(
                    label = "Remove Highlight",
                    icon = Icons.Rounded.Delete,
                    iconTint = softError,
                    textColor = softError,
                    containerColor = softErrorContainer,
                    iconContainerColor = softErrorIconContainer,
                    supportingText = "Clear the linked highlight color"
                ) {
                    onRemoveHighlight(relatedHighlight.id)
                    onDismissMenus()
                }
            }
        }
    }
}

@Composable
private fun AnnotationPreviewCard(text: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 6,
            overflow = TextOverflow.Ellipsis
        )
    }
}
