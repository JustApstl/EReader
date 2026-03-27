package com.dyu.ereader.ui.app

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import com.dyu.ereader.data.repository.update.AppUpdateInstallerRepository
import com.dyu.ereader.ui.home.navigation.HOME_ROUTE
import com.dyu.ereader.ui.home.navigation.homeScreen
import com.dyu.ereader.ui.reader.navigation.navigateToReader
import com.dyu.ereader.ui.reader.navigation.readerScreen
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@EntryPoint
@InstallIn(SingletonComponent::class)
interface AppUpdateInstallerEntryPoint {
    fun appUpdateInstallerRepository(): AppUpdateInstallerRepository
}

@Composable
fun AppNavHost(
    mainViewModel: MainViewModel,
    isDarkTheme: Boolean
) {
    val navController = rememberNavController()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val pendingBook by mainViewModel.pendingBook.collectAsState()
    val appUpdateState by mainViewModel.appUpdateState.collectAsState()
    val pendingUpdateInstall by mainViewModel.pendingUpdateInstall.collectAsState()
    val pendingUnknownAppsPermissionInstall by mainViewModel.pendingUnknownAppsPermissionInstall.collectAsState()
    val installerRepository = EntryPointAccessors
        .fromApplication(context, AppUpdateInstallerEntryPoint::class.java)
        .appUpdateInstallerRepository()
    val unknownAppsPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        mainViewModel.resumePendingUpdateInstall()
    }
    val installPackageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        mainViewModel.consumePendingUpdateInstall()
    }

    DisposableEffect(lifecycleOwner, pendingUnknownAppsPermissionInstall?.uriString) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME && pendingUnknownAppsPermissionInstall != null) {
                mainViewModel.resumePendingUpdateInstall()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Handle intent-based navigation (e.g., opening a file from outside the app)
    LaunchedEffect(pendingBook) {
        pendingBook?.let { (uri, type) ->
            navController.navigateToReader(uri, type.name)
            mainViewModel.consumePendingBook()
        }
    }

    LaunchedEffect(pendingUnknownAppsPermissionInstall?.uriString) {
        pendingUnknownAppsPermissionInstall ?: return@LaunchedEffect
        unknownAppsPermissionLauncher.launch(installerRepository.createUnknownAppsSettingsIntent())
    }

    LaunchedEffect(pendingUpdateInstall?.uriString) {
        val install = pendingUpdateInstall ?: return@LaunchedEffect
        installPackageLauncher.launch(installerRepository.createInstallIntent(install))
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

    AppUpdateDialogs(
        updateState = appUpdateState,
        onDismissUpdate = mainViewModel::dismissUpdatePrompt,
        onInstallLatestUpdate = mainViewModel::installLatestUpdate,
        onDismissChangelog = mainViewModel::dismissChangelogPrompt
    )
}
