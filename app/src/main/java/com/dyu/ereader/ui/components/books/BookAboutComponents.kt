package com.dyu.ereader.ui.components.books

import android.graphics.Typeface
import android.text.method.LinkMovementMethod
import android.widget.TextView
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.text.HtmlCompat
import com.dyu.ereader.ui.app.theme.UiTokens
import com.dyu.ereader.ui.components.buttons.AppChromeIconButton
import com.dyu.ereader.ui.components.insets.stableStatusBarsPadding
import com.dyu.ereader.ui.components.surfaces.SectionSurface

@Composable
fun BookAboutSection(
    title: String? = null,
    icon: ImageVector? = null,
    liquidGlassEnabled: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
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
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (title != null && icon != null) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(horizontal = 4.dp)
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
        }
        SectionSurface(
            modifier = Modifier.fillMaxWidth(),
            shape = UiTokens.SettingsCardShape,
            color = sectionColor,
            border = sectionBorder,
            tonalElevation = 0.dp,
            shadowElevation = 0.dp,
            contentPadding = androidx.compose.foundation.layout.PaddingValues(20.dp),
            content = content
        )
    }
}

@Composable
fun BookAboutTopBar(
    title: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .stableStatusBarsPadding()
            .padding(horizontal = 20.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        AppChromeIconButton(
            icon = androidx.compose.material.icons.Icons.AutoMirrored.Rounded.ArrowBack,
            contentDescription = "Back",
            onClick = onBack
        )
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (!subtitle.isNullOrBlank()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun BookMetadataChip(
    label: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.38f))
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
        )
    }
}

@Composable
fun BookPrimaryActionButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null,
    loading: Boolean = false,
    loadingLabel: String = label
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp),
        shape = RoundedCornerShape(18.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        )
    ) {
        if (loading) {
            androidx.compose.material3.CircularProgressIndicator(
                strokeWidth = 2.dp,
                modifier = Modifier.size(16.dp),
                color = MaterialTheme.colorScheme.onPrimary
            )
            Spacer(Modifier.width(10.dp))
            Text(
                text = loadingLabel,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.ExtraBold
            )
        } else {
            if (icon != null) {
                androidx.compose.material3.Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(8.dp))
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.ExtraBold
            )
        }
    }
}

@Composable
fun BookAboutDetailRow(
    icon: ImageVector,
    label: String,
    value: String
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        androidx.compose.material3.Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = "$label:",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun BookMetadataHtmlText(
    html: String,
    modifier: Modifier = Modifier
) {
    val textColor = MaterialTheme.colorScheme.onSurfaceVariant
    val textStyle = MaterialTheme.typography.bodyMedium
    val typeface = remember(textStyle.fontFamily) { textStyle.toPlatformTypeface() }
    val normalizedHtml = remember(html) { normalizeHtmlForDisplay(html) }
    AndroidView(
        modifier = modifier,
        factory = { context ->
            TextView(context).apply {
                setTextColor(textColor.toArgb())
                textSize = if (textStyle.fontSize == TextUnit.Unspecified) 14f else textStyle.fontSize.value
                setLineSpacing(0f, 1.2f)
                setTypeface(typeface, Typeface.NORMAL)
                movementMethod = LinkMovementMethod.getInstance()
            }
        },
        update = { view ->
            view.setTextColor(textColor.toArgb())
            view.textSize = if (textStyle.fontSize == TextUnit.Unspecified) 14f else textStyle.fontSize.value
            view.setTypeface(typeface, Typeface.NORMAL)
            view.text = HtmlCompat.fromHtml(normalizedHtml, HtmlCompat.FROM_HTML_MODE_LEGACY)
        }
    )
}

private fun TextStyle.toPlatformTypeface(): Typeface = when (fontFamily) {
    FontFamily.Serif -> Typeface.SERIF
    FontFamily.SansSerif -> Typeface.SANS_SERIF
    FontFamily.Monospace -> Typeface.MONOSPACE
    else -> Typeface.DEFAULT
}

private fun normalizeHtmlForDisplay(raw: String): String {
    if (raw.contains("<") && raw.contains(">")) return raw
    val looksEscaped = raw.contains("&lt;") && raw.contains("&gt;")
    return if (looksEscaped) {
        HtmlCompat.fromHtml(raw, HtmlCompat.FROM_HTML_MODE_LEGACY).toString()
    } else {
        raw
    }
}
