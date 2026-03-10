package com.dyu.ereader.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import com.dyu.ereader.ui.home.HOME_ROUTE
import com.dyu.ereader.ui.home.homeScreen
import com.dyu.ereader.ui.reader.navigateToReader
import com.dyu.ereader.ui.reader.readerScreen

@Composable
fun AppNavHost(
    mainViewModel: MainViewModel,
    isDarkTheme: Boolean
) {
    val navController = rememberNavController()
    val pendingBook by mainViewModel.pendingBook.collectAsState()

    // Handle intent-based navigation (e.g., opening a file from outside the app)
    LaunchedEffect(pendingBook) {
        pendingBook?.let { (uri, type) ->
            navController.navigateToReader(uri, type.name)
            mainViewModel.consumePendingBook()
        }
    }

    NavHost(
        navController = navController,
        startDestination = HOME_ROUTE
    ) {
        // Extracted Home Screen Logic (com.dyu.ereader.ui.home.HomeNavigation.kt)
        homeScreen(
            mainViewModel = mainViewModel,
            onNavigateToReader = { uri, typeName ->
                navController.navigateToReader(uri, typeName)
            }
        )

        // Extracted Reader Screen Logic (com.dyu.ereader.ui.reader.ReaderNavigation.kt)
        readerScreen(
            isDarkTheme = isDarkTheme,
            onBack = { navController.popBackStack() },
            onNavigateHome = {
                navController.popBackStack(HOME_ROUTE, inclusive = false)
            }
        )
    }
}
