package com.dyu.ereader.ui.home.components

import android.graphics.BitmapFactory
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.dyu.ereader.data.model.library.BookItem
import com.dyu.ereader.ui.components.buttons.AppChromeIconButton
import com.dyu.ereader.ui.components.surfaces.LiquidGlassOverlay
import com.dyu.ereader.ui.components.surfaces.rememberLiquidGlassStyle

@Composable
fun FavoriteBookCard(
    book: BookItem,
    onClick: () -> Unit,
    onShowActions: (BookItem) -> Unit,
    modifier: Modifier = Modifier,
    liquidGlassEnabled: Boolean = false
) {
    val coverBitmap = remember(book.id) { book.coverImage?.let { BitmapFactory.decodeByteArray(it, 0, it.size)?.asImageBitmap() } }
    val glassStyle = rememberLiquidGlassStyle()
    val cardColor = if (liquidGlassEnabled) glassStyle.containerColor else MaterialTheme.colorScheme.surfaceContainerLow
    val cardBorder = if (liquidGlassEnabled) glassStyle.border else null

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

                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                ) {
                    AppChromeIconButton(
                        icon = Icons.Rounded.MoreVert,
                        contentDescription = "Options",
                        onClick = { onShowActions(book) },
                        size = 30.dp,
                        iconSize = 16.dp
                    )
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
