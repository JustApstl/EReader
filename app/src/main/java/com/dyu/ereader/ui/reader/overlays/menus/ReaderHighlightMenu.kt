package com.dyu.ereader.ui.reader.overlays.menus

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.dyu.ereader.ui.reader.controls.menus.ReaderDropdownHeader
import com.dyu.ereader.ui.reader.controls.menus.ReaderDropdownSurface
import com.dyu.ereader.ui.reader.controls.menus.SelectionActionRow
import com.dyu.ereader.ui.reader.controls.menus.dropdownWidthForLabels
import com.dyu.ereader.ui.reader.state.HighlightMenuState

@Composable
internal fun ReaderHighlightMenu(
    menu: HighlightMenuState?,
    screenWidthDp: Dp,
    screenHeightDp: Dp,
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
    val highlightMenuWidth = maxOf(dropdownWidthForLabels(listOf("Remove Highlight")), 208.dp)

    Popup(
        popupPositionProvider = popupPositionProvider,
        onDismissRequest = onDismissMenus,
        properties = PopupProperties(focusable = false, dismissOnClickOutside = true)
    ) {
        ReaderDropdownSurface(width = highlightMenuWidth, maxHeight = 144.dp) {
            ReaderDropdownHeader(
                title = "Highlight",
                subtitle = "Saved annotation on this text"
            )
            val softError = MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
            val softErrorContainer = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.22f)
            val softErrorIconContainer = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.6f)
            SelectionActionRow(
                label = "Remove Highlight",
                icon = Icons.Rounded.Delete,
                iconTint = softError,
                textColor = softError,
                containerColor = softErrorContainer,
                iconContainerColor = softErrorIconContainer,
                supportingText = "Clear the highlight from this text"
            ) {
                onRemoveHighlight(menuState.highlightId)
                onDismissMenus()
            }
        }
    }
}
