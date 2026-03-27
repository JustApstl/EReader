package com.dyu.ereader.ui.reader.screens

import android.app.Activity
import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

internal data class ReaderPickerLaunchers(
    val launchImagePicker: () -> Unit,
    val launchFontPicker: () -> Unit
)

@Composable
internal fun rememberReaderPickerLaunchers(
    onBackgroundImageSelected: (String?) -> Unit,
    onCustomFontSelected: (String?) -> Unit
): ReaderPickerLaunchers {
    val context = LocalContext.current
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            context.contentResolver.takePersistableUriPermission(it, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            onBackgroundImageSelected(it.toString())
        }
    }

    val fontPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            context.contentResolver.takePersistableUriPermission(it, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            onCustomFontSelected(it.toString())
        }
    }

    return ReaderPickerLaunchers(
        launchImagePicker = { imagePickerLauncher.launch(arrayOf("image/*")) },
        launchFontPicker = { fontPickerLauncher.launch(arrayOf("font/*", "application/octet-stream", "*/*")) }
    )
}

@Composable
internal fun ReaderScreenWindowEffects(
    statusBarColor: Color,
    hideStatusBar: Boolean
) {
    val context = LocalContext.current
    val view = LocalView.current
    val isLightStatusBar = statusBarColor.luminance() > 0.5f

    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            val controller = WindowCompat.getInsetsController(window, view)
            @Suppress("DEPRECATION")
            window.statusBarColor = statusBarColor.toArgb()
            controller.isAppearanceLightStatusBars = isLightStatusBar
        }
    }

    DisposableEffect(view) {
        val previousKeepScreenOn = view.keepScreenOn
        view.keepScreenOn = true
        onDispose {
            view.keepScreenOn = previousKeepScreenOn
        }
    }

    LaunchedEffect(hideStatusBar) {
        val window = (context as? ComponentActivity)?.window ?: return@LaunchedEffect
        val insetsController = WindowCompat.getInsetsController(window, window.decorView)
        insetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE

        if (hideStatusBar) {
            insetsController.hide(WindowInsetsCompat.Type.statusBars())
        } else {
            insetsController.show(WindowInsetsCompat.Type.statusBars())
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            val window = (context as? ComponentActivity)?.window ?: return@onDispose
            val insetsController = WindowCompat.getInsetsController(window, window.decorView)
            insetsController.show(WindowInsetsCompat.Type.statusBars())
        }
    }
}
