package com.dyu.ereader.ui.reader

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.PopupProperties
import androidx.core.graphics.toColorInt
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import coil.compose.AsyncImage
import com.dyu.ereader.data.database.MarginNoteEntity
import com.dyu.ereader.data.model.BookType
import com.dyu.ereader.data.model.FontColorTheme
import com.dyu.ereader.data.model.ImageFilter
import com.dyu.ereader.data.model.ReaderFont
import com.dyu.ereader.data.model.ReaderSettings
import com.dyu.ereader.data.model.ReaderTheme
import com.dyu.ereader.data.model.ReadingMode
import com.dyu.ereader.data.model.NavigationBarStyle
import com.dyu.ereader.data.model.PageTransitionStyle
import com.dyu.ereader.data.model.ReaderControl
import com.dyu.ereader.data.model.getBackgroundColor
import com.dyu.ereader.data.model.getColor
import com.dyu.ereader.data.model.SearchResult
import com.dyu.ereader.ui.search.SearchDialog
import com.dyu.ereader.ui.tts.TTSControls
import com.dyu.ereader.ui.accessibility.AccessibilitySettings
import com.dyu.ereader.ui.analytics.AnalyticsDashboard
import com.dyu.ereader.ui.export.ExportDialog
import com.dyu.ereader.ui.home.HomeDisplayPreferences
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.gestures.transformable
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderScreen(
    bookUri: String,
    bookType: BookType,
    uiState: ReaderUiState,
    isDarkTheme: Boolean,
    onBack: () -> Unit,
    onHome: () -> Unit,
    onReaderThemeChange: (ReaderTheme) -> Unit,
    onModeChanged: (ReadingMode) -> Unit,
    onFontColorThemeChange: (FontColorTheme) -> Unit,
    onAutoFontColorToggle: (Boolean) -> Unit,
    onFocusTextToggle: (Boolean) -> Unit,
    onFocusTextBoldnessChange: (Int) -> Unit,
    onFocusTextColorChange: (Int?) -> Unit,
    onFocusModeToggle: (Boolean) -> Unit,
    onHideStatusBarToggle: (Boolean) -> Unit,
    onFontSizeChange: (Float) -> Unit,
    onLineSpacingChange: (Float) -> Unit,
    onMarginChange: (Float) -> Unit,
    onFontChange: (ReaderFont) -> Unit,
    onProgressChanged: (Float, String?) -> Unit,
    onProgressJumpRequest: (Float) -> Unit,
    onPaginationChanged: (Int, Int) -> Unit,
    onNextChapter: () -> Unit,
    onPreviousChapter: () -> Unit,
    onJumpToChapter: (String) -> Unit,
    onJumpConsumed: () -> Unit,
    onAddHighlight: (chapterAnchor: String, selectionJson: String, text: String, color: String) -> Unit,
    onAddBookmark: (chapterAnchor: String, cfi: String, title: String?) -> Unit,
    onAddBookmarkAtCurrent: () -> Unit,
    onAddMarginNote: (chapterAnchor: String, cfi: String, content: String, color: String) -> Unit,
    onRemoveHighlight: (Long) -> Unit,
    onRemoveMarginNote: (MarginNoteEntity) -> Unit,
    onChaptersLoaded: (List<Chapter>) -> Unit,
    onResetSettings: () -> Unit,
    onRetry: () -> Unit,
    onTextSelected: (String, String, String, Float, Float) -> Unit,
    onHighlightClick: (Long, Float, Float) -> Unit,
    onMarginNoteClick: (Long, Float, Float) -> Unit,
    onImageClick: (String?) -> Unit,
    onDismissMenus: () -> Unit,
    onCustomColorSelected: (Int) -> Unit,
    onCustomFontColorSelected: (Int) -> Unit,
    onCustomFontSelected: (String?) -> Unit,
    onClearCustomFont: () -> Unit = {},
    onBackgroundImageSelected: (String?) -> Unit,
    onBackgroundImageBlurChange: (Float) -> Unit,
    onBackgroundImageOpacityChange: (Float) -> Unit,
    onImageFilterChange: (ImageFilter) -> Unit,
    onUsePublisherStyleToggle: (Boolean) -> Unit,
    onUnderlineLinksToggle: (Boolean) -> Unit,
    onTextShadowToggle: (Boolean) -> Unit,
    onTextShadowColorChange: (Int?) -> Unit,
    onNavigationBarStyleChange: (NavigationBarStyle) -> Unit,
    onSearch: (String) -> Unit,
    searchResults: List<SearchResult>,
    isSearching: Boolean,
    onTextExtracted: (String) -> Unit = {},
    onPageTurn3dToggle: (Boolean) -> Unit = {},
    onPageTransitionStyleChange: (PageTransitionStyle) -> Unit = {},
    onLoadingProgressChange: (Float) -> Unit = {},
    displayPrefs: HomeDisplayPreferences = HomeDisplayPreferences(),
    modifier: Modifier = Modifier,
    onHighlightContextClick: (Long, Float, Float) -> Unit = { _, _, _ -> },
    onStartTTS: (String) -> Unit = {}
) {
    val context = LocalContext.current
    var showChrome by remember { mutableStateOf(false) } 
    var showSettings by remember { mutableStateOf(false) }
    var showChapterSheet by remember { mutableStateOf(false) }
    var showSearchDialog by remember { mutableStateOf(false) }
    var showTTSControls by remember { mutableStateOf(false) }
    var showAccessibilitySettings by remember { mutableStateOf(false) }
    var showAnalytics by remember { mutableStateOf(false) }
    var showExportDialog by remember { mutableStateOf(false) }
    var showAddNoteDialog by remember { mutableStateOf(false) }
    var showHighlightColorPicker by remember { mutableStateOf(false) }
    var noteDraft by remember { mutableStateOf("") }
    var noteTargetSelection by remember { mutableStateOf<SelectionMenuState?>(null) }
    var customHighlightColorInt by remember { mutableStateOf<Int?>(null) }

    val isFocusMode = uiState.settings.focusMode
    val hideStatusBar = uiState.settings.hideStatusBar
    val navBarStyle = uiState.settings.navBarStyle

    val readerBg = uiState.settings.readerTheme.getBackgroundColor(isDarkTheme, uiState.settings.customBackgroundColor)
    val isLightBg = readerBg.luminance() > 0.5f
    val contrastingContentColor = if (isLightBg) Color.Black else Color.White

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            val controller = WindowCompat.getInsetsController(window, view)
            controller.isAppearanceLightStatusBars = isLightBg
        }
    }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            context.contentResolver.takePersistableUriPermission(it, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            onBackgroundImageSelected(it.toString())
        }
    }

    val fontPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            context.contentResolver.takePersistableUriPermission(it, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            onCustomFontSelected(it.toString())
        }
    }

    LaunchedEffect(isFocusMode) {
        if (isFocusMode) {
            showChrome = false
            showSettings = false
        }
    }

    LaunchedEffect(hideStatusBar) {
        val window = (context as? ComponentActivity)?.window ?: return@LaunchedEffect
        val insetsController = WindowCompat.getInsetsController(window, window.decorView)
        insetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE

        if (hideStatusBar) {
            insetsController.hide(WindowInsetsCompat.Type.statusBars())
        } else {
            insetsController.show(WindowInsetsCompat.Type.statusBars())
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            val window = (context as? ComponentActivity)?.window ?: return@onDispose
            val insetsController = WindowCompat.getInsetsController(window, window.decorView)
            insetsController.show(WindowInsetsCompat.Type.statusBars())
        }
    }

    val baseHighlightColors = listOf(
        "#FFF176", // Yellow
        "#A5D6A7", // Green
        "#80DEEA", // Cyan
        "#CE93D8"  // Purple
    )
    val customHighlightHex = customHighlightColorInt?.let {
        String.format("#%06X", 0xFFFFFF and it)
    }
    val highlightColors = baseHighlightColors

    val screenWidthDp = LocalConfiguration.current.screenWidthDp.dp
    val screenHeightDp = LocalConfiguration.current.screenHeightDp.dp

    if (showSettings) {
        ReaderSettingsSheet(
            settings = uiState.settings,
            isDarkTheme = isDarkTheme,
            onDismiss = { showSettings = false },
            onReadingModeChange = onModeChanged,
            onThemeChange = onReaderThemeChange,
            onFontColorThemeChange = onFontColorThemeChange,
            onAutoFontColorToggle = onAutoFontColorToggle,
            onFocusTextToggle = onFocusTextToggle,
            onFocusTextBoldnessChange = onFocusTextBoldnessChange,
            onFocusTextColorChange = onFocusTextColorChange,
            onFocusModeToggle = onFocusModeToggle,
            onHideStatusBarToggle = onHideStatusBarToggle,
            onFontSizeChange = onFontSizeChange,
            onLineSpacingChange = onLineSpacingChange,
            onMarginChange = onMarginChange,
            onFontChange = onFontChange,
            onResetSettings = onResetSettings,
            onCustomColorSelected = onCustomColorSelected,
            onCustomFontColorSelected = onCustomFontColorSelected,
            onPickCustomFont = { fontPickerLauncher.launch(arrayOf("font/*", "application/octet-stream", "*/*")) },
            onClearCustomFont = onClearCustomFont,
            onPickBackgroundImage = { imagePickerLauncher.launch(arrayOf("image/*")) },
            onBackgroundImageBlurChange = onBackgroundImageBlurChange,
            onBackgroundImageOpacityChange = onBackgroundImageOpacityChange,
            onImageFilterChange = onImageFilterChange,
            onUsePublisherStyleToggle = onUsePublisherStyleToggle,
            onUnderlineLinksToggle = onUnderlineLinksToggle,
            onTextShadowToggle = onTextShadowToggle,
            onTextShadowColorChange = onTextShadowColorChange,
            onNavigationBarStyleChange = onNavigationBarStyleChange,
            onPageTurn3dToggle = onPageTurn3dToggle,
            onPageTransitionStyleChange = onPageTransitionStyleChange
        )
    }

    if (showChapterSheet) {
        TableOfContentsSheet(
            chapters = uiState.chapters,
            currentChapterIndex = uiState.currentChapterIndex,
            onChapterSelected = { index ->
                onJumpToChapter(uiState.chapters[index].href)
                showChapterSheet = false
            },
            onDismiss = { showChapterSheet = false }
        )
    }

    uiState.zoomImageUrl?.let { url ->
        ImageZoomDialog(
            url = url,
            onDismiss = { onImageClick(null) }
        )
    }

    if (showSearchDialog) {
        SearchDialog(
            onDismiss = { showSearchDialog = false },
            onResultSelected = { result ->
                onJumpToChapter(result.chapterHref)
                showSearchDialog = false
            },
            onSearch = onSearch,
            results = searchResults,
            isSearching = isSearching
        )
    }

    if (showTTSControls) {
        ModalBottomSheet(
            onDismissRequest = { showTTSControls = false },
            dragHandle = { BottomSheetDefaults.DragHandle() },
            containerColor = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp
        ) {
            TTSControls(
                modifier = Modifier.padding(bottom = 32.dp)
            )
        }
    }

    if (showAccessibilitySettings) {
        ModalBottomSheet(
            onDismissRequest = { showAccessibilitySettings = false },
            dragHandle = { BottomSheetDefaults.DragHandle() },
            containerColor = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp
        ) {
            AccessibilitySettings(
                modifier = Modifier.padding(bottom = 32.dp)
            )
        }
    }

    if (showAnalytics) {
        ModalBottomSheet(
            onDismissRequest = { showAnalytics = false },
            dragHandle = { BottomSheetDefaults.DragHandle() },
            containerColor = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp
        ) {
            AnalyticsDashboard(
                modifier = Modifier.padding(bottom = 32.dp)
            )
        }
    }

    if (showExportDialog) {
        ExportDialog(
            onDismiss = { showExportDialog = false }
        )
    }

    if (showAddNoteDialog) {
        AlertDialog(
            onDismissRequest = {
                showAddNoteDialog = false
                noteTargetSelection = null
                noteDraft = ""
            },
            title = { Text("Add Note") },
            text = {
                OutlinedTextField(
                    value = noteDraft,
                    onValueChange = { noteDraft = it },
                    placeholder = { Text("Write your note...") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    maxLines = 6
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val target = noteTargetSelection
                        if (target != null && noteDraft.isNotBlank()) {
                            onAddMarginNote(
                                target.chapterAnchor,
                                target.selectionJson,
                                noteDraft.trim(),
                                "#FFF59D"
                            )
                        }
                        showAddNoteDialog = false
                        noteTargetSelection = null
                        noteDraft = ""
                        onDismissMenus()
                    },
                    enabled = noteDraft.isNotBlank()
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showAddNoteDialog = false
                        noteTargetSelection = null
                        noteDraft = ""
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showHighlightColorPicker) {
        ColorPickerDialog(
            onDismiss = { showHighlightColorPicker = false },
            onColorSelected = { customHighlightColorInt = it },
            initialColor = customHighlightColorInt ?: 0xFFF176.toInt()
        )
    }

    Box(modifier = modifier.fillMaxSize().background(readerBg)) {
        Box(
            modifier = Modifier
                .fillMaxSize()
        ) {
            if (uiState.settings.readerTheme == ReaderTheme.IMAGE && uiState.settings.backgroundImageUri != null) {
                AsyncImage(
                    model = uiState.settings.backgroundImageUri,
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer(alpha = uiState.settings.backgroundImageOpacity)
                        .blur(uiState.settings.backgroundImageBlur.dp),
                    contentScale = ContentScale.Crop
                )
            }

            if (uiState.isLoading) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    CircularProgressIndicator(
                        progress = { if (uiState.loadingProgress > 0) uiState.loadingProgress else 0f },
                        strokeWidth = 3.dp,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(
                        "Opening book...", 
                        style = MaterialTheme.typography.bodyMedium, 
                        color = contrastingContentColor.copy(alpha = 0.7f)
                    )
                }
            } else if (uiState.errorMessage != null) {
                Column(
                    modifier = Modifier.align(Alignment.Center).padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        uiState.errorMessage,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center
                    )
                    Button(onClick = onRetry, shape = RoundedCornerShape(12.dp)) {
                        Text("Retry")
                    }
                }
            } else {
                if (bookType == BookType.PDF) {
                    PdfReaderScreen(
                        uri = bookUri,
                        settings = uiState.settings,
                        onProgressChanged = { p -> onProgressChanged(p, null) },
                        onPaginationChanged = onPaginationChanged,
                        modifier = Modifier.fillMaxSize().statusBarsPadding()
                    )
                } else {
                    key(uiState.settings.readingMode) {
                        EpubJsReader(
                            bookUri = bookUri,
                            initialProgress = uiState.progress,
                            initialCfi = uiState.savedCfi,
                            settings = uiState.settings,
                            highlights = uiState.highlights,
                            marginNotes = uiState.marginNotes,
                            isDarkTheme = isDarkTheme,
                            onProgressChanged = onProgressChanged,
                            onToggleMenu = { if (!isFocusMode) showChrome = !showChrome },
                            onTextSelected = onTextSelected,
                            onHighlightClicked = onHighlightClick,
                            onMarginNoteClicked = onMarginNoteClick,
                            isSelectionMenuVisible = uiState.selectionMenu != null,
                            isHighlightMenuVisible = uiState.highlightMenu != null,
                            isMarginNoteMenuVisible = uiState.marginNoteMenu != null,
                            onChaptersLoaded = onChaptersLoaded,
                            onTextExtracted = onTextExtracted,
                            onPaginationChanged = onPaginationChanged,
                            onStartTTS = onStartTTS,
                            onLoadingProgressChange = onLoadingProgressChange,
                            pendingJumpHref = uiState.pendingAnchorJump,
                            pendingProgressJump = uiState.pendingProgressJump,
                            requestTextExtraction = uiState.requestTextExtraction,
                            onJumpConsumed = onJumpConsumed,
                            modifier = Modifier.fillMaxSize().statusBarsPadding()
                        )
                    }
                }
            }

            uiState.selectionMenu?.let { selection ->
                val density = LocalDensity.current
                val marginPx = with(density) { 8.dp.roundToPx() }
                val verticalGapPx = with(density) { 3.dp.roundToPx() }
                val screenWidthPx = with(density) { screenWidthDp.roundToPx() }
                val screenHeightPx = with(density) { screenHeightDp.roundToPx() }
                val anchorX = if (!selection.x.isNaN() && !selection.x.isInfinite()) selection.x.roundToInt() else screenWidthPx / 2
                val rawAnchorY = if (!selection.y.isNaN() && !selection.y.isInfinite()) selection.y.roundToInt() else screenHeightPx / 3
                val anchorY = rawAnchorY
                val popupPositionProvider = remember(anchorX, anchorY, marginPx, verticalGapPx) {
                    anchoredDropdownPositionProvider(
                        anchorX = anchorX,
                        anchorY = anchorY,
                        marginPx = marginPx,
                        verticalGapPx = verticalGapPx
                    )
                }
                val existingHighlight = uiState.highlights.firstOrNull { it.selectionJson == selection.selectionJson }
                val existingNote = uiState.marginNotes.firstOrNull { it.cfi == selection.selectionJson }
                val selectionLabels = buildList {
                    add("Define")
                    add("Note")
                    if (existingNote != null) add("Remove Note")
                    add("Copy")
                    add("Share")
                    add("Read")
                    add("Bookmark")
                    if (existingHighlight != null) add("Remove")
                }
                val selectionMenuWidth = dropdownWidthForLabels(selectionLabels)
                val colorRowWidth = (selectionMenuWidth - 40.dp).coerceIn(116.dp, 136.dp)

                Popup(
                    popupPositionProvider = popupPositionProvider,
                    onDismissRequest = onDismissMenus,
                    properties = PopupProperties(focusable = false, dismissOnClickOutside = true)
                ) {
                    ReaderDropdownSurface(width = selectionMenuWidth, maxHeight = 248.dp) {
                        Text(
                            text = "Colors",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        LazyRow(
                            modifier = Modifier
                                .width(colorRowWidth)
                                .padding(bottom = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            items(highlightColors) { colorHex ->
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clip(CircleShape)
                                        .background(Color(colorHex.toColorInt()))
                                        .border(BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant), CircleShape)
                                        .clickable {
                                            onAddHighlight(selection.chapterAnchor, selection.selectionJson, selection.text, colorHex)
                                        }
                                )
                            }
                            item {
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clip(CircleShape)
                                        .background(customHighlightHex?.let { Color(it.toColorInt()) } ?: MaterialTheme.colorScheme.surfaceVariant)
                                        .border(BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)), CircleShape)
                                        .clickable {
                                            showHighlightColorPicker = true
                                        },
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

                        if (existingHighlight != null) {
                            SelectionActionRow(
                                label = "Remove",
                                icon = Icons.Rounded.Delete,
                                iconTint = MaterialTheme.colorScheme.error,
                                textColor = MaterialTheme.colorScheme.error,
                                containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.32f),
                                iconContainerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.14f)
                            ) {
                                onRemoveHighlight(existingHighlight.id)
                                onDismissMenus()
                            }
                        }

                        HorizontalDivider(modifier = Modifier.padding(vertical = 2.dp))

                        SelectionActionRow(
                            label = "Define",
                            icon = Icons.AutoMirrored.Rounded.MenuBook
                        ) {
                            val query = Uri.encode("define ${selection.text}")
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/search?q=$query"))
                            context.startActivity(intent)
                            onDismissMenus()
                        }
                        SelectionActionRow(
                            label = "Note",
                            icon = Icons.Rounded.EditNote
                        ) {
                            noteTargetSelection = selection
                            noteDraft = existingNote?.content?.take(500) ?: selection.text.take(200)
                            showAddNoteDialog = true
                            onDismissMenus()
                        }
                        if (existingNote != null) {
                            SelectionActionRow(
                                label = "Remove Note",
                                icon = Icons.Rounded.Delete,
                                iconTint = MaterialTheme.colorScheme.error,
                                textColor = MaterialTheme.colorScheme.error,
                                containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.32f),
                                iconContainerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.14f)
                            ) {
                                onRemoveMarginNote(existingNote)
                                onDismissMenus()
                            }
                        }
                        SelectionActionRow(
                            label = "Copy",
                            icon = Icons.Rounded.ContentCopy
                        ) {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("EReader", selection.text))
                            onDismissMenus()
                        }
                        SelectionActionRow(
                            label = "Share",
                            icon = Icons.Rounded.Share
                        ) {
                            val intent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, selection.text)
                            }
                            context.startActivity(Intent.createChooser(intent, "Share text"))
                            onDismissMenus()
                        }
                        SelectionActionRow(
                            label = "Read",
                            icon = Icons.Rounded.VolumeUp
                        ) {
                            onStartTTS(selection.text)
                            showTTSControls = true
                            onDismissMenus()
                        }
                        SelectionActionRow(
                            label = "Bookmark",
                            icon = Icons.Rounded.Bookmark
                        ) {
                            onAddBookmark(
                                selection.chapterAnchor,
                                selection.selectionJson,
                                selection.text.take(80)
                            )
                            onDismissMenus()
                        }
                    }
                }
            }

            uiState.highlightMenu?.let { menu ->
                val density = LocalDensity.current
                val marginPx = with(density) { 8.dp.roundToPx() }
                val verticalGapPx = with(density) { 3.dp.roundToPx() }
                val screenWidthPx = with(density) { screenWidthDp.roundToPx() }
                val screenHeightPx = with(density) { screenHeightDp.roundToPx() }
                val anchorX = if (!menu.x.isNaN() && !menu.x.isInfinite()) menu.x.roundToInt() else screenWidthPx / 2
                val rawAnchorY = if (!menu.y.isNaN() && !menu.y.isInfinite()) menu.y.roundToInt() else screenHeightPx / 3
                val anchorY = rawAnchorY
                val popupPositionProvider = remember(anchorX, anchorY, marginPx, verticalGapPx) {
                    anchoredDropdownPositionProvider(
                        anchorX = anchorX,
                        anchorY = anchorY,
                        marginPx = marginPx,
                        verticalGapPx = verticalGapPx
                    )
                }
                val highlightMenuWidth = dropdownWidthForLabels(listOf("Remove Annotation"))

                Popup(
                    popupPositionProvider = popupPositionProvider,
                    onDismissRequest = onDismissMenus,
                    properties = PopupProperties(focusable = false, dismissOnClickOutside = true)
                ) {
                    ReaderDropdownSurface(width = highlightMenuWidth, maxHeight = 120.dp) {
                        Text(
                            text = "Highlight",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        SelectionActionRow(
                            label = "Remove Annotation",
                            icon = Icons.Rounded.Delete,
                            iconTint = MaterialTheme.colorScheme.error,
                            textColor = MaterialTheme.colorScheme.error,
                            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.32f),
                            iconContainerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.14f)
                        ) {
                            onRemoveHighlight(menu.highlightId)
                            onDismissMenus()
                        }
                    }
                }
            }

            uiState.marginNoteMenu?.let { menu ->
                val density = LocalDensity.current
                val marginPx = with(density) { 8.dp.roundToPx() }
                val verticalGapPx = with(density) { 3.dp.roundToPx() }
                val screenWidthPx = with(density) { screenWidthDp.roundToPx() }
                val screenHeightPx = with(density) { screenHeightDp.roundToPx() }
                val anchorX = if (!menu.x.isNaN() && !menu.x.isInfinite()) menu.x.roundToInt() else screenWidthPx / 2
                val rawAnchorY = if (!menu.y.isNaN() && !menu.y.isInfinite()) menu.y.roundToInt() else screenHeightPx / 3
                val anchorY = rawAnchorY
                val popupPositionProvider = remember(anchorX, anchorY, marginPx, verticalGapPx) {
                    anchoredDropdownPositionProvider(
                        anchorX = anchorX,
                        anchorY = anchorY,
                        marginPx = marginPx,
                        verticalGapPx = verticalGapPx
                    )
                }
                val noteContent = uiState.marginNotes.firstOrNull { it.id == menu.noteId }?.content?.trim().orEmpty()
                val noteEntity = uiState.marginNotes.firstOrNull { it.id == menu.noteId }
                val relatedHighlight = noteEntity?.let { note ->
                    uiState.highlights.firstOrNull { highlight -> highlight.selectionJson == note.cfi }
                }
                val noteLabels = buildList {
                    if (noteEntity != null) add("Edit Note")
                    if (noteEntity != null) add("Remove Note")
                    if (relatedHighlight != null) add("Remove Annotation")
                }
                val noteMenuWidth = dropdownWidthForLabels(if (noteLabels.isNotEmpty()) noteLabels else listOf("Note"))

                Popup(
                    popupPositionProvider = popupPositionProvider,
                    onDismissRequest = onDismissMenus,
                    properties = PopupProperties(focusable = true, dismissOnClickOutside = true)
                ) {
                    ReaderDropdownSurface(width = noteMenuWidth, maxHeight = 230.dp) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Note",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            IconButton(
                                onClick = onDismissMenus,
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    Icons.Rounded.Close,
                                    contentDescription = "Close",
                                    modifier = Modifier.size(15.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Text(
                            text = if (noteContent.isNotBlank()) noteContent else "Note not found.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 8,
                            overflow = TextOverflow.Ellipsis
                        )
                        HorizontalDivider(modifier = Modifier.padding(vertical = 2.dp))
                        if (noteEntity != null) {
                            SelectionActionRow(
                                label = "Edit Note",
                                icon = Icons.Rounded.Edit
                            ) {
                                noteTargetSelection = SelectionMenuState(
                                    chapterAnchor = noteEntity.chapterAnchor,
                                    selectionJson = noteEntity.cfi,
                                    text = noteEntity.content,
                                    x = menu.x,
                                    y = menu.y
                                )
                                noteDraft = noteEntity.content
                                showAddNoteDialog = true
                                onDismissMenus()
                            }
                        }
                        if (noteEntity != null) {
                            SelectionActionRow(
                                label = "Remove Note",
                                icon = Icons.Rounded.Delete,
                                iconTint = MaterialTheme.colorScheme.error,
                                textColor = MaterialTheme.colorScheme.error,
                                containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.32f),
                                iconContainerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.14f)
                            ) {
                                onRemoveMarginNote(noteEntity)
                                onDismissMenus()
                            }
                        }
                        if (relatedHighlight != null) {
                            SelectionActionRow(
                                label = "Remove Annotation",
                                icon = Icons.Rounded.Delete,
                                iconTint = MaterialTheme.colorScheme.error,
                                textColor = MaterialTheme.colorScheme.error,
                                containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.32f),
                                iconContainerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.14f)
                            ) {
                                onRemoveHighlight(relatedHighlight.id)
                                onDismissMenus()
                            }
                        }
                    }
                }
            }
        }

        // Top Chrome - Follows NavigationBarStyle, Resized to be smaller
        AnimatedVisibility(
            visible = showChrome,
            enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut()
        ) {
            if (navBarStyle == NavigationBarStyle.FLOATING) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 32.dp, vertical = 12.dp)
                ) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .shadow(8.dp, RoundedCornerShape(24.dp)),
                        shape = RoundedCornerShape(24.dp),
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
                    ) {
                        ReaderTopBarContent(
                            uiState = uiState,
                            onBack = onBack,
                            onShowChapters = { showChapterSheet = true },
                            onAddBookmark = onAddBookmarkAtCurrent
                        )
                    }
                }
            } else {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .height(52.dp),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 3.dp,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.1f))
                ) {
                    ReaderTopBarContent(
                        uiState = uiState,
                        onBack = onBack,
                        onShowChapters = { showChapterSheet = true },
                        onAddBookmark = onAddBookmarkAtCurrent
                    )
                }
            }
        }

        // Bottom Navigation/Progress Bar
        AnimatedVisibility(
            visible = showChrome,
            modifier = Modifier.align(Alignment.BottomCenter),
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
        ) {
            if (navBarStyle == NavigationBarStyle.FLOATING) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 32.dp, vertical = 32.dp)
                        .navigationBarsPadding()
                ) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .shadow(12.dp, RoundedCornerShape(32.dp)),
                        shape = RoundedCornerShape(32.dp),
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
                    ) {
                        ReaderBottomBarContent(
                            uiState = uiState,
                            onHome = onHome,
                            displayPrefs = displayPrefs,
                            onShowSettings = { showSettings = true },
                            onShowSearch = { showSearchDialog = true },
                            onShowTTS = { showTTSControls = true },
                            onShowAccessibility = { showAccessibilitySettings = true },
                            onShowAnalytics = { showAnalytics = true },
                            onShowExport = { showExportDialog = true },
                            onProgressChange = onProgressJumpRequest
                        )
                    }
                }
            } else {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding(),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 3.dp,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.1f))
                ) {
                    ReaderBottomBarContent(
                        uiState = uiState,
                        onHome = onHome,
                        displayPrefs = displayPrefs,
                        onShowSettings = { showSettings = true },
                        onShowSearch = { showSearchDialog = true },
                        onShowTTS = { showTTSControls = true },
                        onShowAccessibility = { showAccessibilitySettings = true },
                        onShowAnalytics = { showAnalytics = true },
                        onShowExport = { showExportDialog = true },
                        onProgressChange = onProgressJumpRequest
                    )
                }
            }
        }

        // Page Index / Progress Indicator - Adjusted for Default Style
        if (!showChrome && !isFocusMode && !uiState.isLoading) {
            val bottomPadding = if (navBarStyle == NavigationBarStyle.DEFAULT) 80.dp else 24.dp
            val topPadding = if (navBarStyle == NavigationBarStyle.DEFAULT) 64.dp else 24.dp
            
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = bottomPadding, top = topPadding)
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.BottomCenter
            ) {
                // Progress index indicator when chrome is hidden
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.padding(bottom = 8.dp)
                ) {
                    val progressText = if (uiState.settings.readingMode == ReadingMode.PAGE && uiState.totalPages > 0) {
                        "${uiState.currentPage} / ${uiState.totalPages}"
                    } else {
                        "${(uiState.progress * 100).roundToInt()}%"
                    }
                    Text(
                        text = progressText,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        if (isFocusMode) {
            val navBarPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
            val gestureBarBottomPadding by animateDpAsState(
                targetValue = if (showChrome) 140.dp + navBarPadding else 24.dp + navBarPadding,
                label = "gestureBarPadding"
            )

            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = gestureBarBottomPadding)
                    .width(100.dp)
                    .height(32.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { showChrome = !showChrome }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .width(48.dp)
                        .height(4.dp)
                        .clip(CircleShape)
                        .background(
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                        )
                )
            }
        }
    }
}

@Composable
private fun ReaderTopBarContent(
    uiState: ReaderUiState,
    onBack: () -> Unit,
    onShowChapters: () -> Unit,
    onAddBookmark: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack, modifier = Modifier.size(36.dp)) {
            Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back", tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(20.dp))
        }

        Text(
            text = uiState.title,
            modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            IconButton(onClick = onAddBookmark, modifier = Modifier.size(36.dp)) {
                Icon(
                    Icons.Rounded.Bookmark,
                    "Bookmark",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }
            IconButton(onClick = onShowChapters, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Rounded.FormatListNumbered, "Chapters", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
            }
        }
    }
}

@Composable
private fun ReaderBottomBarContent(
    uiState: ReaderUiState,
    onHome: () -> Unit,
    displayPrefs: HomeDisplayPreferences,
    onShowSettings: () -> Unit,
    onShowSearch: () -> Unit,
    onShowTTS: () -> Unit,
    onShowAccessibility: () -> Unit,
    onShowAnalytics: () -> Unit,
    onShowExport: () -> Unit,
    onProgressChange: (Float) -> Unit
) {
    Column(
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            val progressText = if (uiState.settings.readingMode == ReadingMode.PAGE && uiState.totalPages > 0) {
                "${uiState.currentPage}/${uiState.totalPages}"
            } else {
                "${(uiState.progress * 100).roundToInt()}%"
            }

            Text(
                text = progressText,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.width(44.dp),
                textAlign = TextAlign.Center
            )

            // Allow user to swipe progress bar to jump on pages
            Slider(
                value = uiState.progress,
                onValueChange = onProgressChange,
                modifier = Modifier.weight(1f).height(32.dp),
                colors = SliderDefaults.colors(
                    thumbColor = MaterialTheme.colorScheme.primary,
                    activeTrackColor = MaterialTheme.colorScheme.primary,
                    inactiveTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                )
            )
        }

        // Build reader control actions with max 4 controls (6 total with home + settings)
        val readerControlActions = buildList<ReaderBottomAction> {
            val orderedReaderControls = if (displayPrefs.readerControlOrder.isEmpty()) {
                ReaderControl.defaultOrder()
            } else {
                displayPrefs.readerControlOrder
            }
            orderedReaderControls.forEach { control ->
                if (size >= 4) return@forEach // Limit to 4 reader controls max (6 total with home + settings)
                when (control) {
                    ReaderControl.SEARCH -> {
                        if (displayPrefs.showReaderSearch) {
                            add(ReaderBottomAction("search", Icons.Rounded.Search, "Search", false, onShowSearch))
                        }
                    }
                    ReaderControl.TTS -> {
                        if (displayPrefs.showReaderTTS) {
                            add(
                                ReaderBottomAction(
                                    "tts",
                                    Icons.Rounded.VolumeUp,
                                    "TTS",
                                    false,
                                    onShowTTS
                                )
                            )
                        }
                    }
                    ReaderControl.ACCESSIBILITY -> {
                        if (displayPrefs.showReaderAccessibility) {
                            add(
                                ReaderBottomAction(
                                    "accessibility",
                                    Icons.Rounded.Accessibility,
                                    "Accessibility",
                                    false,
                                    onShowAccessibility
                                )
                            )
                        }
                    }
                    ReaderControl.ANALYTICS -> {
                        if (displayPrefs.showReaderAnalytics) {
                            add(ReaderBottomAction("analytics", Icons.Rounded.Analytics, "Analytics", false, onShowAnalytics))
                        }
                    }
                    ReaderControl.EXPORT_HIGHLIGHT -> {
                        if (displayPrefs.showReaderExport) {
                            add(ReaderBottomAction("export", Icons.Rounded.Share, "Export", false, onShowExport))
                        }
                    }
                }
            }
        }

        val actions = buildList<ReaderBottomAction> {
            add(ReaderBottomAction("home", Icons.Rounded.Home, "Home", false, onHome))
            addAll(readerControlActions)
            add(ReaderBottomAction("settings", Icons.Rounded.Settings, "Settings", false, onShowSettings))
        }

        val buttonSize = 40.dp
        val spacing = 8.dp
        val actionScroll = rememberScrollState()

        BoxWithConstraints(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            val contentWidth = (buttonSize + spacing) * actions.size - spacing
            val shouldCenter = contentWidth <= maxWidth
            val horizontalPadding = if (shouldCenter) {
                (maxWidth - contentWidth) / 2
            } else {
                0.dp
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(actionScroll)
                    .padding(horizontal = horizontalPadding),
                horizontalArrangement = Arrangement.spacedBy(spacing),
                verticalAlignment = Alignment.CenterVertically
            ) {
                actions.forEach { action ->
                    IconButton(
                        onClick = action.onClick,
                        modifier = Modifier
                            .size(buttonSize)
                            .background(
                                if (action.highlighted) {
                                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                                } else {
                                    Color.Transparent
                                },
                                CircleShape
                            )
                    ) {
                        Icon(
                            action.icon,
                            action.label,
                            modifier = Modifier.size(22.dp),
                            tint = if (action.highlighted) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    }
                }
            }
        }
    }
}

private data class ReaderBottomAction(
    val id: String,
    val icon: ImageVector,
    val label: String,
    val highlighted: Boolean,
    val onClick: () -> Unit
)

@Composable
private fun SelectionActionRow(
    label: String,
    icon: ImageVector,
    iconTint: Color = MaterialTheme.colorScheme.onSurface,
    textColor: Color = MaterialTheme.colorScheme.onSurface,
    containerColor: Color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.28f),
    iconContainerColor: Color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(containerColor)
            .clickable(onClick = onClick)
            .padding(horizontal = 6.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(20.dp)
                .clip(CircleShape)
                .background(iconContainerColor),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                modifier = Modifier.size(12.dp),
                tint = iconTint
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = textColor
        )
    }
}

@Composable
private fun ReaderDropdownSurface(
    modifier: Modifier = Modifier,
    width: Dp = 196.dp,
    maxHeight: Dp = 248.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = RoundedCornerShape(14.dp)
    val scrollState = rememberScrollState()
    Surface(
        modifier = modifier
            .width(width)
            .shadow(10.dp, shape, clip = false),
        shape = shape,
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.985f),
        tonalElevation = 8.dp,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = maxHeight)
                .verticalScroll(scrollState)
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            content = content
        )
    }
}

private fun dropdownWidthForLabels(labels: List<String>): Dp {
    val longest = labels.maxOfOrNull { it.length } ?: 10
    val estimated = (longest * 6.2f + 62f).dp
    return estimated.coerceIn(148.dp, 220.dp)
}

private fun anchoredDropdownPositionProvider(
    anchorX: Int,
    anchorY: Int,
    marginPx: Int,
    verticalGapPx: Int
): PopupPositionProvider {
    return object : PopupPositionProvider {
        override fun calculatePosition(
            anchorBounds: IntRect,
            windowSize: IntSize,
            layoutDirection: LayoutDirection,
            popupContentSize: IntSize
        ): IntOffset {
            val maxX = (windowSize.width - popupContentSize.width - marginPx).coerceAtLeast(marginPx)
            val centeredX = (anchorX - popupContentSize.width / 2).coerceIn(marginPx, maxX)

            val maxY = (windowSize.height - popupContentSize.height - marginPx).coerceAtLeast(marginPx)
            val preferredBelowY = anchorY + verticalGapPx
            val bestY = preferredBelowY.coerceIn(marginPx, maxY)

            return IntOffset(centeredX, bestY)
        }
    }
}

@Composable
fun ImageZoomDialog(
    url: String,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        val configuration = LocalConfiguration.current
        val density = LocalDensity.current
        val screenWidth = with(density) { configuration.screenWidthDp.dp.toPx() }
        val screenHeight = with(density) { configuration.screenHeightDp.dp.toPx() }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.9f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss
                ),
            contentAlignment = Alignment.Center
        ) {
            var scale by remember { mutableFloatStateOf(1f) }
            var offset by remember { mutableStateOf(Offset.Zero) }
            val state = rememberTransformableState { zoomChange: Float, offsetChange: Offset, _ ->
                scale = (scale * zoomChange).coerceIn(1f, 5f)

                val extraWidth = (scale - 1) * screenWidth
                val extraHeight = (scale - 1) * screenHeight

                val maxX = extraWidth / 2
                val maxY = extraHeight / 2

                offset = if (scale > 1f) {
                    Offset(
                        x = (offset.x + offsetChange.x).coerceIn(-maxX, maxX),
                        y = (offset.y + offsetChange.y).coerceIn(-maxY, maxY)
                    )
                } else {
                    Offset.Zero
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer(
                        scaleX = scale,
                        scaleY = scale,
                        translationX = offset.x,
                        translationY = offset.y
                    )
                    .transformable(state = state)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {}
                    )
            ) {
                AsyncImage(
                    model = url,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize()
                )
            }

            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
                    .background(Color.Black.copy(alpha = 0.5f), CircleShape)
            ) {
                Icon(Icons.Rounded.Close, "Close", tint = Color.White)
            }
        }
    }
}
