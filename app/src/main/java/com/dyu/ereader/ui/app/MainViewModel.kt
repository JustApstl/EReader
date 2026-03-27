package com.dyu.ereader.ui.app

import android.content.Intent
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dyu.ereader.BuildConfig
import com.dyu.ereader.data.format.BookFormatRegistry
import com.dyu.ereader.data.local.prefs.ReaderPreferencesStore
import com.dyu.ereader.data.model.app.AppAccent
import com.dyu.ereader.data.model.app.AppFont
import com.dyu.ereader.data.model.app.AppTheme
import com.dyu.ereader.data.model.app.NavigationBarStyle
import com.dyu.ereader.data.model.library.BookType
import com.dyu.ereader.data.model.update.PendingAppInstall
import com.dyu.ereader.data.model.update.AppUpdateUiState
import com.dyu.ereader.data.repository.update.AppUpdateInstallerRepository
import com.dyu.ereader.data.repository.update.AppUpdateNotificationRepository
import com.dyu.ereader.data.repository.update.AppUpdateRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val preferencesStore: ReaderPreferencesStore,
    private val appUpdateRepository: AppUpdateRepository,
    private val appUpdateInstallerRepository: AppUpdateInstallerRepository,
    private val appUpdateNotificationRepository: AppUpdateNotificationRepository
) : ViewModel() {

    private val startupPrefs = runBlocking {
        preferencesStore.readAppStartupPreferences()
    }

    private val _appearance = MutableStateFlow(startupPrefs.toAppAppearanceState())
    val appearance: StateFlow<AppAppearanceState> = _appearance.asStateFlow()

    private val _pendingBook = MutableStateFlow<Pair<String, BookType>?>(null)
    val pendingBook: StateFlow<Pair<String, BookType>?> = _pendingBook.asStateFlow()
    private val _pendingCloudAuthUri = MutableStateFlow<Uri?>(null)
    val pendingCloudAuthUri: StateFlow<Uri?> = _pendingCloudAuthUri.asStateFlow()
    private val _notificationPermissionPrompted = MutableStateFlow(false)
    val notificationPermissionPrompted: StateFlow<Boolean> = _notificationPermissionPrompted.asStateFlow()
    private val _appUpdateState = MutableStateFlow(AppUpdateUiState())
    val appUpdateState: StateFlow<AppUpdateUiState> = _appUpdateState.asStateFlow()
    private val _pendingUpdateInstall = MutableStateFlow<PendingAppInstall?>(null)
    val pendingUpdateInstall: StateFlow<PendingAppInstall?> = _pendingUpdateInstall.asStateFlow()
    private val _pendingUnknownAppsPermissionInstall = MutableStateFlow<PendingAppInstall?>(null)
    val pendingUnknownAppsPermissionInstall: StateFlow<PendingAppInstall?> = _pendingUnknownAppsPermissionInstall.asStateFlow()

    init {
        observeAppearancePreference(preferencesStore.appThemeFlow) { theme ->
            copy(theme = theme)
        }
        observeAppearancePreference(preferencesStore.appFontFlow) { appFont ->
            copy(appFont = appFont)
        }
        observeAppearancePreference(preferencesStore.appAccentFlow) { accent ->
            copy(accent = accent)
        }
        observeAppearancePreference(preferencesStore.appAccentCustomColorFlow) { customAccentColor ->
            copy(customAccentColor = customAccentColor)
        }
        observeAppearancePreference(preferencesStore.liquidGlassEnabledFlow) { liquidGlassEnabled ->
            copy(liquidGlassEnabled = liquidGlassEnabled)
        }
        observeAppearancePreference(preferencesStore.navBarStyleFlow) { navBarStyle ->
            copy(navBarStyle = navBarStyle)
        }
        observeAppearancePreference(preferencesStore.hideStatusBarFlow) { hideStatusBar ->
            copy(hideStatusBar = hideStatusBar)
        }
        observeAppearancePreference(preferencesStore.appTextScaleFlow) { appTextScale ->
            copy(appTextScale = appTextScale)
        }
        observeAppearancePreference(preferencesStore.hideBetaFeaturesFlow) { hideBetaFeatures ->
            copy(hideBetaFeatures = hideBetaFeatures)
        }
        viewModelScope.launch {
            preferencesStore.notificationPermissionPromptedFlow.collectLatest { prompted ->
                _notificationPermissionPrompted.value = prompted
            }
        }
        viewModelScope.launch {
            prepareInstalledVersionChangelog()
            checkForUpdates(force = false, showPromptOnAvailable = true)
        }
    }

    private fun <T> observeAppearancePreference(
        flow: Flow<T>,
        updateAppearance: AppAppearanceState.(T) -> AppAppearanceState
    ) {
        viewModelScope.launch {
            flow.collectLatest { value ->
                _appearance.update { current ->
                    current.updateAppearance(value)
                }
            }
        }
    }

    fun handleIntent(intent: Intent?) {
        if (intent?.action == Intent.ACTION_VIEW) {
            val uri = intent.data ?: return
            if (uri.scheme == DROPBOX_REDIRECT_SCHEME && uri.host == DROPBOX_REDIRECT_HOST) {
                _pendingCloudAuthUri.value = uri
                return
            }
            val type = BookFormatRegistry.resolveTypeFromPath(uri.toString(), BookType.EPUB)
            _pendingBook.value = uri.toString() to type
        }
    }

    fun consumePendingBook() {
        _pendingBook.value = null
    }

    fun consumePendingCloudAuthUri() {
        _pendingCloudAuthUri.value = null
    }

    fun setTheme(theme: AppTheme) {
        viewModelScope.launch {
            preferencesStore.setAppTheme(theme)
        }
    }

    fun setAppFont(font: AppFont) {
        viewModelScope.launch {
            preferencesStore.setAppFont(font)
        }
    }

    fun setAccent(accent: AppAccent) {
        viewModelScope.launch {
            preferencesStore.setAppAccent(accent)
        }
    }

    fun setCustomAccentColor(color: Int?) {
        viewModelScope.launch {
            preferencesStore.setAppAccentCustomColor(color)
        }
    }

    fun setLiquidGlassEnabled(enabled: Boolean) {
        viewModelScope.launch {
            preferencesStore.setLiquidGlassEnabled(enabled)
        }
    }

    fun setNavigationBarStyle(style: NavigationBarStyle) {
        viewModelScope.launch {
            preferencesStore.setNavigationBarStyle(style)
        }
    }

    fun markNotificationPermissionPrompted() {
        viewModelScope.launch {
            preferencesStore.setNotificationPermissionPrompted(true)
        }
    }

    fun checkForUpdates(
        force: Boolean = true,
        showPromptOnAvailable: Boolean = false
    ) {
        viewModelScope.launch {
            val current = _appUpdateState.value
            _appUpdateState.value = current.copy(isChecking = true, errorMessage = null)

            appUpdateRepository.checkForUpdates(
                currentVersionName = CURRENT_VERSION_NAME,
                force = force
            ).fold(
                onSuccess = { snapshot ->
                    val latestRelease = snapshot.latestRelease
                    val updateAvailable = snapshot.updateAvailable
                    val dismissedVersion = snapshot.dismissedVersion
                    maybeNotifyAboutUpdate(latestRelease, updateAvailable)
                    _appUpdateState.value = _appUpdateState.value.copy(
                        isChecking = false,
                        isPreparingInstall = false,
                        latestRelease = latestRelease,
                        updateAvailable = updateAvailable,
                        lastCheckedAt = snapshot.lastCheckedAt,
                        errorMessage = null,
                        showLatestReleaseDetails = false,
                        showUpdatePrompt = showPromptOnAvailable &&
                            updateAvailable &&
                            latestRelease != null &&
                            latestRelease.versionName != dismissedVersion
                    )
                },
                onFailure = { error ->
                    val rawMessage = error.message.orEmpty()
                    val noReleaseData = rawMessage.contains("No release data returned from GitHub", ignoreCase = true)
                    _appUpdateState.value = _appUpdateState.value.copy(
                        isChecking = false,
                        isPreparingInstall = false,
                        updateAvailable = if (noReleaseData) false else _appUpdateState.value.updateAvailable,
                        lastCheckedAt = if (noReleaseData) System.currentTimeMillis() else _appUpdateState.value.lastCheckedAt,
                        errorMessage = if (noReleaseData) {
                            null
                        } else if (force) {
                            error.message ?: "Unable to check for updates right now."
                        } else {
                            null
                        }
                    )
                }
            )
        }
    }

    fun dismissUpdatePrompt() {
        viewModelScope.launch {
            _appUpdateState.value.latestRelease?.versionName?.let { versionName ->
                appUpdateRepository.dismissRelease(versionName)
            }
            _appUpdateState.update { current ->
                current.copy(showUpdatePrompt = false)
            }
        }
    }

    fun installLatestUpdate() {
        val release = _appUpdateState.value.latestRelease ?: return
        viewModelScope.launch {
            _appUpdateState.update { current ->
                current.copy(
                    isPreparingInstall = true,
                    errorMessage = null
                )
            }
            appUpdateInstallerRepository.prepareUpdateInstall(release).fold(
                onSuccess = { install ->
                    _appUpdateState.update { current ->
                        current.copy(isPreparingInstall = false)
                    }
                    if (appUpdateInstallerRepository.canInstallPackages()) {
                        _pendingUpdateInstall.value = install
                    } else {
                        _pendingUnknownAppsPermissionInstall.value = install
                    }
                },
                onFailure = { error ->
                    _appUpdateState.update { current ->
                        current.copy(
                            isPreparingInstall = false,
                            errorMessage = error.message ?: "Unable to prepare the update installer."
                        )
                    }
                }
            )
        }
    }

    fun resumePendingUpdateInstall() {
        val install = _pendingUnknownAppsPermissionInstall.value ?: return
        if (appUpdateInstallerRepository.canInstallPackages()) {
            _pendingUnknownAppsPermissionInstall.value = null
            _pendingUpdateInstall.value = install
        } else {
            _pendingUnknownAppsPermissionInstall.value = null
            _appUpdateState.update { current ->
                current.copy(errorMessage = "Allow Install unknown apps for EReader to continue the update.")
            }
        }
    }

    fun consumePendingUpdateInstall() {
        _pendingUpdateInstall.value = null
    }

    fun consumePendingUnknownAppsPermissionInstall() {
        _pendingUnknownAppsPermissionInstall.value = null
    }

    fun toggleLatestReleaseDetails() {
        _appUpdateState.update { current ->
            if (current.latestRelease == null) {
                current
            } else {
                current.copy(showLatestReleaseDetails = !current.showLatestReleaseDetails)
            }
        }
    }

    fun toggleReleaseHistory() {
        viewModelScope.launch {
            if (_appUpdateState.value.showReleaseHistory) {
                _appUpdateState.update { current ->
                    current.copy(showReleaseHistory = false)
                }
            } else if (_appUpdateState.value.releaseHistory.isEmpty()) {
                _appUpdateState.update { current ->
                    current.copy(isLoadingReleaseHistory = true, errorMessage = null)
                }
                appUpdateRepository.fetchReleaseHistory().fold(
                    onSuccess = { releases ->
                        _appUpdateState.update { current ->
                            current.copy(
                                releaseHistory = releases,
                                isLoadingReleaseHistory = false,
                                showReleaseHistory = true
                            )
                        }
                    },
                    onFailure = { error ->
                        _appUpdateState.update { current ->
                            current.copy(
                                isLoadingReleaseHistory = false,
                                errorMessage = error.message ?: "Unable to load release history."
                            )
                        }
                    }
                )
            } else {
                _appUpdateState.update { current ->
                    current.copy(showReleaseHistory = true)
                }
            }
        }
    }

    fun dismissChangelogPrompt() {
        viewModelScope.launch {
            _appUpdateState.value.changelogRelease?.versionName?.let { versionName ->
                appUpdateRepository.markChangelogShown(versionName)
            }
            _appUpdateState.update { current ->
                current.copy(showChangelogPrompt = false)
            }
        }
    }

    private suspend fun prepareInstalledVersionChangelog() {
        val changelog = appUpdateRepository.prepareChangelogForInstalledVersion(CURRENT_VERSION_NAME)
        if (changelog != null) {
            _appUpdateState.update { current ->
                current.copy(
                    latestRelease = if (!current.updateAvailable) changelog else current.latestRelease,
                    changelogRelease = changelog,
                    showChangelogPrompt = true
                )
            }
        }
    }

    private suspend fun maybeNotifyAboutUpdate(
        latestRelease: com.dyu.ereader.data.model.update.AppReleaseInfo?,
        updateAvailable: Boolean
    ) {
        if (!updateAvailable || latestRelease == null) {
            return
        }
        val lastNotified = appUpdateRepository.getLastNotifiedVersion()
        if (lastNotified == latestRelease.versionName) {
            return
        }
        appUpdateNotificationRepository.notifyUpdateAvailable(latestRelease)
        appUpdateRepository.markUpdateNotified(latestRelease.versionName)
    }

    companion object {
        private const val DROPBOX_REDIRECT_SCHEME = "ereader"
        private const val DROPBOX_REDIRECT_HOST = "dropbox-auth"
        private val CURRENT_VERSION_NAME = BuildConfig.VERSION_NAME
    }
}
