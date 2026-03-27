package com.dyu.ereader.ui.home.overlays.sheets

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoStories
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.FilterAlt
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.Sell
import androidx.compose.material.icons.rounded.Sort
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.dyu.ereader.core.locale.displayLanguageName
import com.dyu.ereader.data.model.library.BookType
import com.dyu.ereader.ui.app.theme.UiTokens
import com.dyu.ereader.ui.components.buttons.AppChromeIconButton
import com.dyu.ereader.ui.components.surfaces.SectionSurface
import com.dyu.ereader.ui.home.state.HomeUiState
import com.dyu.ereader.ui.home.state.ReadingStatus
import com.dyu.ereader.ui.home.state.SortOrder

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun FilterBottomSheet(
    uiState: HomeUiState,
    onDismiss: () -> Unit,
    onToggleType: (BookType) -> Unit,
    onToggleGenre: (String) -> Unit,
    onToggleLanguage: (String) -> Unit,
    onToggleYear: (String) -> Unit,
    onToggleCountry: (String) -> Unit,
    onSortOrderChanged: (SortOrder) -> Unit,
    onToggleReadingStatus: (ReadingStatus) -> Unit,
    onReset: () -> Unit
) {
    val hasActiveFilters =
        uiState.sortOrder != SortOrder.TITLE ||
            uiState.selectedTypes.isNotEmpty() ||
            uiState.selectedGenres.isNotEmpty() ||
            uiState.selectedLanguages.isNotEmpty() ||
            uiState.selectedYears.isNotEmpty() ||
            uiState.selectedCountries.isNotEmpty() ||
            uiState.selectedStatuses.isNotEmpty()

    val liquidGlassEnabled = uiState.display.liquidGlassEffect
    val chipColors = FilterChipDefaults.filterChipColors(
        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.9f),
        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
        selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        labelColor = MaterialTheme.colorScheme.onSurface,
        iconColor = MaterialTheme.colorScheme.onSurfaceVariant
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        scrimColor = MaterialTheme.colorScheme.scrim.copy(alpha = 0.22f),
        tonalElevation = 0.dp,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        dragHandle = { BottomSheetDefaults.DragHandle(color = MaterialTheme.colorScheme.outlineVariant) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = "Filter Library",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = if (hasActiveFilters) "Refine books by format, status, language, and more." else "Choose filters to narrow your library.",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                if (hasActiveFilters) {
                    AppChromeIconButton(
                        icon = Icons.Rounded.RestartAlt,
                        contentDescription = "Reset filters",
                        onClick = onReset
                    )
                }
                AppChromeIconButton(
                    icon = Icons.Rounded.Close,
                    contentDescription = "Close filters",
                    onClick = onDismiss
                )
            }

            FilterSection(
                title = "Sort By",
                icon = Icons.Rounded.Tune,
                liquidGlassEnabled = liquidGlassEnabled
            ) {
                FilterChipGroup {
                    SortOrder.entries.forEach { order ->
                        FilterChip(
                            selected = uiState.sortOrder == order,
                            onClick = { onSortOrderChanged(order) },
                            label = { Text(order.label) },
                            colors = chipColors
                        )
                    }
                }
            }

            FilterSection(
                title = "Reading Status",
                icon = Icons.Rounded.AutoStories,
                liquidGlassEnabled = liquidGlassEnabled
            ) {
                FilterChipGroup {
                    ReadingStatus.entries.forEach { status ->
                        FilterChip(
                            selected = uiState.selectedStatuses.contains(status),
                            onClick = { onToggleReadingStatus(status) },
                            label = { Text(status.label) },
                            colors = chipColors
                        )
                    }
                }
            }

            FilterSection(
                title = "Formats",
                icon = Icons.Rounded.Tune,
                liquidGlassEnabled = liquidGlassEnabled
            ) {
                FilterChipGroup {
                    BookType.entries.forEach { type ->
                        FilterChip(
                            selected = uiState.selectedTypes.contains(type),
                            onClick = { onToggleType(type) },
                            label = { Text(type.label) },
                            colors = chipColors
                        )
                    }
                }
            }

            if (uiState.availableGenres.isNotEmpty()) {
                FilterSection(
                    title = "Genres",
                    icon = Icons.Rounded.Sell,
                    liquidGlassEnabled = liquidGlassEnabled
                ) {
                    FilterChipGroup {
                        uiState.availableGenres.forEach { genre ->
                            FilterChip(
                                selected = uiState.selectedGenres.contains(genre),
                                onClick = { onToggleGenre(genre) },
                                label = { Text(genre) },
                                colors = chipColors
                            )
                        }
                    }
                }
            }

            if (uiState.availableLanguages.isNotEmpty()) {
                FilterSection(
                    title = "Language",
                    icon = Icons.Rounded.Language,
                    liquidGlassEnabled = liquidGlassEnabled
                ) {
                    FilterChipGroup {
                        uiState.availableLanguages.forEach { language ->
                            val languageLabel = displayLanguageName(language) ?: language
                            FilterChip(
                                selected = uiState.selectedLanguages.contains(language),
                                onClick = { onToggleLanguage(language) },
                                label = { Text(languageLabel) },
                                colors = chipColors
                            )
                        }
                    }
                }
            }

            if (uiState.availableCountries.isNotEmpty()) {
                FilterSection(
                    title = "Country",
                    icon = Icons.Rounded.Public,
                    liquidGlassEnabled = liquidGlassEnabled
                ) {
                    FilterChipGroup {
                        uiState.availableCountries.forEach { country ->
                            FilterChip(
                                selected = uiState.selectedCountries.contains(country),
                                onClick = { onToggleCountry(country) },
                                label = { Text(country) },
                                colors = chipColors
                            )
                        }
                    }
                }
            }

            if (uiState.availableYears.isNotEmpty()) {
                FilterSection(
                    title = "Publish Year",
                    icon = Icons.Rounded.FilterAlt,
                    liquidGlassEnabled = liquidGlassEnabled
                ) {
                    FilterChipGroup {
                        uiState.availableYears.forEach { year ->
                            FilterChip(
                                selected = uiState.selectedYears.contains(year),
                                onClick = { onToggleYear(year) },
                                label = { Text(year) },
                                colors = chipColors
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(12.dp))
        }
    }
}

@Composable
private fun FilterSection(
    title: String,
    icon: ImageVector,
    liquidGlassEnabled: Boolean,
    content: @Composable () -> Unit
) {
    val sectionColor = if (liquidGlassEnabled) {
        MaterialTheme.colorScheme.surface.copy(alpha = 0.78f)
    } else {
        MaterialTheme.colorScheme.surfaceContainerLow
    }
    val sectionBorder = BorderStroke(
        1.dp,
        if (liquidGlassEnabled) {
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)
        } else {
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
        }
    )

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            androidx.compose.material3.Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        SectionSurface(
            modifier = Modifier.fillMaxWidth(),
            shape = UiTokens.SettingsCardShape,
            color = sectionColor,
            border = sectionBorder,
            tonalElevation = 0.dp,
            shadowElevation = 0.dp,
            contentPadding = androidx.compose.foundation.layout.PaddingValues(20.dp)
        ) {
            content()
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FilterChipGroup(
    content: @Composable () -> Unit
) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) { content() }
}
