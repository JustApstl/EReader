package com.dyu.ereader.ui.components.refresh

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EReaderPullRefreshIndicator(
    state: PullToRefreshState,
    isRefreshing: Boolean,
    modifier: Modifier = Modifier
) {
    val distanceFraction = state.distanceFraction.coerceIn(0f, 1.15f)
    val isVisible = isRefreshing || distanceFraction > 0f
    if (!isVisible) return

    val alpha = animateFloatAsState(
        targetValue = if (isRefreshing) 1f else (0.46f + (distanceFraction.coerceIn(0f, 1f) * 0.54f)),
        label = "PullRefreshAlpha"
    )
    val scale = animateFloatAsState(
        targetValue = if (isRefreshing) 1f else (0.9f + (distanceFraction.coerceIn(0f, 1f) * 0.1f)),
        label = "PullRefreshScale"
    )
    val rotation = if (isRefreshing) 0f else distanceFraction.coerceIn(0f, 1f) * 180f
    val supportingText = when {
        isRefreshing -> "Refreshing"
        distanceFraction >= 1f -> "Release to update"
        else -> "Pull to refresh"
    }

    Surface(
        modifier = modifier.graphicsLayer(
            alpha = alpha.value,
            scaleX = scale.value,
            scaleY = scale.value
        ),
        shape = RoundedCornerShape(999.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.98f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.28f)),
        shadowElevation = 10.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.78f)
            ) {
                if (isRefreshing) {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .padding(8.dp)
                            .size(18.dp),
                        strokeWidth = 2.2.dp,
                        color = MaterialTheme.colorScheme.primary
                    )
                } else {
                    Icon(
                        imageVector = Icons.Rounded.Sync,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .padding(8.dp)
                            .size(18.dp)
                            .graphicsLayer(rotationZ = rotation)
                    )
                }
            }
            Text(
                text = supportingText,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
