package com.dyu.ereader.ui.components.cards

import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.MarqueeAnimationMode
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dyu.ereader.data.model.app.AppTheme
import com.dyu.ereader.data.model.library.BookType
import com.dyu.ereader.data.model.library.BookItem
import com.dyu.ereader.ui.app.theme.UiTokens
import com.dyu.ereader.ui.components.buttons.AppChromeIconButton
import com.dyu.ereader.ui.components.surfaces.LiquidGlassOverlay
import com.dyu.ereader.ui.components.surfaces.rememberLiquidGlassStyle

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun BookCard(
    book: BookItem,
    onClick: () -> Unit,
    onToggleFavorite: (String, Boolean) -> Unit,
    onShowActions: (BookItem) -> Unit = {},
    onShowInfo: (BookItem) -> Unit,
    onDelete: (BookItem) -> Unit = {},
    showBookType: Boolean = true,
    showFavoriteButton: Boolean = true,
    showProgress: Boolean = false,
    isNew: Boolean = false,
    gridColumns: Int = 2,
    appTheme: AppTheme = AppTheme.SYSTEM,
    liquidGlassEnabled: Boolean = false,
    textScrollerEnabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    val coverBitmap = remember(book.id) {
        book.coverImage?.let {
            BitmapFactory.decodeByteArray(it, 0, it.size)?.asImageBitmap()
        }
    }
    var showMenu by remember { mutableStateOf(false) }

    val titleMarqueeEnabled = remember(book.title, gridColumns, textScrollerEnabled) {
        if (!textScrollerEnabled) return@remember false
        val threshold = if (gridColumns <= 2) 26 else 20
        book.title.length > threshold
    }
    val authorMarqueeEnabled = remember(book.author, gridColumns, textScrollerEnabled) {
        if (!textScrollerEnabled) return@remember false
        val threshold = if (gridColumns <= 2) 20 else 16
        book.author.length > threshold
    }

    val glassStyle = rememberLiquidGlassStyle()
    val cardShape = UiTokens.CardShape
    val cardBgColor = if (liquidGlassEnabled) glassStyle.containerColor else MaterialTheme.colorScheme.surface.copy(alpha = 0.98f)
    val borderStroke = if (liquidGlassEnabled) {
        glassStyle.border
    } else {
        BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.28f))
    }
    val coverOverlay = MaterialTheme.colorScheme.scrim.copy(alpha = 0.24f)
    val progressBadgeContainer = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.92f)
    val progressBadgeContent = MaterialTheme.colorScheme.onSurface

    val titleFontSize = when(gridColumns) {
        2 -> 13.sp
        3 -> 11.sp
        4 -> 10.sp
        else -> 11.sp
    }
    val authorFontSize = when(gridColumns) {
        2 -> 11.sp
        3 -> 9.sp
        4 -> 8.sp
        else -> 9.sp
    }
    val titleMarqueeModifier = marqueeModifier(titleMarqueeEnabled, book.title.length)
    val authorMarqueeModifier = marqueeModifier(authorMarqueeEnabled, book.author.length)
    val readingStatus = remember(book.progress) {
        when {
            book.progress >= 0.98f -> "FINISHED"
            book.progress > 0f -> "IN PROGRESS"
            else -> null
        }
    }

    Column(
        modifier = modifier
            .clip(cardShape)
            .clickable(onClick = onClick)
            .padding(bottom = 4.dp)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(0.72f)
                .shadow(
                    elevation = if (liquidGlassEnabled) 2.dp else 10.dp,
                    shape = cardShape,
                    clip = false
                ),
            shape = cardShape,
            colors = CardDefaults.cardColors(containerColor = cardBgColor),
            border = borderStroke
        ) {
            Box(modifier = Modifier.fillMaxSize().clip(cardShape)) {
                if (coverBitmap != null) {
                    Image(
                        bitmap = coverBitmap,
                        contentDescription = "${book.title} cover",
                        modifier = Modifier.fillMaxSize().clip(cardShape),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    val placeholderBrush = Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = if (liquidGlassEnabled) 0.12f else 0.18f),
                            MaterialTheme.colorScheme.surface.copy(alpha = if (liquidGlassEnabled) 0.16f else 0.24f)
                        )
                    )
                    Box(
                        Modifier
                            .fillMaxSize()
                            .clip(cardShape)
                            .background(placeholderBrush),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.MenuBook,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = if (liquidGlassEnabled) 0.78f else 0.58f),
                            modifier = Modifier.size(if (gridColumns <= 2) 48.dp else 32.dp)
                        )
                    }
                }

                if (liquidGlassEnabled) {
                    LiquidGlassOverlay(
                        shape = cardShape,
                        intensity = if (coverBitmap != null) 0.55f else 1f
                    )
                }
                
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .align(Alignment.BottomCenter)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, coverOverlay)
                            )
                        )
                )

                if (showBookType || isNew || readingStatus != null) {
                    Column(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        if (showBookType) {
                            val badgeColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = if (liquidGlassEnabled) 0.9f else 1f)
                            val badgeTextColor = MaterialTheme.colorScheme.onPrimaryContainer
                            Surface(
                                color = badgeColor,
                                shape = RoundedCornerShape(6.dp),
                                border = BorderStroke(0.6.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f))
                            ) {
                                Text(
                                    text = book.type.label,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Black
                                    ),
                                    color = badgeTextColor
                                )
                            }
                        }
                        if (isNew) {
                            Surface(
                                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = if (liquidGlassEnabled) 0.9f else 1f),
                                shape = RoundedCornerShape(6.dp),
                                border = BorderStroke(0.6.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.4f))
                            ) {
                                Text(
                                    text = "NEW",
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Black
                                    ),
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                        }
                        readingStatus?.let { status ->
                            val statusColor = if (status == "FINISHED") {
                                MaterialTheme.colorScheme.tertiaryContainer
                            } else {
                                MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.9f)
                            }
                            val statusTextColor = if (status == "FINISHED") {
                                MaterialTheme.colorScheme.onTertiaryContainer
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
                            Surface(
                                color = statusColor.copy(alpha = if (liquidGlassEnabled) 0.9f else 1f),
                                shape = RoundedCornerShape(6.dp),
                                border = BorderStroke(0.6.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                            ) {
                                Text(
                                    text = status,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Black
                                    ),
                                    color = statusTextColor
                                )
                            }
                        }
                    }
                }

                BookActionDock(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp),
                    isFavorite = book.isFavorite,
                    showFavoriteButton = showFavoriteButton,
                    onToggleFavorite = { onToggleFavorite(book.id, !book.isFavorite) },
                    onShowActions = { onShowActions(book) },
                    isLiquidGlass = liquidGlassEnabled
                )

                if (showProgress && book.progress > 0) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.BottomCenter)
                    ) {
                        Surface(
                            modifier = Modifier
                                .align(Alignment.End)
                                .padding(end = 8.dp, bottom = 4.dp),
                            color = progressBadgeContainer,
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = "${(book.progress * 100).toInt()}%",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp, fontWeight = FontWeight.Bold),
                                color = progressBadgeContent,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                        }
                        LinearProgressIndicator(
                            progress = { book.progress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(4.dp)
                                .clip(RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp)),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = Color.Transparent
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = book.title,
                style = MaterialTheme.typography.labelLarge.copy(
                    fontSize = titleFontSize,
                    lineHeight = titleFontSize * 1.25,
                    fontWeight = FontWeight.ExtraBold
                ),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = if (titleMarqueeEnabled) 1 else 2,
                overflow = if (titleMarqueeEnabled) TextOverflow.Clip else TextOverflow.Ellipsis,
                modifier = titleMarqueeModifier
            )
            Text(
                text = book.author,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = authorFontSize,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.82f)
                ),
                maxLines = 1,
                overflow = if (authorMarqueeEnabled) TextOverflow.Clip else TextOverflow.Ellipsis,
                modifier = authorMarqueeModifier
            )
        }
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun BookListItem(
    book: BookItem,
    onClick: () -> Unit,
    onToggleFavorite: (String, Boolean) -> Unit,
    onShowActions: (BookItem) -> Unit = {},
    onShowInfo: (BookItem) -> Unit,
    onDelete: (BookItem) -> Unit = {},
    showBookType: Boolean = true,
    showFavoriteButton: Boolean = true,
    showProgress: Boolean = false,
    isNew: Boolean = false,
    appTheme: AppTheme = AppTheme.SYSTEM,
    liquidGlassEnabled: Boolean = false,
    textScrollerEnabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    val coverBitmap = remember(book.id) {
        book.coverImage?.let {
            BitmapFactory.decodeByteArray(it, 0, it.size)?.asImageBitmap()
        }
    }
    val titleMarqueeEnabled = remember(book.title, textScrollerEnabled) {
        textScrollerEnabled && book.title.length > 32
    }
    val authorMarqueeEnabled = remember(book.author, textScrollerEnabled) {
        textScrollerEnabled && book.author.length > 24
    }
    val listTitleMarqueeModifier = marqueeModifier(titleMarqueeEnabled, book.title.length)
    val listAuthorMarqueeModifier = marqueeModifier(authorMarqueeEnabled, book.author.length)
    val readingStatus = remember(book.progress) {
        when {
            book.progress >= 0.98f -> "FINISHED"
            book.progress > 0f -> "IN PROGRESS"
            else -> null
        }
    }
    val glassListStyle = rememberLiquidGlassStyle()
    val bgColor = if (liquidGlassEnabled) glassListStyle.containerColor else MaterialTheme.colorScheme.surfaceContainerLow
    val borderStroke = if (liquidGlassEnabled) glassListStyle.border else null

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = UiTokens.CardShape,
        color = bgColor,
        border = borderStroke,
        shadowElevation = UiTokens.SectionShadowElevation
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier
                    .size(width = 64.dp, height = 88.dp)
                    .shadow(6.dp, RoundedCornerShape(14.dp)),
                shape = RoundedCornerShape(14.dp),
                color = if (liquidGlassEnabled) {
                    MaterialTheme.colorScheme.surface.copy(alpha = 0.6f)
                } else {
                    MaterialTheme.colorScheme.surfaceContainerLow
                },
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.22f))
            ) {
                Box(modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(14.dp)), contentAlignment = Alignment.Center) {
                    if (coverBitmap != null) {
                        Image(
                            bitmap = coverBitmap,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(14.dp)),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Icon(Icons.AutoMirrored.Rounded.MenuBook, contentDescription = null, modifier = Modifier.size(24.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                    }
                    
                    if (showProgress && book.progress > 0) {
                        LinearProgressIndicator(
                            progress = { book.progress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(4.dp)
                                .align(Alignment.BottomCenter)
                                .clip(RoundedCornerShape(bottomStart = 8.dp, bottomEnd = 8.dp)),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = Color.Transparent
                        )
                    }

                    if (liquidGlassEnabled) {
                        LiquidGlassOverlay(
                            shape = RoundedCornerShape(14.dp),
                            intensity = if (coverBitmap != null) 0.45f else 0.95f
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = book.title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, lineHeight = 20.sp),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = if (titleMarqueeEnabled) 1 else 2,
                    overflow = if (titleMarqueeEnabled) TextOverflow.Clip else TextOverflow.Ellipsis,
                    modifier = listTitleMarqueeModifier
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = book.author,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    maxLines = 1,
                    overflow = if (authorMarqueeEnabled) TextOverflow.Clip else TextOverflow.Ellipsis,
                    modifier = listAuthorMarqueeModifier
                )
                
                Row(
                    modifier = Modifier.padding(top = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (isNew) {
                        Surface(
                            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = "NEW",
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Black),
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                    if (showBookType) {
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = book.type.label,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Black),
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                    readingStatus?.let { status ->
                        val statusColor = if (status == "FINISHED") {
                            MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.75f)
                        } else {
                            MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.92f)
                        }
                        val statusTextColor = if (status == "FINISHED") {
                            MaterialTheme.colorScheme.onTertiaryContainer
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                        Surface(
                            color = statusColor,
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = status,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Black),
                                color = statusTextColor
                            )
                        }
                    }

                    if (showProgress && book.progress > 0) {
                        Text(
                            text = "${(book.progress * 100).toInt()}% read",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
            
            BookActionDock(
                isFavorite = book.isFavorite,
                showFavoriteButton = showFavoriteButton,
                onToggleFavorite = { onToggleFavorite(book.id, !book.isFavorite) },
                onShowActions = { onShowActions(book) },
                isLiquidGlass = liquidGlassEnabled
            )
        }
    }
}

@Composable
private fun BookActionDock(
    isFavorite: Boolean,
    showFavoriteButton: Boolean,
    onToggleFavorite: () -> Unit,
    onShowActions: () -> Unit,
    isLiquidGlass: Boolean = false,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        color = if (isLiquidGlass) {
            MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)
        } else {
            MaterialTheme.colorScheme.surface.copy(alpha = 0.92f)
        },
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)),
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            if (showFavoriteButton) {
                BookOverlayActionButton(
                    icon = if (isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                    contentDescription = "Favorite",
                    onClick = onToggleFavorite,
                    selected = isFavorite,
                    destructive = isFavorite
                )
            }
            BookOverlayActionButton(
                icon = Icons.Rounded.MoreVert,
                contentDescription = "More",
                onClick = onShowActions
            )
        }
    }
}

@Composable
private fun BookOverlayActionButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    selected: Boolean = false,
    destructive: Boolean = false
) {
    AppChromeIconButton(
        icon = icon,
        contentDescription = contentDescription,
        onClick = onClick,
        selected = selected,
        destructive = destructive,
        size = 30.dp,
        iconSize = 16.dp
    )
}

@Composable
private fun FavoriteButton(
    isFavorite: Boolean,
    onToggle: () -> Unit,
    isLiquidGlass: Boolean = false,
    modifier: Modifier = Modifier
) {
    AppChromeIconButton(
        icon = if (isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
        contentDescription = "Favorite",
        onClick = onToggle,
        modifier = modifier,
        destructive = isFavorite,
        size = 32.dp,
        iconSize = 17.dp
    )
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
private fun marqueeModifier(enabled: Boolean, textLength: Int): Modifier {
    if (!enabled) return Modifier
    val velocity = 28.dp
    return Modifier.basicMarquee(
        iterations = Int.MAX_VALUE,
        animationMode = MarqueeAnimationMode.Immediately,
        initialDelayMillis = 900,
        repeatDelayMillis = 1400,
        velocity = velocity
    )
}
