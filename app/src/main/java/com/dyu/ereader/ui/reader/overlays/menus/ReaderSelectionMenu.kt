package com.dyu.ereader.ui.reader.overlays.menus

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.ColorLens
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import androidx.core.graphics.toColorInt
import com.dyu.ereader.data.local.db.HighlightEntity
import com.dyu.ereader.data.local.db.MarginNoteEntity
import com.dyu.ereader.ui.reader.controls.menus.ReaderDropdownHeader
import com.dyu.ereader.ui.reader.controls.menus.ReaderDropdownSectionLabel
import com.dyu.ereader.ui.reader.controls.menus.ReaderDropdownSurface
import com.dyu.ereader.ui.reader.controls.menus.SelectionActionRow
import com.dyu.ereader.ui.reader.controls.menus.dropdownWidthForLabels
import com.dyu.ereader.ui.reader.state.SelectionMenuState

@Composable
internal fun ReaderSelectionMenu(
    selection: SelectionMenuState?,
    highlights: List<HighlightEntity>,
    marginNotes: List<MarginNoteEntity>,
    screenWidthDp: Dp,
    screenHeightDp: Dp,
    highlightColors: List<String>,
    customHighlightColorInt: Int?,
    onCustomColorClick: () -> Unit,
    onAddHighlight: (chapterAnchor: String, selectionJson: String, text: String, color: String) -> Unit,
    onRemoveHighlight: (Long) -> Unit,
    onRequestAddNote: (SelectionMenuState, String) -> Unit,
    onRemoveMarginNote: (MarginNoteEntity) -> Unit,
    onStartListen: (String) -> Unit,
    onDismissMenus: () -> Unit
) {
    val selectionState = selection ?: return
    val context = LocalContext.current
    val popupPositionProvider = rememberReaderMenuPositionProvider(
        x = selectionState.x,
        y = selectionState.y,
        screenWidthDp = screenWidthDp,
        screenHeightDp = screenHeightDp
    )
    val existingHighlight = highlights.firstOrNull { it.selectionJson == selectionState.selectionJson }
    val existingNote = marginNotes.firstOrNull { it.cfi == selectionState.selectionJson }
    val selectionLabels = buildList {
        add("Define")
        add("Add Note")
        if (existingNote != null) add("Remove Note")
        add("Copy")
        add("Share")
        add("Read")
        if (existingHighlight != null) add("Remove Highlight")
    }
    val selectionMenuWidth = maxOf(dropdownWidthForLabels(selectionLabels), 236.dp)
    val customHighlightHex = customHighlightColorInt?.let {
        String.format("#%06X", 0xFFFFFF and it)
    }
    val activeHighlightHex = existingHighlight?.color?.trim()?.uppercase()
    val customIsSelected = !customHighlightHex.isNullOrBlank() &&
        activeHighlightHex == customHighlightHex.uppercase() &&
        highlightColors.none { it.equals(customHighlightHex, ignoreCase = true) }

    Popup(
        popupPositionProvider = popupPositionProvider,
        onDismissRequest = onDismissMenus,
        properties = PopupProperties(focusable = false, dismissOnClickOutside = true)
    ) {
        ReaderDropdownSurface(width = selectionMenuWidth, maxHeight = 320.dp) {
            ReaderDropdownHeader(
                title = if (existingHighlight != null || existingNote != null) "Annotation" else "Selection",
                subtitle = selectionState.text
            )

            ReaderDropdownSectionLabel(
                label = "Highlight",
                trailingLabel = if (existingHighlight != null) "Saved" else null
            )
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(highlightColors) { colorHex ->
                    val isSelected = activeHighlightHex == colorHex.uppercase()
                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .clip(CircleShape)
                            .background(Color(colorHex.toColorInt()))
                            .border(
                                border = BorderStroke(
                                    width = if (isSelected) 2.dp else 1.dp,
                                    color = if (isSelected) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.outlineVariant
                                    }
                                ),
                                shape = CircleShape
                            )
                            .clickable {
                                onAddHighlight(
                                    selectionState.chapterAnchor,
                                    selectionState.selectionJson,
                                    selectionState.text,
                                    colorHex
                                )
                            },
                        contentAlignment = Alignment.Center
                    ) {}
                }
                item {
                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .clip(CircleShape)
                            .background(customHighlightHex?.let { Color(it.toColorInt()) } ?: MaterialTheme.colorScheme.surfaceVariant)
                            .border(
                                BorderStroke(
                                    if (customIsSelected) 2.dp else 1.dp,
                                    if (customIsSelected) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)
                                    }
                                ),
                                CircleShape
                            )
                            .clickable { onCustomColorClick() },
                        contentAlignment = Alignment.Center
                    ) {
                        if (customHighlightHex == null) {
                            Icon(
                                Icons.Rounded.ColorLens,
                                contentDescription = "Custom color",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(13.dp)
                            )
                        }
                    }
                }
            }

            val softError = MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
            val softErrorContainer = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.22f)
            val softErrorIconContainer = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.6f)

            if (existingHighlight != null) {
                SelectionActionRow(
                    label = "Remove Highlight",
                    icon = Icons.Rounded.Delete,
                    iconTint = softError,
                    textColor = softError,
                    containerColor = softErrorContainer,
                    iconContainerColor = softErrorIconContainer,
                    supportingText = "Clear the saved highlight color"
                ) {
                    onRemoveHighlight(existingHighlight.id)
                    onDismissMenus()
                }
            }

            ReaderDropdownSectionLabel(label = "Actions")

            SelectionActionRow(
                label = "Define",
                icon = Icons.AutoMirrored.Rounded.MenuBook,
                supportingText = "Look up this selection on the web"
            ) {
                openDefinitionSearch(context, selectionState.text)
                onDismissMenus()
            }
            SelectionActionRow(
                label = if (existingNote != null) "Edit Note" else "Add Note",
                icon = Icons.Rounded.EditNote,
                supportingText = if (existingNote != null) {
                    "Update the attached note"
                } else {
                    "Attach a note to this selection"
                }
            ) {
                val draft = existingNote?.content?.take(500) ?: selectionState.text.take(200)
                onRequestAddNote(selectionState, draft)
                onDismissMenus()
            }
            if (existingNote != null) {
                SelectionActionRow(
                    label = "Remove Note",
                    icon = Icons.Rounded.Delete,
                    iconTint = softError,
                    textColor = softError,
                    containerColor = softErrorContainer,
                    iconContainerColor = softErrorIconContainer,
                    supportingText = "Delete the attached note"
                ) {
                    onRemoveMarginNote(existingNote)
                    onDismissMenus()
                }
            }
            SelectionActionRow(
                label = "Copy",
                icon = Icons.Rounded.ContentCopy,
                supportingText = "Copy the selected text"
            ) {
                copySelection(context, selectionState.text)
                onDismissMenus()
            }
            SelectionActionRow(
                label = "Share",
                icon = Icons.Rounded.Share,
                supportingText = "Send the selection to another app"
            ) {
                shareSelection(context, selectionState.text)
                onDismissMenus()
            }
            SelectionActionRow(
                label = "Read",
                icon = Icons.AutoMirrored.Rounded.VolumeUp,
                supportingText = "Listen to this selection"
            ) {
                onDismissMenus()
                onStartListen(selectionState.text)
            }
        }
    }
}

private fun openDefinitionSearch(context: Context, text: String) {
    val query = Uri.encode("define $text")
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/search?q=$query"))
    context.startActivity(intent)
}

private fun copySelection(context: Context, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText("EReader", text))
}

private fun shareSelection(context: Context, text: String) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
    }
    context.startActivity(Intent.createChooser(intent, "Share text"))
}
