package com.dyu.ereader.ui.reader.overlays.menus

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import com.dyu.ereader.data.local.db.HighlightEntity
import com.dyu.ereader.data.local.db.MarginNoteEntity
import com.dyu.ereader.ui.reader.state.HighlightMenuState
import com.dyu.ereader.ui.reader.state.MarginNoteMenuState
import com.dyu.ereader.ui.reader.state.SelectionMenuState

@Composable
internal fun ReaderSelectionMenus(
    selection: SelectionMenuState?,
    highlightMenu: HighlightMenuState?,
    marginNoteMenu: MarginNoteMenuState?,
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
    onRequestEditNote: (SelectionMenuState) -> Unit,
    onRemoveMarginNote: (MarginNoteEntity) -> Unit,
    onStartListen: (String) -> Unit,
    onDismissMenus: () -> Unit
) {
    ReaderSelectionMenu(
        selection = selection,
        highlights = highlights,
        marginNotes = marginNotes,
        screenWidthDp = screenWidthDp,
        screenHeightDp = screenHeightDp,
        highlightColors = highlightColors,
        customHighlightColorInt = customHighlightColorInt,
        onCustomColorClick = onCustomColorClick,
        onAddHighlight = onAddHighlight,
        onRemoveHighlight = onRemoveHighlight,
        onRequestAddNote = onRequestAddNote,
        onRemoveMarginNote = onRemoveMarginNote,
        onStartListen = onStartListen,
        onDismissMenus = onDismissMenus
    )

    ReaderHighlightMenu(
        menu = highlightMenu,
        screenWidthDp = screenWidthDp,
        screenHeightDp = screenHeightDp,
        onRemoveHighlight = onRemoveHighlight,
        onDismissMenus = onDismissMenus
    )

    ReaderMarginNoteMenu(
        menu = marginNoteMenu,
        marginNotes = marginNotes,
        highlights = highlights,
        screenWidthDp = screenWidthDp,
        screenHeightDp = screenHeightDp,
        onRequestEditNote = onRequestEditNote,
        onRemoveMarginNote = onRemoveMarginNote,
        onRemoveHighlight = onRemoveHighlight,
        onDismissMenus = onDismissMenus
    )
}
