package com.dyu.ereader.ui.reader.controls.dialogs

import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import androidx.compose.foundation.interaction.MutableInteractionSource
import com.dyu.ereader.ui.components.buttons.AppChromeIconButton
import androidx.compose.material3.MaterialTheme

@Composable
fun ImageZoomDialog(
    url: String,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        val configuration = LocalConfiguration.current
        val density = LocalDensity.current
        val screenWidth = with(density) { configuration.screenWidthDp.dp.toPx() }
        val screenHeight = with(density) { configuration.screenHeightDp.dp.toPx() }
        val inlineBitmap = remember(url) {
            if (url.startsWith("data:")) {
                val comma = url.indexOf(',')
                if (comma != -1 && comma + 1 < url.length) {
                    val base64Data = url.substring(comma + 1)
                    runCatching {
                        val bytes = Base64.decode(base64Data, Base64.DEFAULT)
                        BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
                    }.getOrNull()
                } else {
                    null
                }
            } else {
                null
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.92f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss
                ),
            contentAlignment = Alignment.Center
        ) {
            var scale by remember { mutableFloatStateOf(1f) }
            var offset by remember { mutableStateOf(Offset.Zero) }
            val state = rememberTransformableState { zoomChange: Float, offsetChange: Offset, _ ->
                scale = (scale * zoomChange).coerceIn(1f, 5f)

                val extraWidth = (scale - 1) * screenWidth
                val extraHeight = (scale - 1) * screenHeight

                val maxX = extraWidth / 2
                val maxY = extraHeight / 2

                offset = if (scale > 1f) {
                    Offset(
                        x = (offset.x + offsetChange.x).coerceIn(-maxX, maxX),
                        y = (offset.y + offsetChange.y).coerceIn(-maxY, maxY)
                    )
                } else {
                    Offset.Zero
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer(
                        scaleX = scale,
                        scaleY = scale,
                        translationX = offset.x,
                        translationY = offset.y
                    )
                    .transformable(state = state)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onDismiss
                    )
            ) {
                if (inlineBitmap != null) {
                    Image(
                        bitmap = inlineBitmap,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    AsyncImage(
                        model = url,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            AppChromeIconButton(
                icon = Icons.Rounded.Close,
                contentDescription = "Close",
                onClick = onDismiss,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp),
                size = 36.dp,
                iconSize = 18.dp
            )
        }
    }
}
