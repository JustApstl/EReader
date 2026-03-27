package com.dyu.ereader.ui.home.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.dyu.ereader.ui.components.badges.BetaBadge
import com.dyu.ereader.ui.components.badges.LocalBetaFeaturesEnabled
import com.dyu.ereader.ui.components.buttons.AppChromeIconButton

@Composable
internal fun SettingsCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    liquidGlassEnabled: Boolean = false,
    beta: Boolean = false,
    onReset: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    if (beta && !LocalBetaFeaturesEnabled.current) return
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp)
        ) {
            Box {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(icon, null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
                    Text(
                        title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                if (beta) {
                    BetaBadge(Modifier.align(Alignment.TopEnd).offset(x = 32.dp, y = (-6).dp))
                }
            }
            if (onReset != null) {
                AppChromeIconButton(
                    icon = Icons.Rounded.RestartAlt,
                    contentDescription = "Reset $title",
                    onClick = onReset,
                    size = 36.dp,
                    iconSize = 18.dp
                )
            }
        }
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            content()
            Spacer(Modifier.height(2.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
        }
    }
}
