package com.dyu.ereader.ui.home.components

import android.graphics.BitmapFactory
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.dyu.ereader.data.model.library.BookItem
import com.dyu.ereader.ui.components.surfaces.LiquidGlassOverlay
import com.dyu.ereader.ui.components.surfaces.rememberLiquidGlassStyle

@Composable
fun RecentBookCard(
    book: BookItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isNew: Boolean = false,
    liquidGlassEnabled: Boolean = false
) {
    val coverBitmap = remember(book.id) { book.coverImage?.let { BitmapFactory.decodeByteArray(it, 0, it.size)?.asImageBitmap() } }
    val glassStyle = rememberLiquidGlassStyle()
    val cardColor = if (liquidGlassEnabled) glassStyle.containerColor else MaterialTheme.colorScheme.surfaceContainerLow
    val cardBorder = if (liquidGlassEnabled) glassStyle.border else null
    val readingStatus = remember(book.progress) {
        when {
            book.progress >= 0.98f -> "FINISHED"
            book.progress > 0f -> "IN PROGRESS"
            else -> null
        }
    }
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Card(
            onClick = onClick,
            modifier = modifier.aspectRatio(0.72f),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = cardColor),
            border = cardBorder
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                if (coverBitmap != null) {
                    Image(coverBitmap, null, contentScale = androidx.compose.ui.layout.ContentScale.Crop, modifier = Modifier.fillMaxSize())
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
                            .background(placeholderBrush),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.AutoMirrored.Rounded.MenuBook,
                            null,
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = if (liquidGlassEnabled) 0.78f else 0.56f)
                        )
                    }
                }

                if (liquidGlassEnabled) {
                    LiquidGlassOverlay(
                        shape = RoundedCornerShape(12.dp),
                        intensity = if (coverBitmap != null) 0.5f else 0.95f
                    )
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    Color.Transparent,
                                    MaterialTheme.colorScheme.scrim.copy(alpha = 0.82f)
                                )
                            )
                        )
                        .padding(8.dp)
                ) {
                    LinearProgressIndicator(
                        progress = { book.progress.coerceIn(0f, 1f) },
                        modifier = Modifier.fillMaxWidth().height(4.dp).clip(CircleShape),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.28f)
                    )
                    Text(
                        text = "${(book.progress * 100).toInt()}%",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.align(Alignment.End).padding(top = 2.dp)
                    )
                }

                if (isNew || readingStatus != null) {
                    Column(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(6.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        if (isNew) {
                            Surface(
                                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.9f),
                                shape = RoundedCornerShape(6.dp),
                                border = BorderStroke(0.6.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.35f))
                            ) {
                                Text(
                                    text = "NEW",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black),
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        readingStatus?.let { status ->
                            val statusColor = if (status == "FINISHED") {
                                MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.9f)
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
                                shape = RoundedCornerShape(6.dp),
                                border = BorderStroke(0.6.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                            ) {
                                Text(
                                    text = status,
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black),
                                    color = statusTextColor,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(
            text = book.title,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .width(130.dp)
                .padding(horizontal = 4.dp)
                .align(Alignment.Start),
            textAlign = TextAlign.Start,
            color = if (liquidGlassEnabled) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.94f) else MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = book.author,
            style = MaterialTheme.typography.bodySmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .width(130.dp)
                .padding(horizontal = 4.dp)
                .align(Alignment.Start),
            textAlign = TextAlign.Start,
            color = if (liquidGlassEnabled) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f) else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun FeaturedReadingCard(
    book: BookItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val coverBitmap = remember(book.id) { book.coverImage?.let { BitmapFactory.decodeByteArray(it, 0, it.size)?.asImageBitmap() } }
    val progressPercent = (book.progress.coerceIn(0f, 1f) * 100).toInt()
    val estimatedMinutesLeft = ((1f - book.progress.coerceIn(0f, 1f)) * 60f).toInt().coerceAtLeast(1)

    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.18f))
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(240.dp)
                    .clip(RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                contentAlignment = Alignment.Center
            ) {
                if (coverBitmap != null) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        tonalElevation = 0.dp,
                        shadowElevation = 0.dp,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.18f))
                    ) {
                        Image(
                            bitmap = coverBitmap,
                            contentDescription = "${book.title} cover",
                            contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                            modifier = Modifier
                                .width(126.dp)
                                .aspectRatio(0.72f)
                        )
                    }
                } else {
                    Icon(
                        Icons.AutoMirrored.Rounded.MenuBook,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.65f),
                        modifier = Modifier.size(54.dp)
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.7f)
                ) {
                    Text(
                        text = "CURRENTLY READING",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
                Text(
                    text = book.title,
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.Medium
                    ),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "${book.author} • ${book.type.label}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                LinearProgressIndicator(
                    progress = { book.progress.coerceIn(0f, 1f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(CircleShape),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "$progressPercent% COMPLETE",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "$estimatedMinutesLeft MINS LEFT",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Button(
                    onClick = onClick,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "Resume Chapter",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}
