package com.dyu.ereader

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.fragment.app.FragmentActivity
import com.dyu.ereader.data.model.AppTheme
import com.dyu.ereader.ui.AppNavHost
import com.dyu.ereader.ui.MainViewModel
import com.dyu.ereader.ui.theme.EReaderTheme
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
            val appTheme by mainViewModel.theme.collectAsState()
            val liquidGlassEnabled by mainViewModel.liquidGlassEnabled.collectAsState()
            val hideStatusBar by mainViewModel.hideStatusBar.collectAsState()
            
            val isSystemDark = isSystemInDarkTheme()
            val isDark = remember(appTheme, isSystemDark) {
                when (appTheme) {
                    AppTheme.DARK -> true
                    AppTheme.BLACK -> true
                    AppTheme.LIGHT -> false
                    AppTheme.SYSTEM -> isSystemDark
                }
            }

            val windowInsetsController = remember(window) { WindowCompat.getInsetsController(window, window.decorView) }
            LaunchedEffect(hideStatusBar) {
                if (hideStatusBar) {
                    windowInsetsController.hide(WindowInsetsCompat.Type.statusBars())
                    windowInsetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                } else {
                    windowInsetsController.show(WindowInsetsCompat.Type.statusBars())
                }
            }

            EReaderTheme(
                appTheme = appTheme
            ) {
                AppNavHost(
                    mainViewModel = mainViewModel,
                    isDarkTheme = isDark
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        mainViewModel.handleIntent(intent)
    }
}
