package com.dyu.ereader.ui.app

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.CompositionLocalProvider
import androidx.core.content.ContextCompat
import android.content.pm.PackageManager
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.fragment.app.FragmentActivity
import com.dyu.ereader.ui.app.AppNavHost
import com.dyu.ereader.ui.app.MainViewModel
import com.dyu.ereader.ui.app.theme.EReaderTheme
import com.dyu.ereader.ui.app.theme.resolveDarkTheme
import com.dyu.ereader.ui.components.badges.LocalBetaFeaturesEnabled
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : FragmentActivity() {
    private val mainViewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            window.attributes.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }
        
        mainViewModel.handleIntent(intent)
        
        setContent {
            val appearance by mainViewModel.appearance.collectAsState()
            val notificationPermissionPrompted by mainViewModel.notificationPermissionPrompted.collectAsState()
            
            val isSystemDark = isSystemInDarkTheme()
            val isDark = remember(appearance.theme, isSystemDark) {
                appearance.theme.resolveDarkTheme(isSystemDark)
            }
            val notificationPermissionLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.RequestPermission()
            ) { _ ->
                // The app only needs to ask once on fresh install.
            }

            val windowInsetsController = remember(window) { WindowCompat.getInsetsController(window, window.decorView) }
            LaunchedEffect(appearance.hideStatusBar) {
                if (appearance.hideStatusBar) {
                    windowInsetsController.hide(WindowInsetsCompat.Type.statusBars())
                    windowInsetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                } else {
                    windowInsetsController.show(WindowInsetsCompat.Type.statusBars())
                }
            }

            LaunchedEffect(notificationPermissionPrompted) {
                val shouldPrompt =
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                        !notificationPermissionPrompted &&
                        ContextCompat.checkSelfPermission(
                            this@MainActivity,
                            Manifest.permission.POST_NOTIFICATIONS
                        ) != PackageManager.PERMISSION_GRANTED

                if (shouldPrompt) {
                    mainViewModel.markNotificationPermissionPrompted()
                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }

            EReaderTheme(
                appTheme = appearance.theme,
                appFont = appearance.appFont,
                appAccent = appearance.accent,
                customAccentColor = appearance.customAccentColor,
                appTextScale = appearance.appTextScale
            ) {
                CompositionLocalProvider(LocalBetaFeaturesEnabled provides !appearance.hideBetaFeatures) {
                    AppNavHost(
                        mainViewModel = mainViewModel,
                        isDarkTheme = isDark
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        mainViewModel.handleIntent(intent)
    }
}
