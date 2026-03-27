package com.dyu.ereader.ui.app

import com.dyu.ereader.data.local.prefs.AppStartupPreferences
import com.dyu.ereader.data.model.app.AppAccent
import com.dyu.ereader.data.model.app.AppFont
import com.dyu.ereader.data.model.app.AppTheme
import com.dyu.ereader.data.model.app.NavigationBarStyle

data class AppAppearanceState(
    val theme: AppTheme = AppTheme.SYSTEM,
    val appFont: AppFont = AppFont.SYSTEM,
    val accent: AppAccent = AppAccent.SYSTEM,
    val customAccentColor: Int? = null,
    val liquidGlassEnabled: Boolean = false,
    val navBarStyle: NavigationBarStyle = NavigationBarStyle.DEFAULT,
    val hideStatusBar: Boolean = false,
    val appTextScale: Float = 1f,
    val hideBetaFeatures: Boolean = false
)

internal fun AppStartupPreferences.toAppAppearanceState(): AppAppearanceState {
    return AppAppearanceState(
        theme = theme,
        appFont = appFont,
        accent = accent,
        customAccentColor = customAccentColor,
        liquidGlassEnabled = liquidGlassEnabled,
        navBarStyle = navBarStyle,
        hideStatusBar = hideStatusBar,
        appTextScale = appTextScale,
        hideBetaFeatures = hideBetaFeatures
    )
}
