package com.dyu.ereader.ui.home.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoStories
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Sell
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dyu.ereader.data.model.app.AppAccent
import com.dyu.ereader.ui.app.theme.resolveAccentColor
import com.dyu.ereader.ui.app.theme.systemAccentPreviewColor
import java.util.Locale

@Composable
internal fun ThemePreviewPanel(
    appAccent: AppAccent,
    customAccentColor: Int?
) {
    val colors = MaterialTheme.colorScheme
    val systemAccentPreview = systemAccentPreviewColor(colors.primary)
    val seedColor = if (appAccent == AppAccent.SYSTEM) {
        systemAccentPreview
    } else {
        appAccent.resolveAccentColor(
            customAccentColor = customAccentColor,
            fallback = systemAccentPreview
        )
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = "Theme Preview",
            style = MaterialTheme.typography.labelLarge,
            color = colors.primary,
            fontWeight = FontWeight.ExtraBold
        )
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = colors.surfaceContainerLow,
            border = BorderStroke(1.dp, colors.outlineVariant.copy(alpha = 0.28f))
        ) {
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(10.dp)
            ) {
                val compact = maxWidth < 620.dp
                if (compact) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        PalettePreview(
                            seedColor = seedColor,
                            compact = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        ThemeWidgetGrid(
                            compact = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        PalettePreview(
                            seedColor = seedColor,
                            compact = false,
                            modifier = Modifier.width(138.dp)
                        )
                        ThemeWidgetGrid(
                            compact = false,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PalettePreview(
    seedColor: Color,
    compact: Boolean,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme
    if (compact) {
        Column(
            modifier = modifier,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                PaletteSwatchCard("Primary", colors.primary, seedColor, compact = true, modifier = Modifier.weight(1f))
                PaletteSwatchCard("Secondary", colors.secondary, seedColor, compact = true, modifier = Modifier.weight(1f))
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                PaletteSwatchCard("Tertiary", colors.tertiary, seedColor, compact = true, modifier = Modifier.weight(1f))
                PaletteSwatchCard("Neutral", colors.surfaceVariant, seedColor, compact = true, modifier = Modifier.weight(1f))
            }
        }
    } else {
        Column(
            modifier = modifier,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            PaletteSwatchCard("Primary", colors.primary, seedColor)
            PaletteSwatchCard("Secondary", colors.secondary, seedColor)
            PaletteSwatchCard("Tertiary", colors.tertiary, seedColor)
            PaletteSwatchCard("Neutral", colors.surfaceVariant, seedColor)
        }
    }
}

@Composable
private fun PaletteSwatchCard(
    title: String,
    color: Color,
    seedColor: Color,
    compact: Boolean = false,
    modifier: Modifier = Modifier
) {
    val cardHeight = if (compact) 74.dp else 88.dp
    val stripHeight = if (compact) 18.dp else 26.dp
    val labelStyle = if (compact) MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp) else MaterialTheme.typography.labelSmall

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(cardHeight),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
    ) {
        Column(modifier = Modifier.fillMaxHeight()) {
            val topColor = if (title == "Primary") seedColor else color
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(topColor)
                    .padding(horizontal = if (compact) 6.dp else 8.dp, vertical = if (compact) 4.dp else 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    text = title,
                    style = labelStyle,
                    fontWeight = FontWeight.Bold,
                    color = topColor.onPreviewColor(),
                    maxLines = 1
                )
                Text(
                    text = topColor.toHexRgb(),
                    style = labelStyle,
                    color = topColor.onPreviewColor().copy(alpha = 0.92f),
                    maxLines = 1
                )
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(stripHeight)
            ) {
                for (index in 0..8) {
                    val shade = gradientShade(topColor, index / 8f)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .background(shade)
                    )
                }
            }
        }
    }
}

@Composable
private fun ThemeWidgetGrid(
    compact: Boolean,
    modifier: Modifier = Modifier
) {
    if (compact) {
        Column(
            modifier = modifier,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TypographyCard(
                    title = "Headline",
                    sampleStyle = MaterialTheme.typography.headlineLarge.copy(fontSize = 46.sp, lineHeight = 46.sp),
                    compact = true,
                    modifier = Modifier.weight(1f)
                )
                ButtonsCard(compact = true, modifier = Modifier.weight(1f))
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TypographyCard(
                    title = "Body",
                    sampleStyle = MaterialTheme.typography.bodyLarge.copy(fontSize = 46.sp, lineHeight = 46.sp),
                    compact = true,
                    modifier = Modifier.weight(1f)
                )
                SearchCard(compact = true, modifier = Modifier.weight(1f))
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TypographyCard(
                    title = "Label",
                    sampleStyle = MaterialTheme.typography.labelLarge.copy(fontSize = 46.sp, lineHeight = 46.sp),
                    compact = true,
                    modifier = Modifier.weight(1f)
                )
                ProgressCard(compact = true, modifier = Modifier.weight(1f))
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                NavigationCard(compact = true, modifier = Modifier.weight(1f))
                IconRowCard(compact = true, modifier = Modifier.weight(1f))
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CompactIconCard(compact = true, modifier = Modifier.weight(1f))
                ChipCard(compact = true, modifier = Modifier.weight(1f))
            }
        }
    } else {
        Column(
            modifier = modifier,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TypographyCard(
                    title = "Headline",
                    sampleStyle = MaterialTheme.typography.headlineLarge.copy(fontSize = 64.sp),
                    modifier = Modifier.weight(1.2f)
                )
                ButtonsCard(Modifier.weight(1f))
                SearchCard(Modifier.weight(1f))
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TypographyCard(
                    title = "Body",
                    sampleStyle = MaterialTheme.typography.bodyLarge.copy(fontSize = 64.sp, lineHeight = 64.sp),
                    modifier = Modifier.weight(1.2f)
                )
                ProgressCard(Modifier.weight(1f))
                NavigationCard(Modifier.weight(1f))
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TypographyCard(
                    title = "Label",
                    sampleStyle = MaterialTheme.typography.labelLarge.copy(fontSize = 64.sp, lineHeight = 64.sp),
                    modifier = Modifier.weight(1.2f)
                )
                CompactIconCard(Modifier.weight(0.45f))
                ChipCard(Modifier.weight(0.8f))
                IconRowCard(Modifier.weight(1.1f))
            }
        }
    }
}

@Composable
private fun TypographyCard(
    title: String,
    sampleStyle: TextStyle,
    modifier: Modifier = Modifier,
    compact: Boolean = false
) {
    val cardHeight = if (compact) 92.dp else 110.dp
    Surface(
        modifier = modifier.height(cardHeight),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = if (compact) 8.dp else 10.dp, vertical = if (compact) 6.dp else 8.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "Aa",
                style = sampleStyle,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.76f),
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
        }
    }
}

@Composable
private fun ButtonsCard(
    modifier: Modifier = Modifier,
    compact: Boolean = false
) {
    val colors = MaterialTheme.colorScheme
    val cardHeight = if (compact) 92.dp else 110.dp
    val buttonHeight = if (compact) 26.dp else 30.dp
    val buttonPadding = if (compact) 6.dp else 10.dp
    val textStyle = if (compact) MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp) else MaterialTheme.typography.labelSmall

    Surface(
        modifier = modifier.height(cardHeight),
        shape = RoundedCornerShape(12.dp),
        color = colors.surfaceContainerHigh
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(if (compact) 6.dp else 8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Button(
                    onClick = {},
                    modifier = Modifier.weight(1f).height(buttonHeight),
                    shape = RoundedCornerShape(999.dp),
                    contentPadding = PaddingValues(horizontal = buttonPadding, vertical = 0.dp)
                ) {
                    Text("Primary", style = textStyle, maxLines = 1, overflow = TextOverflow.Clip)
                }
                Button(
                    onClick = {},
                    modifier = Modifier.weight(1f).height(buttonHeight),
                    shape = RoundedCornerShape(999.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.secondaryContainer,
                        contentColor = colors.onSecondaryContainer
                    ),
                    contentPadding = PaddingValues(horizontal = buttonPadding, vertical = 0.dp)
                ) {
                    Text("Secondary", style = textStyle, maxLines = 1, overflow = TextOverflow.Clip)
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Button(
                    onClick = {},
                    modifier = Modifier.weight(1f).height(buttonHeight),
                    shape = RoundedCornerShape(999.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.inverseSurface,
                        contentColor = colors.inverseOnSurface
                    ),
                    contentPadding = PaddingValues(horizontal = buttonPadding, vertical = 0.dp)
                ) {
                    Text(if (compact) "Invert" else "Inverted", style = textStyle, maxLines = 1, overflow = TextOverflow.Clip)
                }
                OutlinedButton(
                    onClick = {},
                    modifier = Modifier.weight(1f).height(buttonHeight),
                    shape = RoundedCornerShape(999.dp),
                    border = BorderStroke(1.dp, colors.outlineVariant),
                    contentPadding = PaddingValues(horizontal = buttonPadding, vertical = 0.dp)
                ) {
                    Text(if (compact) "Outline" else "Outlined", style = textStyle, maxLines = 1, overflow = TextOverflow.Clip)
                }
            }
        }
    }
}

@Composable
private fun SearchCard(
    modifier: Modifier = Modifier,
    compact: Boolean = false
) {
    val colors = MaterialTheme.colorScheme
    val cardHeight = if (compact) 92.dp else 110.dp
    Surface(
        modifier = modifier.height(cardHeight),
        shape = RoundedCornerShape(12.dp),
        color = colors.surfaceVariant.copy(alpha = 0.48f)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(if (compact) 8.dp else 10.dp),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                shape = RoundedCornerShape(999.dp),
                color = colors.surfaceContainerHighest,
                border = BorderStroke(1.dp, colors.outlineVariant.copy(alpha = 0.45f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = if (compact) 10.dp else 12.dp, vertical = if (compact) 6.dp else 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Search,
                        contentDescription = null,
                        modifier = Modifier.size(if (compact) 12.dp else 14.dp),
                        tint = colors.onSurfaceVariant
                    )
                    Text(
                        "Search",
                        style = if (compact) MaterialTheme.typography.labelSmall else MaterialTheme.typography.labelMedium,
                        color = colors.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun ProgressCard(
    modifier: Modifier = Modifier,
    compact: Boolean = false
) {
    val colors = MaterialTheme.colorScheme
    val cardHeight = if (compact) 92.dp else 110.dp
    Surface(
        modifier = modifier.height(cardHeight),
        shape = RoundedCornerShape(12.dp),
        color = colors.surfaceContainerHigh
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = if (compact) 8.dp else 10.dp, vertical = if (compact) 10.dp else 12.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.72f)
                    .height(4.dp)
                    .background(colors.primary, RoundedCornerShape(999.dp))
            )
            Box(
                modifier = Modifier
                    .padding(top = 8.dp)
                    .fillMaxWidth(0.9f)
                    .height(4.dp)
                    .background(colors.primary.copy(alpha = 0.6f), RoundedCornerShape(999.dp))
            )
            Box(
                modifier = Modifier
                    .padding(top = 8.dp)
                    .fillMaxWidth(0.58f)
                    .height(4.dp)
                    .background(colors.tertiary, RoundedCornerShape(999.dp))
            )
        }
    }
}

@Composable
private fun NavigationCard(
    modifier: Modifier = Modifier,
    compact: Boolean = false
) {
    val colors = MaterialTheme.colorScheme
    val cardHeight = if (compact) 92.dp else 110.dp
    Surface(
        modifier = modifier.height(cardHeight),
        shape = RoundedCornerShape(12.dp),
        color = colors.surfaceContainerHigh
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(if (compact) 8.dp else 10.dp),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                shape = RoundedCornerShape(if (compact) 12.dp else 14.dp),
                color = colors.surfaceContainerHighest
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = if (compact) 8.dp else 10.dp, vertical = if (compact) 6.dp else 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(if (compact) 10.dp else 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        modifier = Modifier.size(if (compact) 20.dp else 22.dp),
                        shape = CircleShape,
                        color = colors.primary
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Home,
                            contentDescription = null,
                            modifier = Modifier.padding(if (compact) 4.dp else 5.dp),
                            tint = colors.onPrimary
                        )
                    }
                    Icon(Icons.Rounded.Search, contentDescription = null, tint = colors.onSurfaceVariant, modifier = Modifier.size(if (compact) 15.dp else 17.dp))
                    Icon(Icons.Rounded.Person, contentDescription = null, tint = colors.onSurfaceVariant, modifier = Modifier.size(if (compact) 15.dp else 17.dp))
                }
            }
        }
    }
}

@Composable
private fun CompactIconCard(
    modifier: Modifier = Modifier,
    compact: Boolean = false
) {
    val colors = MaterialTheme.colorScheme
    val cardHeight = if (compact) 92.dp else 110.dp
    val iconSize = if (compact) 24.dp else 28.dp
    Surface(
        modifier = modifier.height(cardHeight),
        shape = RoundedCornerShape(12.dp),
        color = colors.surfaceContainerHigh
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxWidth()) {
            Surface(
                modifier = Modifier.size(iconSize),
                shape = CircleShape,
                color = colors.tertiary
            ) {
                Icon(
                    imageVector = Icons.Rounded.Edit,
                    contentDescription = null,
                    tint = colors.onTertiary,
                    modifier = Modifier.padding(if (compact) 6.dp else 7.dp)
                )
            }
        }
    }
}

@Composable
private fun ChipCard(
    modifier: Modifier = Modifier,
    compact: Boolean = false
) {
    val colors = MaterialTheme.colorScheme
    val cardHeight = if (compact) 92.dp else 110.dp
    Surface(
        modifier = modifier.height(cardHeight),
        shape = RoundedCornerShape(12.dp),
        color = colors.surfaceContainerHigh
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxWidth()) {
            Surface(
                shape = RoundedCornerShape(999.dp),
                color = colors.primaryContainer.copy(alpha = 0.88f)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = if (compact) 8.dp else 10.dp, vertical = if (compact) 5.dp else 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Edit,
                        contentDescription = null,
                        modifier = Modifier.size(if (compact) 11.dp else 12.dp),
                        tint = colors.onPrimaryContainer
                    )
                    Text(
                        "Label",
                        style = if (compact) MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp) else MaterialTheme.typography.labelSmall,
                        color = colors.onPrimaryContainer
                    )
                }
            }
        }
    }
}

@Composable
private fun IconRowCard(
    modifier: Modifier = Modifier,
    compact: Boolean = false
) {
    val colors = MaterialTheme.colorScheme
    val cardHeight = if (compact) 92.dp else 110.dp
    Surface(
        modifier = modifier.height(cardHeight),
        shape = RoundedCornerShape(12.dp),
        color = colors.surfaceContainerHigh
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = if (compact) 6.dp else 8.dp, vertical = if (compact) 8.dp else 10.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            PreviewCircleIcon(colors.primary, colors.onPrimary, Icons.Rounded.AutoStories, compact = compact)
            PreviewCircleIcon(colors.secondary, colors.onSecondary, Icons.Rounded.Tune, compact = compact)
            PreviewCircleIcon(colors.tertiary, colors.onTertiary, Icons.Rounded.Sell, compact = compact)
            PreviewCircleIcon(colors.error, colors.onError, Icons.Rounded.Delete, compact = compact)
        }
    }
}

@Composable
private fun PreviewCircleIcon(
    background: Color,
    content: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    compact: Boolean = false
) {
    val size = if (compact) 22.dp else 24.dp
    Surface(
        modifier = Modifier.size(size),
        shape = CircleShape,
        color = background
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = content,
            modifier = Modifier.padding(if (compact) 5.dp else 6.dp)
        )
    }
}

private fun gradientShade(base: Color, t: Float): Color {
    return if (t <= 0.45f) {
        lerp(Color.Black, base, t / 0.45f)
    } else {
        lerp(base, Color.White, (t - 0.45f) / 0.55f * 0.75f)
    }
}

private fun Color.onPreviewColor(): Color = if (luminance() > 0.5f) Color.Black else Color.White

private fun Color.toHexRgb(): String = String.format(Locale.US, "#%06X", toArgb() and 0xFFFFFF)
