package com.dyu.ereader.ui.home.components.library

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.AutoStories
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material.icons.rounded.HistoryEdu
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Psychology
import androidx.compose.material.icons.rounded.Science
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SelfImprovement
import androidx.compose.material.icons.rounded.TheaterComedy
import androidx.compose.material.icons.rounded.TravelExplore
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.absoluteValue

@Composable
internal fun ExploreGenresSection(
    genres: List<String>,
    selectedGenres: Set<String>,
    onGenreClick: (String) -> Unit
) {
    var showAllGenres by rememberSaveable(genres) { mutableStateOf(false) }
    val shownGenres = if (showAllGenres) genres else genres.take(4)
    Text(
        "Explore Genres",
        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
        color = MaterialTheme.colorScheme.onSurface
    )
    Column(
        modifier = Modifier.padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        shownGenres.chunked(2).forEach { rowGenres ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                rowGenres.forEach { genre ->
                    ExploreGenreCard(
                        genre = genre,
                        selected = selectedGenres.contains(genre),
                        onClick = { onGenreClick(genre) },
                        modifier = Modifier.weight(1f)
                    )
                }
                repeat(2 - rowGenres.size) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .clickable(enabled = !showAllGenres) { showAllGenres = true }
                .background(MaterialTheme.colorScheme.surfaceContainerLow)
                .padding(horizontal = 12.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (showAllGenres) "All categories shown" else "View all categories",
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f)
            )
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
private fun ExploreGenreCard(
    genre: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = genreCardPalette(genre)
    val containerColor = if (selected) {
        palette.container.copy(alpha = 0.92f)
    } else {
        palette.container
    }
    val contentColor = if (selected) {
        MaterialTheme.colorScheme.primary
    } else {
        palette.content
    }
    Surface(
        onClick = onClick,
        modifier = modifier.aspectRatio(1f),
        shape = RoundedCornerShape(10.dp),
        color = containerColor,
        border = if (selected) {
            BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
        } else {
            null
        }
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp, vertical = 11.dp)
        ) {
            Icon(
                imageVector = iconForGenre(genre),
                contentDescription = null,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .size(16.dp),
                tint = contentColor
            )
            Text(
                text = genre.uppercase(),
                modifier = Modifier.align(Alignment.BottomStart),
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.8.sp
                ),
                color = contentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

private data class GenreCardPalette(
    val container: Color,
    val content: Color
)

@Composable
private fun genreCardPalette(genre: String): GenreCardPalette {
    val palettes = listOf(
        GenreCardPalette(MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.onPrimaryContainer),
        GenreCardPalette(MaterialTheme.colorScheme.secondaryContainer, MaterialTheme.colorScheme.onSecondaryContainer),
        GenreCardPalette(MaterialTheme.colorScheme.tertiaryContainer, MaterialTheme.colorScheme.onTertiaryContainer),
        GenreCardPalette(MaterialTheme.colorScheme.surfaceContainerHigh, MaterialTheme.colorScheme.onSurface),
        GenreCardPalette(MaterialTheme.colorScheme.surfaceContainerHighest, MaterialTheme.colorScheme.onSurface)
    )
    return remember(genre) {
        palettes[(genre.lowercase().hashCode().absoluteValue % palettes.size)]
    }
}

private fun iconForGenre(genre: String): ImageVector {
    val value = genre.lowercase()
    return when {
        "history" in value || "biography" in value -> Icons.Rounded.HistoryEdu
        "science fiction" in value || "sci-fi" in value || "science" in value || "technology" in value -> Icons.Rounded.Science
        "fantasy" in value || "magic" in value || "myth" in value -> Icons.Rounded.AutoAwesome
        "mystery" in value || "crime" in value || "thriller" in value -> Icons.Rounded.Search
        "fiction" in value || "novel" in value -> Icons.Rounded.AutoStories
        "philosophy" in value || "psychology" in value -> Icons.Rounded.Psychology
        "travel" in value || "adventure" in value -> Icons.Rounded.TravelExplore
        "poetry" in value || "drama" in value -> Icons.Rounded.TheaterComedy
        "religion" in value || "spiritual" in value -> Icons.Rounded.SelfImprovement
        "art" in value || "design" in value -> Icons.Rounded.Palette
        "education" in value || "reference" in value -> Icons.AutoMirrored.Rounded.MenuBook
        else -> Icons.Rounded.Category
    }
}
