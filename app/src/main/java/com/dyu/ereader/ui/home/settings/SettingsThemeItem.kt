package com.dyu.ereader.ui.home.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.padding
import com.dyu.ereader.data.model.app.AppTheme

@Composable
internal fun ThemeItem(
    theme: AppTheme,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val previewColor = when (theme) {
        AppTheme.LIGHT -> Color(0xFFF8F7FA)
        AppTheme.DARK -> Color(0xFF1B1B1F)
        AppTheme.BLACK -> Color(0xFF000000)
        AppTheme.SYSTEM -> MaterialTheme.colorScheme.surfaceContainerHigh
    }
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .background(previewColor, CircleShape)
                .clip(CircleShape)
                .border(
                    width = if (isSelected) 3.dp else 1.dp,
                    color = if (isSelected) {
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                    } else {
                        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.42f)
                    },
                    shape = CircleShape
                )
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            if (isSelected) {
                Icon(
                    imageVector = Icons.Rounded.Check,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = if (previewColor.luminance() > 0.55f) Color.Black else Color.White
                )
            }
        }

        Text(
            text = when (theme) {
                AppTheme.LIGHT -> "Light"
                AppTheme.DARK -> "Dark"
                AppTheme.BLACK -> "OLED"
                AppTheme.SYSTEM -> "System"
            },
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
            fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Bold,
            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 2.dp)
        )
    }
}
