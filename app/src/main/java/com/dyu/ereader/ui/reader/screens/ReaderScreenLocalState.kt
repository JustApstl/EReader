package com.dyu.ereader.ui.reader.screens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.dyu.ereader.ui.reader.state.SelectionMenuState

internal enum class ReaderDockedPanel {
    SETTINGS,
    MENU,
    SEARCH,
    LISTEN,
    ACCESSIBILITY,
    ANALYTICS,
    EXPORT
}

@Stable
internal class ReaderScreenLocalState {
    var showChrome by mutableStateOf(false)
    var dockedPanel by mutableStateOf<ReaderDockedPanel?>(null)
    var showAddNoteDialog by mutableStateOf(false)
    var showHighlightColorPicker by mutableStateOf(false)
    var noteDraft by mutableStateOf("")
    var noteTargetSelection by mutableStateOf<SelectionMenuState?>(null)
    var customHighlightColorInt by mutableStateOf<Int?>(null)
    var highlightOriginalColor by mutableStateOf<Int?>(null)

    val hasDockedPanel: Boolean
        get() = dockedPanel != null

    val showSettings: Boolean
        get() = dockedPanel == ReaderDockedPanel.SETTINGS

    val showChapterSheet: Boolean
        get() = dockedPanel == ReaderDockedPanel.MENU

    val showSearchDialog: Boolean
        get() = dockedPanel == ReaderDockedPanel.SEARCH

    val showAccessibilitySettings: Boolean
        get() = dockedPanel == ReaderDockedPanel.ACCESSIBILITY

    val showAnalytics: Boolean
        get() = dockedPanel == ReaderDockedPanel.ANALYTICS

    val showExportDialog: Boolean
        get() = dockedPanel == ReaderDockedPanel.EXPORT

    fun showDockedPanel(panel: ReaderDockedPanel) {
        dockedPanel = panel
    }

    fun toggleDockedPanel(panel: ReaderDockedPanel) {
        dockedPanel = if (dockedPanel == panel) null else panel
    }

    fun dismissDockedPanel() {
        dockedPanel = null
    }

    fun clearNoteEditor() {
        showAddNoteDialog = false
        noteTargetSelection = null
        noteDraft = ""
    }
}

@Composable
internal fun rememberReaderScreenLocalState(): ReaderScreenLocalState {
    return remember { ReaderScreenLocalState() }
}
