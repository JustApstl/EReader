package com.dyu.ereader.ui.browse.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.SearchOff
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.dyu.ereader.ui.components.surfaces.SectionSurface
import com.dyu.ereader.data.model.browse.BrowseCatalog
import com.dyu.ereader.data.model.browse.CatalogHealthStatus

@Composable
internal fun CatalogList(
    catalogs: List<BrowseCatalog>,
    liquidGlassEnabled: Boolean,
    catalogHealth: Map<String, CatalogHealthStatus>,
    onCatalogClick: (BrowseCatalog) -> Unit,
    onAddSourceClick: () -> Unit,
    onRemoveCustomSource: (BrowseCatalog) -> Unit
) {
    val onlineCatalogs = catalogs.filter { catalogHealth[it.id] != CatalogHealthStatus.ERROR }
    val offlineCatalogs = catalogs.filter { catalogHealth[it.id] == CatalogHealthStatus.ERROR }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 110.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        if (catalogs.isEmpty()) {
            item {
                Box(
                    modifier = Modifier.fillMaxWidth().height(240.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            Icons.Rounded.SearchOff,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.height(48.dp)
                        )
                        Text(
                            text = "No sources available yet.",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Button(onClick = onAddSourceClick) {
                            Text("Add Source")
                        }
                    }
                }
            }
        } else {
            if (onlineCatalogs.isNotEmpty()) {
                item { CatalogSectionHeader("Online Sources", onlineCatalogs.size, liquidGlassEnabled) }
                items(onlineCatalogs) { catalog ->
                    BrowseCatalogItem(
                        catalog = catalog,
                        liquidGlassEnabled = liquidGlassEnabled,
                        healthStatus = catalogHealth[catalog.id] ?: CatalogHealthStatus.UNKNOWN,
                        onClick = { onCatalogClick(catalog) },
                        onRemoveCustom = onRemoveCustomSource
                    )
                }
                item { AddCatalogButton(onClick = onAddSourceClick, liquidGlassEnabled = liquidGlassEnabled) }
            } else {
                item { AddCatalogButton(onClick = onAddSourceClick, liquidGlassEnabled = liquidGlassEnabled) }
            }

            if (offlineCatalogs.isNotEmpty()) {
                item { CatalogSectionHeader("Offline Sources", offlineCatalogs.size, liquidGlassEnabled) }
                items(offlineCatalogs) { catalog ->
                    BrowseCatalogItem(
                        catalog = catalog,
                        liquidGlassEnabled = liquidGlassEnabled,
                        healthStatus = catalogHealth[catalog.id] ?: CatalogHealthStatus.UNKNOWN,
                        onClick = { onCatalogClick(catalog) },
                        onRemoveCustom = onRemoveCustomSource
                    )
                }
            }
        }
    }
}

@Composable
private fun CatalogSectionHeader(
    title: String,
    count: Int,
    liquidGlassEnabled: Boolean
) {
    SectionSurface(
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
        shape = RoundedCornerShape(22.dp),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Surface(
                shape = RoundedCornerShape(999.dp),
                color = if (liquidGlassEnabled) {
                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.62f)
                } else {
                    MaterialTheme.colorScheme.surfaceContainerHigh
                }
            ) {
                Text(
                    text = "$count",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun BrowseCatalogOverviewCard(
    totalCatalogs: Int,
    onlineCatalogs: Int,
    offlineCatalogs: Int
) {
    SectionSurface(
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.36f),
        shape = RoundedCornerShape(26.dp),
        contentPadding = PaddingValues(18.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.AutoAwesome,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 2.dp)
                )
                Column {
                    Text(
                        text = "Browse Better Sources",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Jump between trusted catalogs, curated feeds, and downloadable books.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                BrowseOverviewPill("Sources", totalCatalogs.toString(), Modifier.weight(1f))
                BrowseOverviewPill("Online", onlineCatalogs.toString(), Modifier.weight(1f))
                BrowseOverviewPill("Offline", offlineCatalogs.toString(), Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun BrowseOverviewPill(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.padding(horizontal = 2.dp, vertical = 4.dp)) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
