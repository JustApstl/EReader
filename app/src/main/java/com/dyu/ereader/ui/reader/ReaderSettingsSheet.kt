package com.dyu.ereader.ui.reader

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material3.ripple
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.FormatIndentIncrease
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.offset
import androidx.compose.ui.unit.sp
import com.dyu.ereader.data.model.FontColorTheme
import com.dyu.ereader.data.model.ImageFilter
import com.dyu.ereader.data.model.PageTransitionStyle
import com.dyu.ereader.data.model.ReaderFont
import com.dyu.ereader.data.model.ReaderSettings
import com.dyu.ereader.data.model.ReaderTheme
import com.dyu.ereader.data.model.ReadingMode
import com.dyu.ereader.data.model.NavigationBarStyle
import com.dyu.ereader.data.model.getBackgroundColor
import com.dyu.ereader.data.model.getColor
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderSettingsSheet(
    settings: ReaderSettings,
    isDarkTheme: Boolean,
    onDismiss: () -> Unit,
    onReadingModeChange: (ReadingMode) -> Unit,
    onThemeChange: (ReaderTheme) -> Unit,
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
    onResetSettings: () -> Unit,
    onCustomColorSelected: (Int) -> Unit,
    onCustomFontColorSelected: (Int) -> Unit,
    onPickCustomFont: () -> Unit,
    onClearCustomFont: () -> Unit,
    onPickBackgroundImage: () -> Unit,
    onBackgroundImageBlurChange: (Float) -> Unit,
    onBackgroundImageOpacityChange: (Float) -> Unit,
    onImageFilterChange: (ImageFilter) -> Unit,
    onUsePublisherStyleToggle: (Boolean) -> Unit,
    onUnderlineLinksToggle: (Boolean) -> Unit,
    onTextShadowToggle: (Boolean) -> Unit,
    onTextShadowColorChange: (Int?) -> Unit,
    onNavigationBarStyleChange: (NavigationBarStyle) -> Unit,
    onPageTurn3dToggle: (Boolean) -> Unit = {},
    onPageTransitionStyleChange: (PageTransitionStyle) -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var showColorPicker by remember { mutableStateOf(false) }
    var showFontColorPicker by remember { mutableStateOf(false) }
    var showBionicColorPicker by remember { mutableStateOf(false) }
    var showTextShadowColorPicker by remember { mutableStateOf(false) }

    if (showColorPicker) {
        ColorPickerDialog(
            onDismiss = { showColorPicker = false },
            onColorSelected = onCustomColorSelected,
            initialColor = settings.customBackgroundColor
        )
    }

    if (showFontColorPicker) {
        ColorPickerDialog(
            onDismiss = { showFontColorPicker = false },
            onColorSelected = onCustomFontColorSelected,
            initialColor = settings.customFontColor ?: 0xFF000000.toInt()
        )
    }

    if (showBionicColorPicker) {
        ColorPickerDialog(
            onDismiss = { showBionicColorPicker = false },
            onColorSelected = { onFocusTextColorChange(it) },
            initialColor = settings.focusTextColor ?: 0xFF6650a4.toInt()
        )
    }

    if (showTextShadowColorPicker) {
        ColorPickerDialog(
            onDismiss = { showTextShadowColorPicker = false },
            onColorSelected = { onTextShadowColorChange(it) },
            initialColor = settings.textShadowColor ?: 0x66000000
        )
    }

    val pagerState = rememberPagerState(pageCount = { 3 })

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        dragHandle = {
            BottomSheetDefaults.DragHandle()
        },
        containerColor = MaterialTheme.colorScheme.surface,
        contentWindowInsets = { WindowInsets.navigationBars }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Appearance",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                IconButton(
                    onClick = onResetSettings,
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
                ) {
                    Icon(Icons.Rounded.RestartAlt, contentDescription = "Reset All", tint = MaterialTheme.colorScheme.onSurface)
                }
            }

            ScrollableTabRow(
                selectedTabIndex = pagerState.currentPage,
                modifier = Modifier.fillMaxWidth(),
                edgePadding = 20.dp,
                containerColor = Color.Transparent,
                divider = {}
            ) {
                val tabs = listOf("Layout", "Typeface", "Experience")
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = pagerState.currentPage == index,
                        onClick = { scope.launch { pagerState.animateScrollToPage(index) } },
                        text = { Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold) }
                    )
                }
            }

            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
                userScrollEnabled = true 
            ) { page ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    when (page) {
                        0 -> { // Layout & Themes
                            SettingsCard {
                                Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
                                    SettingsSection(title = "Display Mode", icon = Icons.Rounded.DisplaySettings) {
                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            ModeChip(
                                                selected = settings.readingMode == ReadingMode.SCROLL,
                                                onClick = { onReadingModeChange(ReadingMode.SCROLL) },
                                                label = "Scrolled",
                                                icon = Icons.Rounded.VerticalAlignBottom,
                                                modifier = Modifier.weight(1f)
                                            )
                                            ModeChip(
                                                selected = settings.readingMode == ReadingMode.PAGE,
                                                onClick = { onReadingModeChange(ReadingMode.PAGE) },
                                                label = "Paged",
                                                icon = Icons.Rounded.AutoStories,
                                                modifier = Modifier.weight(1f)
                                            )
                                        }
                                    }

                                    if (settings.readingMode == ReadingMode.PAGE) {
                                        ToggleRow(
                                            title = "3D Page Turn",
                                            desc = "Realistic page turning animation",
                                            checked = settings.pageTurn3d,
                                            icon = Icons.Rounded.ViewInAr,
                                            onToggle = onPageTurn3dToggle
                                        )

                                        if (settings.pageTurn3d) {
                                            SettingsSection(
                                                title = "Page Transition",
                                                icon = Icons.Rounded.Animation
                                            ) {
                                                val transitionStyles = listOf(
                                                    PageTransitionStyle.DEFAULT,
                                                    PageTransitionStyle.TILT,
                                                    PageTransitionStyle.CARD,
                                                    PageTransitionStyle.FLIP,
                                                    PageTransitionStyle.CUBE,
                                                    PageTransitionStyle.ROLL
                                                )
                                                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                    items(transitionStyles) { style ->
                                                        FilterChip(
                                                            selected = settings.pageTransitionStyle == style,
                                                            onClick = { onPageTransitionStyleChange(style) },
                                                            label = style.name.lowercase().replaceFirstChar { it.uppercase() }
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    SettingsSection(title = "Background Theme", icon = Icons.Rounded.Palette) {
                                        ThemeSelector(
                                            settings = settings,
                                            isDarkTheme = isDarkTheme,
                                            onThemeChange = onThemeChange,
                                            onPickCustomColor = { showColorPicker = true },
                                            onPickBackgroundImage = onPickBackgroundImage
                                        )
                                    }

                                    if (settings.readerTheme == ReaderTheme.IMAGE && settings.backgroundImageUri != null) {
                                        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                            SliderSetting(
                                                label = "Background Blur",
                                                value = settings.backgroundImageBlur,
                                                valueRange = 0f..20f,
                                                onValueChange = onBackgroundImageBlurChange,
                                                onReset = { onBackgroundImageBlurChange(0f) },
                                                icon = Icons.Rounded.BlurOn,
                                                valueDisplay = "${settings.backgroundImageBlur.toInt()}px"
                                            )
                                            SliderSetting(
                                                label = "Background Opacity",
                                                value = settings.backgroundImageOpacity,
                                                valueRange = 0.1f..1f,
                                                onValueChange = onBackgroundImageOpacityChange,
                                                onReset = { onBackgroundImageOpacityChange(1f) },
                                                icon = Icons.Rounded.Opacity,
                                                valueDisplay = "${(settings.backgroundImageOpacity * 100).toInt()}%"
                                            )
                                        }
                                    }

                                    SettingsSection(title = "Image Filter", icon = Icons.Rounded.Filter) {
                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            FilterChip(
                                                selected = settings.imageFilter == ImageFilter.NONE,
                                                onClick = { onImageFilterChange(ImageFilter.NONE) },
                                                label = "None"
                                            )
                                            FilterChip(
                                                selected = settings.imageFilter == ImageFilter.INVERT,
                                                onClick = { onImageFilterChange(ImageFilter.INVERT) },
                                                label = "Invert"
                                            )
                                            FilterChip(
                                                selected = settings.imageFilter == ImageFilter.DARKEN,
                                                onClick = { onImageFilterChange(ImageFilter.DARKEN) },
                                                label = "Darken"
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        1 -> { // Typeface
                            SettingsCard {
                                Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
                                    ToggleRow(
                                        title = "Publisher Style",
                                        desc = "Use book's default layout",
                                        checked = settings.usePublisherStyle,
                                        icon = Icons.Rounded.AutoFixHigh,
                                        onToggle = onUsePublisherStyleToggle,
                                        beta = true
                                    )

                                    if (!settings.usePublisherStyle) {
                                        SettingsSection(title = "Typeface", icon = Icons.Rounded.FontDownload) {
                                            FontSelector(
                                                currentFont = settings.font,
                                                hasCustomFont = !settings.customFontUri.isNullOrBlank(),
                                                customFontLabel = settings.customFontUri
                                                    ?.substringAfterLast('/')
                                                    ?.substringBefore('?')
                                                    ?.substringBefore('#')
                                                    ?.take(22),
                                                onPickCustomFont = onPickCustomFont,
                                                onFontChange = onFontChange,
                                                onClearCustomFont = onClearCustomFont
                                            )
                                        }

                                        SettingsSection(title = "Font Color", icon = Icons.Rounded.FormatColorText) {
                                            ToggleRow(
                                                title = "Auto Font Color",
                                                desc = "Adapt text color to background",
                                                checked = settings.autoFontColor,
                                                icon = Icons.Rounded.AutoAwesome,
                                                onToggle = onAutoFontColorToggle
                                            )
                                            Spacer(Modifier.height(8.dp))
                                            FontColorSelector(
                                                currentTheme = settings.fontColorTheme,
                                                customColor = settings.customFontColor,
                                                enabled = !settings.autoFontColor,
                                                onThemeChange = onFontColorThemeChange,
                                                onPickCustomColor = { showFontColorPicker = true }
                                            )
                                        }

                                        SliderSetting(
                                            label = "Font Size",
                                            value = settings.fontSizeSp,
                                            valueRange = 12f..36f,
                                            onValueChange = onFontSizeChange,
                                            onReset = { onFontSizeChange(15f) },
                                            icon = Icons.Rounded.FormatSize,
                                            valueDisplay = "${settings.fontSizeSp.toInt()}"
                                        )

                                        SliderSetting(
                                            label = "Line Spacing",
                                            value = settings.lineSpacing,
                                            valueRange = 1.0f..2.5f,
                                            onValueChange = onLineSpacingChange,
                                            onReset = { onLineSpacingChange(1.55f) },
                                            icon = Icons.Rounded.FormatLineSpacing,
                                            valueDisplay = "%.1f".format(settings.lineSpacing)
                                        )

                                        SliderSetting(
                                            label = "Horizontal Margins",
                                            value = settings.horizontalMarginDp,
                                            valueRange = 0f..60f,
                                            onValueChange = onMarginChange,
                                            onReset = { onMarginChange(20f) },
                                            icon = Icons.AutoMirrored.Rounded.FormatIndentIncrease,
                                            valueDisplay = "${settings.horizontalMarginDp.toInt()}"
                                        )
                                    } else {
                                        Text(
                                            "Font settings are disabled when Publisher Style is on.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.padding(top = 8.dp)
                                        )
                                    }
                                }
                            }
                        }
                        2 -> { // Experience
                            SettingsCard {
                                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                    Text(
                                        "Experience",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold
                                    )

                                    SettingsSection(title = "Navigation Bar Style", icon = Icons.Rounded.SmartButton) {
                                        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                                            NavigationBarStyle.entries.forEachIndexed { index, style ->
                                                SegmentedButton(
                                                    shape = SegmentedButtonDefaults.itemShape(index = index, count = NavigationBarStyle.entries.size),
                                                    onClick = { onNavigationBarStyleChange(style) },
                                                    selected = settings.navBarStyle == style,
                                                    label = { 
                                                        Text(style.name.lowercase().replaceFirstChar { it.uppercase() }) 
                                                    }
                                                )
                                            }
                                        }
                                    }

                                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
                                    
                                    ToggleRow(
                                        title = "Focus Mode",
                                        desc = "Hide all UI while reading",
                                        checked = settings.focusMode,
                                        icon = Icons.Rounded.FilterCenterFocus,
                                        onToggle = onFocusModeToggle
                                    )
                                    
                                    ToggleRow(
                                        title = "Hide Status Bar",
                                        desc = "Full screen experience",
                                        checked = settings.hideStatusBar,
                                        icon = Icons.Rounded.Fullscreen,
                                        onToggle = onHideStatusBarToggle,
                                        beta = true
                                    )

                                    ToggleRow(
                                        title = "Underline Links",
                                        desc = "Show underline for clickable links",
                                        checked = settings.underlineLinks,
                                        icon = Icons.Rounded.Link,
                                        onToggle = onUnderlineLinksToggle,
                                        beta = true
                                    )

                                    ToggleRow(
                                        title = "Text Shadow",
                                        desc = "Add depth to book text",
                                        checked = settings.textShadow,
                                        icon = Icons.Rounded.TextRotationNone,
                                        onToggle = onTextShadowToggle
                                    )

                                    if (settings.textShadow) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    Icons.Rounded.Palette,
                                                    null,
                                                    modifier = Modifier.size(18.dp),
                                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                                Spacer(Modifier.width(8.dp))
                                                Text(
                                                    "Shadow Color",
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                            }
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                if (settings.textShadowColor != null) {
                                                    IconButton(
                                                        onClick = { onTextShadowColorChange(null) },
                                                        modifier = Modifier.size(32.dp)
                                                    ) {
                                                        Icon(
                                                            Icons.Rounded.RestartAlt,
                                                            null,
                                                            modifier = Modifier.size(18.dp),
                                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                                        )
                                                    }
                                                }
                                                Box(
                                                    modifier = Modifier
                                                        .size(32.dp)
                                                        .clip(CircleShape)
                                                        .background(Color(settings.textShadowColor ?: 0x66000000))
                                                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
                                                        .clickable { showTextShadowColorPicker = true }
                                                )
                                            }
                                        }
                                    }

                                    ToggleRow(
                                        title = "Bionic Reading",
                                        desc = "Emphasis on word starts",
                                        checked = settings.focusText,
                                        icon = Icons.Rounded.Bolt,
                                        onToggle = onFocusTextToggle
                                    )

                                    if (settings.focusText) {
                                        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Icon(Icons.Rounded.Palette, null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                                    Spacer(Modifier.width(8.dp))
                                                    Text("Bionic Color", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                                                }
                                                
                                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                    if (settings.focusTextColor != null) {
                                                        IconButton(onClick = { onFocusTextColorChange(null) }, modifier = Modifier.size(32.dp)) {
                                                            Icon(Icons.Rounded.RestartAlt, null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                                        }
                                                    }
                                                    Box(
                                                        modifier = Modifier
                                                            .size(32.dp)
                                                            .clip(CircleShape)
                                                            .background(settings.focusTextColor?.let { Color(it) } ?: MaterialTheme.colorScheme.primary)
                                                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
                                                            .clickable { showBionicColorPicker = true }
                                                    )
                                                }
                                            }

                                            SliderSetting(
                                                label = "Bionic Boldness",
                                                value = settings.focusTextBoldness.toFloat(),
                                                valueRange = 400f..900f,
                                                onValueChange = { onFocusTextBoldnessChange(it.toInt()) },
                                                onReset = { onFocusTextBoldnessChange(700) },
                                                icon = Icons.Rounded.FormatBold,
                                                valueDisplay = settings.focusTextBoldness.toString()
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FilterChip(
    selected: Boolean,
    onClick: () -> Unit,
    label: String
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) },
        leadingIcon = if (selected) {
            { Icon(Icons.Rounded.Check, null, Modifier.size(FilterChipDefaults.IconSize)) }
        } else null
    )
}

@Composable
private fun FontColorSelector(
    currentTheme: FontColorTheme,
    customColor: Int?,
    enabled: Boolean,
    onThemeChange: (FontColorTheme) -> Unit,
    onPickCustomColor: () -> Unit
) {
    val themes = FontColorTheme.entries
    Box(modifier = Modifier.pointerInput(Unit) {
        detectHorizontalDragGestures { _, _ -> }
    }) {
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(themes) { theme ->
                val isSelected = currentTheme == theme
                val color = theme.getColor(customColor) ?: MaterialTheme.colorScheme.onSurface
                
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(color.copy(alpha = if (enabled) 1f else 0.5f))
                            .border(
                                width = if (isSelected) { 3.dp } else { 1.dp },
                                color = if (isSelected) { MaterialTheme.colorScheme.primary } else { MaterialTheme.colorScheme.outlineVariant },
                                shape = CircleShape
                            )
                            .clickable(enabled = enabled) {
                                if (!enabled) return@clickable
                                if (theme == FontColorTheme.CUSTOM) {
                                    if (isSelected) onPickCustomColor()
                                    else if (customColor != null) onThemeChange(FontColorTheme.CUSTOM)
                                    else onPickCustomColor()
                                }
                                else onThemeChange(theme)
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        if (isSelected) {
                            Icon(
                                Icons.Rounded.Check,
                                null,
                                tint = if (color.luminance() > 0.5f) Color.Black else Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        } else if (theme == FontColorTheme.CUSTOM) {
                            Icon(
                                Icons.Rounded.ColorLens,
                                null,
                                tint = if (color.luminance() > 0.5f) Color.Black else Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Text(
                        theme.label,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = if (enabled) 1f else 0.45f)
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingsCard(content: @Composable () -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            content()
        }
    }
}

@Composable
private fun BetaTag() {
    Surface(
        color = MaterialTheme.colorScheme.primaryContainer,
        shape = CircleShape,
        modifier = Modifier.padding(start = 8.dp)
    ) {
        Text(
            "Beta", 
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp),
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp, fontWeight = FontWeight.Black),
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
    }
}

@Composable
private fun SettingsSection(
    title: String,
    icon: ImageVector? = null,
    beta: Boolean = false,
    content: @Composable () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (icon != null) {
                Icon(
                    icon,
                    null,
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.width(8.dp))
            }
            Text(
                title,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
            if (beta) BetaTag()
        }
        content()
    }
}

@Composable
private fun ModeChip(
    selected: Boolean,
    onClick: () -> Unit,
    label: String,
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    Surface(
        selected = selected,
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
        tonalElevation = if (selected) 0.dp else 2.dp,
        border = if (selected) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = modifier.height(48.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                icon,
                null,
                modifier = Modifier.size(20.dp),
                tint = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.width(8.dp))
            Text(
                label,
                style = MaterialTheme.typography.labelLarge,
                color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
            )
        }
    }
}

@Composable
private fun ThemeSelector(
    settings: ReaderSettings,
    isDarkTheme: Boolean,
    onThemeChange: (ReaderTheme) -> Unit,
    onPickCustomColor: () -> Unit,
    onPickBackgroundImage: () -> Unit
) {
    val themes = listOf(
        ReaderTheme.SYSTEM,
        ReaderTheme.WHITE,
        ReaderTheme.SEPIA,
        ReaderTheme.DARK,
        ReaderTheme.BLACK,
        ReaderTheme.CUSTOM,
        ReaderTheme.IMAGE
    )

    Box(modifier = Modifier.pointerInput(Unit) {
        detectHorizontalDragGestures { _, _ -> }
    }) {
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(themes) { theme ->
                val isSelected = settings.readerTheme == theme
                val themeBg = theme.getBackgroundColor(isDarkTheme, settings.customBackgroundColor)
                
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(if (theme == ReaderTheme.IMAGE) Color.LightGray.copy(alpha = 0.5f) else themeBg)
                            .border(
                                width = if (isSelected) { 3.dp } else { 1.dp },
                                color = if (isSelected) { MaterialTheme.colorScheme.primary } else { MaterialTheme.colorScheme.outlineVariant },
                                shape = CircleShape
                            )
                            .clickable { 
                                when (theme) {
                                    ReaderTheme.CUSTOM -> {
                                        if (isSelected) onPickCustomColor()
                                        else if (settings.customBackgroundColor != null) onThemeChange(ReaderTheme.CUSTOM)
                                        else onPickCustomColor()
                                    }
                                    ReaderTheme.IMAGE -> {
                                        if (isSelected) onPickBackgroundImage()
                                        else if (settings.backgroundImageUri != null) onThemeChange(ReaderTheme.IMAGE)
                                        else onPickBackgroundImage()
                                    }
                                    else -> onThemeChange(theme)
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        when (theme) {
                            ReaderTheme.CUSTOM -> Icon(Icons.Rounded.ColorLens, null, tint = if (themeBg.luminance() > 0.5f) Color.Black else Color.White)
                            ReaderTheme.IMAGE -> Icon(Icons.Rounded.Image, null, tint = Color.DarkGray)
                            else -> {
                                if (isSelected) {
                                    Icon(
                                        Icons.Rounded.Check,
                                        null,
                                        tint = if (themeBg.luminance() > 0.5f) Color.Black else Color.White
                                    )
                                }
                            }
                        }
                    }
                    Text(theme.label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface)
                }
            }
        }
    }
}

@Composable
private fun FontSelector(
    currentFont: ReaderFont,
    hasCustomFont: Boolean,
    customFontLabel: String?,
    onPickCustomFont: () -> Unit,
    onFontChange: (ReaderFont) -> Unit,
    onClearCustomFont: () -> Unit = {}
) {
    Box(modifier = Modifier.pointerInput(Unit) {
        detectHorizontalDragGestures { _, _ -> }
    }) {
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(horizontal = 4.dp)
        ) {
            items(ReaderFont.entries) { font ->
                val isSelected = currentFont == font
                val fontFamily = when(font) {
                    ReaderFont.SANS -> FontFamily.SansSerif
                    ReaderFont.SERIF -> FontFamily.Serif
                    ReaderFont.MONO -> FontFamily.Monospace
                    ReaderFont.CURSIVE -> FontFamily.Cursive
                    ReaderFont.NOTO_SERIF -> FontFamily.Serif
                    ReaderFont.GEORGIA -> FontFamily.Serif
                    ReaderFont.BOOK_STYLE -> FontFamily.Serif
                    ReaderFont.CUSTOM -> FontFamily.Default
                    else -> FontFamily.Default
                }
                
                Box {
                    Surface(
                        onClick = {
                            // If clicking custom font without a loaded font, pick one
                            if (font == ReaderFont.CUSTOM && !hasCustomFont) {
                                onPickCustomFont()
                            } else {
                                // For any other font, or custom with a font loaded, apply the change
                                onFontChange(font)
                            }
                        },
                        modifier = Modifier.height(48.dp),
                        shape = RoundedCornerShape(16.dp),
                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        border = BorderStroke(
                            width = if (isSelected) 2.dp else 1.dp,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                        )
                    ) {
                        Box(modifier = Modifier.fillMaxHeight().padding(horizontal = 20.dp), contentAlignment = Alignment.Center) {
                            Text(
                                text = when {
                                    font == ReaderFont.CUSTOM && !hasCustomFont -> "${font.label} +"
                                    font == ReaderFont.CUSTOM && hasCustomFont -> customFontLabel?.let { "Custom: $it" } ?: font.label
                                    else -> font.label
                                },
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    fontFamily = fontFamily,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                                )
                            )
                        }
                    }
                    
                    // Show X button on custom font when it has a font loaded and is selected
                    if (font == ReaderFont.CUSTOM && hasCustomFont && isSelected) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .offset(x = 8.dp, y = (-8).dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.error)
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = ripple(bounded = false, radius = 12.dp),
                                    onClick = {
                                        onClearCustomFont()
                                        // Switch to SANS font after clearing
                                        onFontChange(ReaderFont.SANS)
                                    }
                                )
                                .padding(4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Rounded.Close,
                                contentDescription = "Remove custom font",
                                modifier = Modifier.size(14.dp),
                                tint = MaterialTheme.colorScheme.onError
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SliderSetting(
    label: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit,
    onReset: () -> Unit,
    icon: ImageVector,
    valueDisplay: String
) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Icon(icon, null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.width(8.dp))
            Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.onSurface)
            
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                IconButton(onClick = onReset, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Rounded.RestartAlt, null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        valueDisplay,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            modifier = Modifier.padding(top = 0.dp),
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.primary,
                activeTrackColor = MaterialTheme.colorScheme.primary,
                inactiveTrackColor = MaterialTheme.colorScheme.outlineVariant
            )
        )
    }
}

@Composable
private fun ToggleRow(
    title: String,
    desc: String,
    checked: Boolean,
    icon: ImageVector,
    onToggle: (Boolean) -> Unit,
    beta: Boolean = false
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
            Icon(
                icon,
                null,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.width(12.dp))
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    if (beta) BetaTag()
                }
                Text(desc, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Switch(
            checked = checked, 
            onCheckedChange = onToggle,
            thumbContent = if (checked) {
                { Icon(Icons.Rounded.Check, null, Modifier.size(SwitchDefaults.IconSize)) }
            } else null,
            colors = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                checkedTrackColor = MaterialTheme.colorScheme.primary,
                uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        )
    }
}
