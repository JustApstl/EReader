package com.dyu.ereader.ui.home.screens

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

private data class HomeNavDestination(
    val tabIndex: Int,
    val label: String,
    val icon: ImageVector,
    val showsBetaBadge: Boolean = false
)

@Composable
internal fun HomeDefaultBottomBar(
    currentTab: Int,
    onTabSelected: (Int) -> Unit,
    liquidGlassEnabled: Boolean,
    hideBetaFeatures: Boolean
) {
    Surface(
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
        shape = RoundedCornerShape(30.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = if (liquidGlassEnabled) 0.82f else 0.96f),
        tonalElevation = 0.dp,
        shadowElevation = 10.dp
    ) {
        HomeNavigationBar(
            currentTab = currentTab,
            onTabSelected = onTabSelected,
            hideBetaFeatures = hideBetaFeatures,
            containerColor = Color.Transparent,
            tonalElevation = 0.dp
        )
    }
}

@Composable
internal fun HomeFloatingBottomBar(
    currentTab: Int,
    onTabSelected: (Int) -> Unit,
    animationsEnabled: Boolean,
    liquidGlassEnabled: Boolean,
    hideBetaFeatures: Boolean,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(30.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = if (liquidGlassEnabled) 0.8f else 0.94f),
        tonalElevation = 0.dp,
        shadowElevation = 12.dp
    ) {
        HomeNavigationBar(
            currentTab = currentTab,
            onTabSelected = onTabSelected,
            hideBetaFeatures = hideBetaFeatures,
            containerColor = Color.Transparent,
            tonalElevation = 0.dp
        )
    }
}

@Composable
private fun HomeNavigationBar(
    currentTab: Int,
    onTabSelected: (Int) -> Unit,
    hideBetaFeatures: Boolean,
    containerColor: Color,
    tonalElevation: androidx.compose.ui.unit.Dp
) {
    NavigationBar(
        modifier = Modifier
            .fillMaxWidth()
            .height(82.dp),
        containerColor = containerColor,
        tonalElevation = tonalElevation
    ) {
        homeNavDestinations(hideBetaFeatures).forEach { destination ->
            val selected = currentTab == destination.tabIndex
            NavigationBarItem(
                selected = selected,
                onClick = { onTabSelected(destination.tabIndex) },
                icon = {
                    if (destination.showsBetaBadge) {
                        BadgedBox(
                            badge = {
                                Badge(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary
                                )
                            }
                        ) {
                            Icon(
                                imageVector = destination.icon,
                                contentDescription = destination.label
                            )
                        }
                    } else {
                        Icon(
                            imageVector = destination.icon,
                            contentDescription = destination.label
                        )
                    }
                },
                label = {
                    androidx.compose.material3.Text(
                        text = destination.label,
                        maxLines = 1
                    )
                },
                alwaysShowLabel = true,
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    selectedTextColor = MaterialTheme.colorScheme.onSurface,
                    indicatorColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }
    }
}

private fun homeNavDestinations(hideBetaFeatures: Boolean): List<HomeNavDestination> {
    return buildList {
        add(HomeNavDestination(0, "Library", Icons.AutoMirrored.Rounded.MenuBook))
        if (!hideBetaFeatures) {
            add(HomeNavDestination(1, "Browse", Icons.Rounded.Explore, showsBetaBadge = true))
        }
        add(HomeNavDestination(2, "Settings", Icons.Rounded.Settings))
    }
}
