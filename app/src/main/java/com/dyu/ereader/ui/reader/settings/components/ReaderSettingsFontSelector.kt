package com.dyu.ereader.ui.reader.settings.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.dyu.ereader.data.model.reader.ReaderFont

@Composable
internal fun FontSelector(
    currentFont: ReaderFont,
    hasCustomFont: Boolean,
    customFontLabel: String?,
    onPickCustomFont: () -> Unit,
    onFontChange: (ReaderFont) -> Unit,
    onClearCustomFont: () -> Unit = {},
    enabled: Boolean
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
                val fontFamily = when (font) {
                    ReaderFont.SANS -> FontFamily.SansSerif
                    ReaderFont.SERIF -> FontFamily.Serif
                    ReaderFont.MONO -> FontFamily.Monospace
                    ReaderFont.CURSIVE -> FontFamily.Cursive
                    ReaderFont.CUSTOM -> FontFamily.Default
                    else -> FontFamily.Default
                }

                Box {
                    val isCustomOption = font == ReaderFont.CUSTOM
                    val buttonLabel = when {
                        font == ReaderFont.CUSTOM && hasCustomFont ->
                            customFontLabel?.uppercase() ?: "CUSTOM"
                        else -> when (font) {
                            ReaderFont.DEFAULT -> "ORIGINAL"
                            ReaderFont.SYSTEM -> "SYSTEM"
                            ReaderFont.SANS -> "SANS SERIF"
                            ReaderFont.SERIF -> "SERIF"
                            ReaderFont.MONO -> "MONO"
                            ReaderFont.CURSIVE -> "CURSIVE"
                            ReaderFont.CUSTOM -> "CUSTOM"
                        }
                    }
                    val containerColor = when {
                        isSelected -> MaterialTheme.colorScheme.primary
                        isCustomOption -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.58f)
                        else -> MaterialTheme.colorScheme.surface
                    }
                    val contentColor = when {
                        isSelected -> MaterialTheme.colorScheme.onPrimary
                        isCustomOption -> MaterialTheme.colorScheme.onSecondaryContainer
                        else -> MaterialTheme.colorScheme.onSurface
                    }
                    Surface(
                        onClick = {
                            if (!enabled) return@Surface
                            if (font == ReaderFont.CUSTOM && !hasCustomFont) {
                                onPickCustomFont()
                            } else {
                                onFontChange(font)
                            }
                        },
                        modifier = Modifier.height(60.dp),
                        shape = RoundedCornerShape(18.dp),
                        color = containerColor,
                        border = BorderStroke(
                            width = if (isSelected) 2.dp else 1.dp,
                            color = when {
                                isSelected -> MaterialTheme.colorScheme.primary
                                isCustomOption -> MaterialTheme.colorScheme.secondary.copy(alpha = 0.36f)
                                else -> MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                            }
                        ),
                        enabled = enabled
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .padding(horizontal = 22.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = buttonLabel,
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    fontFamily = fontFamily,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = contentColor
                                ),
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    if (font == ReaderFont.CUSTOM && hasCustomFont && isSelected) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .offset(x = 8.dp, y = (-8).dp)
                                .background(MaterialTheme.colorScheme.error, CircleShape)
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = ripple(bounded = false, radius = 12.dp),
                                    onClick = {
                                        onClearCustomFont()
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
