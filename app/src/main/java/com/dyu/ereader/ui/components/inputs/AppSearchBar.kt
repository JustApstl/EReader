package com.dyu.ereader.ui.components.inputs

import android.app.Activity
import android.content.Intent
import android.speech.RecognizerIntent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dyu.ereader.ui.components.buttons.AppChromeIconButton
import com.dyu.ereader.ui.app.theme.UiTokens
import java.util.Locale

@Composable
fun AppSearchBar(
    query: String,
    placeholder: String,
    onQueryChange: (String) -> Unit,
    onSearch: (String) -> Unit,
    liquidGlassEnabled: Boolean = false,
    focusRequester: FocusRequester? = null,
    autoFocus: Boolean = false,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val activeFocusRequester = focusRequester ?: remember { FocusRequester() }
    val voiceSearchLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode != Activity.RESULT_OK) return@rememberLauncherForActivityResult
        val spokenText = result.data
            ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            ?.firstOrNull()
            ?.trim()
            .orEmpty()
        if (spokenText.isNotBlank()) {
            onQueryChange(spokenText)
            onSearch(spokenText)
        }
    }
    val shape = RoundedCornerShape(22.dp)
    val backgroundLuminance = MaterialTheme.colorScheme.background.luminance()
    val isDarkTheme = backgroundLuminance < 0.45f
    val isOled = backgroundLuminance < 0.05f
    val searchTextStyle = MaterialTheme.typography.bodyMedium.copy(lineHeight = 20.sp)
    val borderColor = if (liquidGlassEnabled) {
        MaterialTheme.colorScheme.outlineVariant.copy(alpha = if (isDarkTheme) 0.28f else 0.34f)
    } else {
        MaterialTheme.colorScheme.primary.copy(alpha = if (isDarkTheme) 0.18f else 0.12f)
    }
    val containerColor = if (liquidGlassEnabled) {
        MaterialTheme.colorScheme.surface.copy(
            alpha = if (isOled) 0.72f else if (isDarkTheme) 0.76f else 0.88f
        )
    } else {
        MaterialTheme.colorScheme.primaryContainer.copy(
            alpha = if (isDarkTheme) 0.28f else 0.54f
        )
    }

    LaunchedEffect(autoFocus) {
        if (autoFocus) {
            activeFocusRequester.requestFocus()
            keyboardController?.show()
        }
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp),
        shape = shape,
        color = containerColor,
        border = BorderStroke(1.dp, borderColor),
        shadowElevation = UiTokens.SectionShadowElevation
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight()
                .padding(start = 18.dp, end = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = androidx.compose.material.icons.Icons.Rounded.Search,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(21.dp)
            )

            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                modifier = Modifier
                    .weight(1f)
                    .focusRequester(activeFocusRequester),
                singleLine = true,
                textStyle = searchTextStyle.copy(color = MaterialTheme.colorScheme.onSurface),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(
                    onSearch = {
                        onSearch(query.trim())
                        keyboardController?.hide()
                    }
                ),
                decorationBox = { innerTextField ->
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        if (query.isBlank()) {
                            Text(
                                text = placeholder,
                                style = searchTextStyle,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        innerTextField()
                    }
                }
            )

            if (query.isNotBlank()) {
                AppChromeIconButton(
                    icon = androidx.compose.material.icons.Icons.Rounded.Close,
                    contentDescription = "Clear",
                    onClick = { onQueryChange("") },
                    liquidGlassEnabled = liquidGlassEnabled,
                    size = 28.dp,
                    iconSize = 14.dp
                )
            } else {
                Surface(
                    onClick = {
                            val voiceIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                                putExtra(
                                    RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                                    RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
                                )
                                putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                                putExtra(RecognizerIntent.EXTRA_PROMPT, placeholder)
                            }
                            val hasVoiceSupport = voiceIntent.resolveActivity(context.packageManager) != null
                            if (hasVoiceSupport) {
                                voiceSearchLauncher.launch(voiceIntent)
                            } else {
                                Toast.makeText(context, "Voice search is unavailable", Toast.LENGTH_SHORT).show()
                            }
                    },
                    modifier = Modifier
                        .size(32.dp)
                        .semantics {
                            contentDescription = "Voice search"
                            role = Role.Button
                        },
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.58f)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = SearchVoiceMicIcon,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

private val SearchVoiceMicIcon: ImageVector by lazy {
    ImageVector.Builder(
        name = "SearchVoiceMic",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        path(
            fill = SolidColor(Color.Black),
            pathFillType = PathFillType.NonZero
        ) {
            moveTo(12f, 15f)
            curveTo(9.79f, 15f, 8f, 13.21f, 8f, 11f)
            lineTo(8f, 6f)
            curveTo(8f, 3.79f, 9.79f, 2f, 12f, 2f)
            curveTo(14.21f, 2f, 16f, 3.79f, 16f, 6f)
            lineTo(16f, 11f)
            curveTo(16f, 13.21f, 14.21f, 15f, 12f, 15f)
            close()

            moveTo(10f, 6f)
            lineTo(10f, 11f)
            curveTo(10f, 12.1f, 10.9f, 13f, 12f, 13f)
            curveTo(13.1f, 13f, 14f, 12.1f, 14f, 11f)
            lineTo(14f, 6f)
            curveTo(14f, 4.9f, 13.1f, 4f, 12f, 4f)
            curveTo(10.9f, 4f, 10f, 4.9f, 10f, 6f)
            close()

            moveTo(18f, 11f)
            curveTo(18f, 14.31f, 15.31f, 17f, 12f, 17f)
            curveTo(8.69f, 17f, 6f, 14.31f, 6f, 11f)
            lineTo(4f, 11f)
            curveTo(4f, 15.08f, 7.05f, 18.44f, 11f, 18.92f)
            lineTo(11f, 22f)
            lineTo(13f, 22f)
            lineTo(13f, 18.92f)
            curveTo(16.95f, 18.44f, 20f, 15.08f, 20f, 11f)
            lineTo(18f, 11f)
            close()
        }
    }.build()
}
