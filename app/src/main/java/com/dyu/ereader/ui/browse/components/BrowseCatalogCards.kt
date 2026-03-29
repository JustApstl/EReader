package com.dyu.ereader.ui.browse.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import coil.compose.SubcomposeAsyncImageContent
import coil.request.ImageRequest
import com.dyu.ereader.data.model.browse.BrowseCatalog
import com.dyu.ereader.data.model.browse.CatalogHealthStatus
import com.dyu.ereader.ui.app.theme.UiTokens
import com.dyu.ereader.ui.components.surfaces.SectionSurface

@Composable
internal fun BrowseCatalogItem(
    catalog: BrowseCatalog,
    liquidGlassEnabled: Boolean,
    healthStatus: CatalogHealthStatus,
    onClick: () -> Unit,
    onRemoveCustom: (BrowseCatalog) -> Unit
) {
    val description = catalog.description
    val context = LocalContext.current
    val iconUrl = remember(catalog.icon, catalog.url) {
        catalog.icon?.takeIf { it.isNotBlank() }
            ?: "https://www.google.com/s2/favicons?domain_url=${catalog.url}&sz=64"
    }
    var showRemove by remember(catalog.id) { mutableStateOf(false) }
    val softError = MaterialTheme.colorScheme.error.copy(alpha = 0.8f)
    val softErrorContainer = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.25f)
    val backgroundLuminance = MaterialTheme.colorScheme.background.luminance()
    val isDarkTheme = backgroundLuminance < 0.45f
    val cardColor = if (liquidGlassEnabled) {
        MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.82f)
    } else if (isDarkTheme) {
        MaterialTheme.colorScheme.surfaceContainer
    } else {
        MaterialTheme.colorScheme.surfaceContainerLow
    }
    val cardBorderColor = if (liquidGlassEnabled) {
        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
    } else {
        MaterialTheme.colorScheme.outlineVariant.copy(alpha = if (isDarkTheme) 0.24f else 0.18f)
    }
    val iconChipColor = if (liquidGlassEnabled) {
        MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.48f)
    } else if (isDarkTheme) {
        MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.58f)
    } else {
        MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.78f)
    }
    SectionSurface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(UiTokens.SettingsCardShape)
            .combinedClickable(
                onClick = {
                    if (showRemove) showRemove = false else onClick()
                },
                onLongClick = {
                    if (catalog.isCustom) showRemove = true
                }
        ),
        shape = UiTokens.SettingsCardShape,
        color = cardColor,
        border = BorderStroke(1.dp, cardBorderColor),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
        contentPadding = PaddingValues(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(46.dp),
                shape = CircleShape,
                color = iconChipColor,
                border = BorderStroke(1.dp, cardBorderColor.copy(alpha = 0.8f))
            ) {
                SubcomposeAsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(iconUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = null,
                    modifier = Modifier.fillMaxWidth().padding(9.dp),
                    contentScale = ContentScale.Fit
                ) {
                    when (painter.state) {
                        is coil.compose.AsyncImagePainter.State.Success -> SubcomposeAsyncImageContent()
                        else -> {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Rounded.Language,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = catalog.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (!description.isNullOrBlank()) {
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (showRemove && catalog.isCustom) {
                    SourceActionChip(
                        onClick = {
                            showRemove = false
                            onRemoveCustom(catalog)
                        },
                        label = "Remove",
                        icon = Icons.Rounded.CloudOff,
                        containerColor = softErrorContainer,
                        contentColor = softError,
                        borderColor = softError.copy(alpha = 0.2f)
                    )
                } else {
                    CatalogHealthBadge(
                        healthStatus = healthStatus,
                        liquidGlassEnabled = liquidGlassEnabled,
                        isDarkTheme = isDarkTheme
                    )
                    SourceActionChip(
                        onClick = onClick,
                        label = "Open",
                        icon = Icons.AutoMirrored.Rounded.ArrowForward,
                        containerColor = iconChipColor,
                        contentColor = MaterialTheme.colorScheme.primary,
                        borderColor = cardBorderColor
                    )
                }
            }
        }
    }
}

@Composable
private fun CatalogHealthBadge(
    healthStatus: CatalogHealthStatus,
    liquidGlassEnabled: Boolean,
    isDarkTheme: Boolean
) {
    val (label, color, icon) = when (healthStatus) {
        CatalogHealthStatus.ONLINE -> Triple("Online", MaterialTheme.colorScheme.tertiary, Icons.Rounded.CheckCircle)
        CatalogHealthStatus.ERROR -> Triple("Offline", MaterialTheme.colorScheme.error, Icons.Rounded.CloudOff)
        CatalogHealthStatus.CHECKING -> Triple("Checking", MaterialTheme.colorScheme.secondary, Icons.Rounded.WarningAmber)
        CatalogHealthStatus.UNKNOWN -> Triple("Unknown", MaterialTheme.colorScheme.outline, Icons.Rounded.WarningAmber)
    }
    val containerColor = if (liquidGlassEnabled) {
        color.copy(alpha = 0.16f)
    } else if (isDarkTheme) {
        color.copy(alpha = 0.2f)
    } else {
        color.copy(alpha = 0.12f)
    }
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(containerColor)
            .padding(horizontal = 8.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(12.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, color = color, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
internal fun AddCatalogButton(
    onClick: () -> Unit,
    liquidGlassEnabled: Boolean
) {
    val backgroundLuminance = MaterialTheme.colorScheme.background.luminance()
    val isDarkTheme = backgroundLuminance < 0.45f
    val cardColor = if (liquidGlassEnabled) {
        MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.82f)
    } else if (isDarkTheme) {
        MaterialTheme.colorScheme.surfaceContainer
    } else {
        MaterialTheme.colorScheme.surfaceContainerLow
    }
    val cardBorderColor = if (liquidGlassEnabled) {
        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
    } else {
        MaterialTheme.colorScheme.outlineVariant.copy(alpha = if (isDarkTheme) 0.24f else 0.18f)
    }
    val iconChipColor = if (liquidGlassEnabled) {
        MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.48f)
    } else if (isDarkTheme) {
        MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.58f)
    } else {
        MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.78f)
    }
    SectionSurface(
        modifier = Modifier.fillMaxWidth(),
        shape = UiTokens.SettingsCardShape,
        color = cardColor,
        border = BorderStroke(1.dp, cardBorderColor),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
        contentPadding = PaddingValues(24.dp, 18.dp),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                modifier = Modifier.size(44.dp),
                shape = CircleShape,
                color = iconChipColor,
                border = BorderStroke(1.dp, cardBorderColor.copy(alpha = 0.8f))
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.Add, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Add Source",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Add another OPDS catalog.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Surface(
                shape = RoundedCornerShape(999.dp),
                color = iconChipColor,
                border = BorderStroke(1.dp, cardBorderColor.copy(alpha = 0.8f))
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(10.dp).size(16.dp)
                )
            }
        }
    }
}

@Composable
private fun SourceActionChip(
    onClick: () -> Unit,
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    containerColor: Color,
    contentColor: Color,
    borderColor: Color
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(18.dp),
        color = containerColor,
        border = BorderStroke(1.dp, borderColor)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = contentColor
            )
        }
    }
}
