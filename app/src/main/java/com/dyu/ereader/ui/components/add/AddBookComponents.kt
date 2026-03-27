package com.dyu.ereader.ui.components.add

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.dyu.ereader.data.model.app.AppTheme

@Composable
fun AddBookCard(
    appTheme: AppTheme = AppTheme.SYSTEM,
    liquidGlassEnabled: Boolean = false,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val isDark = when (appTheme) {
        AppTheme.DARK, AppTheme.BLACK -> true
        AppTheme.LIGHT -> false
        AppTheme.SYSTEM -> isSystemInDarkTheme()
    }

    val bgColor = if (liquidGlassEnabled) {
        MaterialTheme.colorScheme.surface.copy(alpha = if (isDark) 0.5f else 0.58f)
    } else MaterialTheme.colorScheme.surface

    val borderColor = if (liquidGlassEnabled) {
        MaterialTheme.colorScheme.onSurface.copy(alpha = if (isDark) 0.2f else 0.14f)
    } else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)

    val borderStroke = BorderStroke(1.2.dp, borderColor)

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .padding(bottom = 4.dp)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(0.72f)
                .shadow(
                    elevation = if (liquidGlassEnabled) 1.dp else 0.dp,
                    shape = RoundedCornerShape(16.dp),
                    clip = false
                ),
            shape = RoundedCornerShape(16.dp),
            color = bgColor,
            border = borderStroke
        ) {
            Box(contentAlignment = Alignment.Center) {
                val placeholderBrush = Brush.radialGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primary.copy(alpha = if (liquidGlassEnabled) 0.16f else 0.12f),
                        Color.Transparent
                    )
                )
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .background(placeholderBrush, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Add,
                            contentDescription = null,
                            modifier = Modifier.size(28.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Add Book",
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
fun AddBookListItem(
    appTheme: AppTheme = AppTheme.SYSTEM,
    liquidGlassEnabled: Boolean = false,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val isDark = when (appTheme) {
        AppTheme.DARK, AppTheme.BLACK -> true
        AppTheme.LIGHT -> false
        AppTheme.SYSTEM -> isSystemInDarkTheme()
    }

    val bgColor = if (liquidGlassEnabled) {
        MaterialTheme.colorScheme.surface.copy(alpha = if (isDark) 0.5f else 0.58f)
    } else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)

    Surface(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = bgColor,
        border = BorderStroke(
            1.2.dp,
            if (liquidGlassEnabled) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.16f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.Add,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "Import more books",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}
