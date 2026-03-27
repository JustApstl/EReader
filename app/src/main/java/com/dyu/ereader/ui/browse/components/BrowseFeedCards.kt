package com.dyu.ereader.ui.browse.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.MarqueeAnimationMode
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.LocalIndication
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import coil.compose.SubcomposeAsyncImageContent
import coil.request.ImageRequest
import com.dyu.ereader.data.model.browse.BrowseBook
import com.dyu.ereader.data.repository.browse.BROWSE_USER_AGENT
import com.dyu.ereader.ui.components.books.BookMetadataChip
import kotlinx.coroutines.delay
import androidx.compose.animation.core.animateFloatAsState

@Composable
internal fun BrowseShelfRow(
    books: List<BrowseBook>,
    liquidGlassEnabled: Boolean,
    textScrollerEnabled: Boolean,
    onBookClick: (BrowseBook) -> Unit,
    cardWidth: Dp,
    modifier: Modifier = Modifier,
    contentPadding: androidx.compose.foundation.layout.PaddingValues = androidx.compose.foundation.layout.PaddingValues(vertical = 4.dp)
) {
    LazyRow(
        modifier = modifier,
        contentPadding = contentPadding,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(books) { book ->
            BrowseBookCard(
                book = book,
                liquidGlassEnabled = liquidGlassEnabled,
                textScrollerEnabled = textScrollerEnabled,
                onClick = { onBookClick(book) },
                modifier = Modifier.width(cardWidth)
            )
        }
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
internal fun BrowseBookCard(
    book: BrowseBook,
    liquidGlassEnabled: Boolean,
    textScrollerEnabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.98f else 1f, label = "BrowseCardScale")
    val showAuthor = book.author.isNotBlank() && !book.author.equals("Unknown Author", ignoreCase = true)
    val titleMarqueeEnabled = remember(book.title, textScrollerEnabled) {
        textScrollerEnabled && book.title.length > 28
    }
    val authorMarqueeEnabled = remember(book.author, textScrollerEnabled) {
        textScrollerEnabled && book.author.length > 22
    }
    val titleMarqueeModifier = marqueeModifier(titleMarqueeEnabled, book.title.length)
    val authorMarqueeModifier = marqueeModifier(authorMarqueeEnabled, book.author.length)
    val cardColor = if (liquidGlassEnabled) {
        MaterialTheme.colorScheme.surface.copy(alpha = 0.72f)
    } else {
        MaterialTheme.colorScheme.surfaceContainerLow
    }
    val coverOverlay = MaterialTheme.colorScheme.scrim.copy(alpha = 0.28f)
    Column(
        modifier = modifier
            .graphicsLayer(scaleX = scale, scaleY = scale)
            .clickable(
                onClick = onClick,
                interactionSource = interactionSource,
                indication = LocalIndication.current
            )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(0.72f),
            shape = RoundedCornerShape(12.dp),
            color = cardColor,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.22f)),
            tonalElevation = 0.dp,
            shadowElevation = 0.dp
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                BrowseCoverImage(
                    coverUrl = book.coverUrl,
                    title = book.title,
                    author = book.author,
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .align(Alignment.BottomCenter)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    coverOverlay
                                )
                            )
                        )
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = book.title,
            style = MaterialTheme.typography.labelLarge,
            maxLines = if (titleMarqueeEnabled) 1 else 2,
            overflow = if (titleMarqueeEnabled) TextOverflow.Clip else TextOverflow.Ellipsis,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = titleMarqueeModifier
        )
        if (showAuthor) {
            Text(
                text = book.author,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = if (authorMarqueeEnabled) TextOverflow.Clip else TextOverflow.Ellipsis,
                modifier = authorMarqueeModifier
            )
        }
    }
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

@Composable
internal fun BrowseCoverImage(
    coverUrl: String?,
    title: String,
    author: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val needsGutenbergReferer = coverUrl?.contains("gutenberg.org", ignoreCase = true) == true
    SubcomposeAsyncImage(
        model = ImageRequest.Builder(context)
            .data(coverUrl)
            .addHeader("User-Agent", BROWSE_USER_AGENT)
            .apply {
                if (needsGutenbergReferer) {
                    addHeader("Referer", "https://www.gutenberg.org/")
                }
            }
            .crossfade(true)
            .build(),
        contentDescription = title,
        contentScale = ContentScale.Crop,
        modifier = modifier
    ) {
        when (painter.state) {
            is coil.compose.AsyncImagePainter.State.Success -> {
                SubcomposeAsyncImageContent()
            }
            is coil.compose.AsyncImagePainter.State.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.84f))
                )
            }
            else -> {
                CoverPlaceholder(title = title, author = author)
            }
        }
    }
}

@Composable
private fun CoverPlaceholder(title: String, author: String) {
    val gradient = Brush.verticalGradient(
        listOf(
            MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.92f),
            MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.98f)
        )
    )
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(gradient)
    ) {
        Icon(
            Icons.AutoMirrored.Rounded.MenuBook,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f),
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(12.dp)
                .size(28.dp)
        )
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            if (author.isNotBlank() && !author.equals("Unknown Author", ignoreCase = true)) {
                Text(
                    text = author,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
internal fun FormatChip(label: String) {
    BookMetadataChip(label = label)
}
