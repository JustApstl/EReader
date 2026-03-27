package com.dyu.ereader.ui.browse.screens

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.dyu.ereader.ui.home.viewmodel.HomeViewModel

@Composable
fun BrowseCatalogScreen(
    modifier: Modifier = Modifier,
    liquidGlassEnabled: Boolean = false,
    viewModel: HomeViewModel = hiltViewModel()
) {
    BrowseCatalogRouteContent(
        modifier = modifier,
        liquidGlassEnabled = liquidGlassEnabled,
        viewModel = viewModel
    )
}
