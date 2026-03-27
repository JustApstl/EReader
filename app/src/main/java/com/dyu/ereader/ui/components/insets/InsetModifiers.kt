package com.dyu.ereader.ui.components.insets

import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBarsIgnoringVisibility
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun Modifier.stableStatusBarsPadding(): Modifier =
    this.windowInsetsPadding(WindowInsets.statusBarsIgnoringVisibility)
