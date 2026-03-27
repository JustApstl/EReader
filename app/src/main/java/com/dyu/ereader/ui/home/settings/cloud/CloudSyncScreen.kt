@file:Suppress("DEPRECATION")

package com.dyu.ereader.ui.home.settings.cloud

import android.content.Intent
import android.content.Context
import android.net.Uri
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.text.format.Formatter
import androidx.compose.foundation.clickable
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Cloud
import androidx.compose.material.icons.rounded.CloudSync
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.LinkOff
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Restore
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material.icons.rounded.Dns
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import coil.compose.AsyncImage
import com.dyu.ereader.data.model.cloud.CloudBackupScope
import com.dyu.ereader.data.model.cloud.CloudLinkedAccount
import com.dyu.ereader.data.model.cloud.CloudProvider
import com.dyu.ereader.data.repository.cloud.dropbox.DropboxApi
import com.dyu.ereader.data.repository.cloud.google.GoogleDriveApi
import com.dyu.ereader.data.repository.cloud.webdav.WebDavApi
import com.dyu.ereader.ui.home.viewmodel.HomeViewModel
import com.google.android.gms.auth.api.signin.GoogleSignInStatusCodes
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.CommonStatusCodes
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

@Composable
fun CloudSyncScreen(
    modifier: Modifier = Modifier,
    liquidGlassEnabled: Boolean = false,
    pendingCloudAuthUri: Uri? = null,
    onConsumePendingCloudAuthUri: () -> Unit = {},
    viewModel: HomeViewModel = hiltViewModel()
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        CloudSyncContent(
            liquidGlassEnabled = liquidGlassEnabled,
            pendingCloudAuthUri = pendingCloudAuthUri,
            onConsumePendingCloudAuthUri = onConsumePendingCloudAuthUri,
            viewModel = viewModel
        )
        Spacer(Modifier.height(96.dp))
    }
}

@Composable
fun CloudSyncContent(
    modifier: Modifier = Modifier,
    liquidGlassEnabled: Boolean = false,
    pendingCloudAuthUri: Uri? = null,
    onConsumePendingCloudAuthUri: () -> Unit = {},
    viewModel: HomeViewModel = hiltViewModel()
) {
    val linkedAccounts by viewModel.linkedCloudAccounts.collectAsState(initial = emptyList())
    val storageSummary by viewModel.cloudStorageSummary.collectAsState()
    val syncStatus by viewModel.cloudSyncStatus.collectAsState(initial = "Ready")
    val lastSyncTime by viewModel.lastSyncTime.collectAsState(initial = null)
    val isSyncing by viewModel.isSyncing.collectAsState(initial = false)
    val cloudMessage by viewModel.cloudMessage.collectAsState(initial = null)
    val context = LocalContext.current
    val dropboxApi = remember(context) { DropboxApi(context) }
    val webDavApi = remember { WebDavApi() }
    val coroutineScope = rememberCoroutineScope()
    val driveScope = remember { com.google.android.gms.common.api.Scope(GoogleDriveApi.DRIVE_APPDATA_SCOPE) }
    val googleSignInClient = remember(context, driveScope) {
        com.google.android.gms.auth.api.signin.GoogleSignIn.getClient(
            context,
            com.google.android.gms.auth.api.signin.GoogleSignInOptions.Builder(
                com.google.android.gms.auth.api.signin.GoogleSignInOptions.DEFAULT_SIGN_IN
            )
                .requestEmail()
                .requestScopes(driveScope)
                .build()
        )
    }
    val isOnline = rememberNetworkAvailability()
    var authMessage by remember { mutableStateOf<String?>(null) }
    var webDavServerUrl by rememberSaveable { mutableStateOf("") }
    var webDavUsername by rememberSaveable { mutableStateOf("") }
    var webDavPassword by rememberSaveable { mutableStateOf("") }
    var showWebDavSetup by rememberSaveable { mutableStateOf(false) }
    var selectedProvider by rememberSaveable { mutableStateOf(CloudProvider.WEB_DAV) }

    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result: ActivityResult ->
        val task = com.google.android.gms.auth.api.signin.GoogleSignIn.getSignedInAccountFromIntent(result.data)
        runCatching { task.getResult(ApiException::class.java) }
            .onSuccess { account ->
                val token = account.email ?: account.account?.name ?: account.id
                if (token == null) {
                    authMessage = "Google account did not return a usable identity."
                    return@onSuccess
                }
                authMessage = null
                viewModel.onCloudAuthComplete(
                    provider = CloudProvider.GOOGLE_DRIVE,
                    accessToken = token,
                    displayName = account.displayName,
                    email = account.email,
                    photoUrl = account.photoUrl?.toString()
                )
            }
            .onFailure { error ->
                authMessage = resolveGoogleSignInMessage(error)
            }
    }

    LaunchedEffect(linkedAccounts, context, driveScope) {
        val lastSignedInAccount = com.google.android.gms.auth.api.signin.GoogleSignIn.getLastSignedInAccount(context)
        val hasDrivePermission = lastSignedInAccount != null &&
            com.google.android.gms.auth.api.signin.GoogleSignIn.hasPermissions(lastSignedInAccount, driveScope)
        val alreadyLinked = linkedAccounts.any { account ->
            account.email != null && account.email.equals(lastSignedInAccount?.email, ignoreCase = true)
        }
        if (lastSignedInAccount != null && hasDrivePermission && !alreadyLinked) {
            val token = lastSignedInAccount.email ?: lastSignedInAccount.account?.name ?: lastSignedInAccount.id
            if (token != null) {
                viewModel.onCloudAuthComplete(
                    provider = CloudProvider.GOOGLE_DRIVE,
                    accessToken = token,
                    displayName = lastSignedInAccount.displayName,
                    email = lastSignedInAccount.email,
                    photoUrl = lastSignedInAccount.photoUrl?.toString()
                )
            }
        }
    }

    fun connectGoogleDrive() {
        val lastSignedInAccount = com.google.android.gms.auth.api.signin.GoogleSignIn.getLastSignedInAccount(context)
        val hasDrivePermission = lastSignedInAccount != null &&
            com.google.android.gms.auth.api.signin.GoogleSignIn.hasPermissions(lastSignedInAccount, driveScope)
        if (lastSignedInAccount != null && hasDrivePermission) {
            val token = lastSignedInAccount.email ?: lastSignedInAccount.account?.name ?: lastSignedInAccount.id
            if (token != null) {
                authMessage = null
                viewModel.onCloudAuthComplete(
                    provider = CloudProvider.GOOGLE_DRIVE,
                    accessToken = token,
                    displayName = lastSignedInAccount.displayName,
                    email = lastSignedInAccount.email,
                    photoUrl = lastSignedInAccount.photoUrl?.toString()
                )
            } else {
                authMessage = "Google account is missing an email or user ID."
            }
        } else {
            authMessage = null
            googleSignInLauncher.launch(googleSignInClient.signInIntent)
        }
    }

    fun connectDropbox() {
        authMessage = null
        val authUrl = runCatching { dropboxApi.createAuthorizationUrl() }
            .getOrElse { error ->
                authMessage = error.localizedMessage ?: "Dropbox sign-in could not start."
                return
            }
        runCatching {
            context.startActivity(Intent(Intent.ACTION_VIEW, authUrl.toUri()))
        }.onFailure { error ->
            authMessage = error.localizedMessage ?: "Dropbox sign-in could not open a browser."
        }
    }

    fun connectWebDav() {
        val serverUrl = webDavServerUrl.trim()
        val username = webDavUsername.trim()
        val password = webDavPassword
        if (serverUrl.isBlank() || username.isBlank() || password.isBlank()) {
            authMessage = "Enter the WebDAV server URL, username, and password to continue."
            showWebDavSetup = true
            return
        }

        authMessage = null
        coroutineScope.launch {
            runCatching {
                webDavApi.connect(
                    serverUrl = serverUrl,
                    username = username,
                    password = password
                )
            }.onSuccess { result ->
                viewModel.onCloudAuthComplete(
                    provider = CloudProvider.WEB_DAV,
                    accessToken = password,
                    displayName = result.displayName,
                    email = username,
                    accountId = result.accountId,
                    serverUrl = result.serverUrl,
                    usedBytes = result.usedBytes,
                    totalBytes = result.totalBytes
                )
                authMessage = null
                showWebDavSetup = false
            }.onFailure { error ->
                authMessage = error.localizedMessage ?: "WebDAV connection failed."
                showWebDavSetup = true
            }
        }
    }

    LaunchedEffect(pendingCloudAuthUri) {
        val callbackUri = pendingCloudAuthUri ?: return@LaunchedEffect
        if (callbackUri.scheme != DROPBOX_REDIRECT_SCHEME || callbackUri.host != DROPBOX_REDIRECT_HOST) {
            onConsumePendingCloudAuthUri()
            return@LaunchedEffect
        }

        runCatching { dropboxApi.finishAuthentication(callbackUri) }
            .onSuccess { auth ->
                authMessage = null
                viewModel.onCloudAuthComplete(
                    provider = CloudProvider.DROPBOX,
                    accessToken = auth.accessToken,
                    displayName = auth.displayName,
                    email = auth.email,
                    photoUrl = auth.photoUrl,
                    refreshToken = auth.refreshToken,
                    accessTokenExpiresAt = auth.accessTokenExpiresAt,
                    accountId = auth.accountId,
                    usedBytes = auth.usedBytes,
                    totalBytes = auth.totalBytes
                )
            }
            .onFailure { error ->
                authMessage = error.localizedMessage ?: "Dropbox sign-in failed."
            }

        onConsumePendingCloudAuthUri()
    }

    val primaryAccount = linkedAccounts.firstOrNull()
    val activeProvider = primaryAccount?.provider
    LaunchedEffect(activeProvider) {
        if (activeProvider == null) {
            showWebDavSetup = true
            selectedProvider = CloudProvider.WEB_DAV
        } else {
            selectedProvider = activeProvider
        }
    }
    LaunchedEffect(primaryAccount?.id) {
        if (primaryAccount?.provider == CloudProvider.WEB_DAV) {
            webDavServerUrl = primaryAccount.serverUrl.orEmpty()
            webDavUsername = primaryAccount.email.orEmpty()
            webDavPassword = primaryAccount.accessToken
        }
    }
    val cardColor = if (liquidGlassEnabled) {
        MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)
    } else {
        MaterialTheme.colorScheme.surface.copy(alpha = 0.96f)
    }
    val cardBorder = if (liquidGlassEnabled) {
        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.28f)
    } else {
        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.22f)
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        val previewProvider = activeProvider ?: selectedProvider
        val heroTitle = when (previewProvider) {
            CloudProvider.WEB_DAV -> "WebDAV Backup"
            CloudProvider.DROPBOX -> "Dropbox Backup"
            CloudProvider.GOOGLE_DRIVE -> "Google Drive Backup"
            else -> "Cloud Backup"
        }
        val heroDescription = when (previewProvider) {
            CloudProvider.WEB_DAV -> "Your reader settings, highlights, notes, bookmarks, and app setup are ready to sync with your WebDAV server."
            CloudProvider.DROPBOX -> "Dropbox backup is parked for now. It will be ready once the app key and provider registration are configured."
            CloudProvider.GOOGLE_DRIVE -> "Google Drive backup is parked for now. It will be ready after the Google OAuth setup and app registration are finished."
            else -> "Choose a provider for app settings, reader settings, highlights, notes, bookmarks, and other backup data."
        }
        val heroStatusText = when {
            lastSyncTime != null -> "Last sync: $lastSyncTime"
            syncStatus != "Ready" -> syncStatus
            activeProvider == CloudProvider.WEB_DAV -> "WebDAV linked and ready."
            activeProvider == CloudProvider.DROPBOX -> "Dropbox linked and ready."
            activeProvider == CloudProvider.GOOGLE_DRIVE -> "Google Drive linked and ready."
            previewProvider == CloudProvider.WEB_DAV -> "Add your server details below to connect WebDAV."
            previewProvider == CloudProvider.DROPBOX -> "Disabled until the Dropbox app registration is ready."
            previewProvider == CloudProvider.GOOGLE_DRIVE -> "Disabled until the Google Drive app registration is ready."
            else -> "No cloud account linked yet."
        }

        CloudDriveHero(
            account = primaryAccount,
            isOnline = isOnline,
            isSyncing = isSyncing,
            statusText = heroStatusText,
            authMessage = authMessage,
            cloudMessage = cloudMessage,
            cardColor = cardColor,
            cardBorder = cardBorder,
            title = heroTitle,
            description = heroDescription
        )

        CloudProviderSelector(
            selectedProvider = previewProvider,
            activeProvider = activeProvider,
            cardColor = cardColor,
            cardBorder = cardBorder,
            onSelect = { provider ->
                if (provider != CloudProvider.WEB_DAV) return@CloudProviderSelector
                authMessage = null
                selectedProvider = provider
                showWebDavSetup = true
            }
        )

        if (primaryAccount != null) {
            CloudDriveAccountPanel(
                account = primaryAccount,
                providerLabel = providerLabel(primaryAccount.provider),
                storageUsedBytes = storageSummary.totalUsedBytes,
                storageTotalBytes = storageSummary.totalCapacityBytes,
                isSyncing = isSyncing,
                isOnline = isOnline,
                cardColor = cardColor,
                cardBorder = cardBorder,
                onSyncNow = { viewModel.syncCloudAccount(primaryAccount.id) },
                onRestoreLatest = { viewModel.restoreCloudAccount(primaryAccount.id) },
                storageAvailable = primaryAccount.provider != CloudProvider.WEB_DAV || storageSummary.totalCapacityBytes > 0L,
                onSignOut = {
                    if (primaryAccount.provider == CloudProvider.GOOGLE_DRIVE) {
                        googleSignInClient.signOut().addOnCompleteListener {
                            viewModel.removeCloudAccount(primaryAccount.id)
                        }
                    } else {
                        viewModel.removeCloudAccount(primaryAccount.id)
                    }
                },
                onScopeToggle = { scope, enabled ->
                    viewModel.setCloudAccountBackupScope(primaryAccount.id, scope, enabled)
                },
                onEditConnection = if (primaryAccount.provider == CloudProvider.WEB_DAV) {
                    { showWebDavSetup = !showWebDavSetup }
                } else {
                    null
                }
            )

            if (primaryAccount.provider == CloudProvider.WEB_DAV && showWebDavSetup) {
                WebDavSetupCard(
                    cardColor = cardColor,
                    cardBorder = cardBorder,
                    serverUrl = webDavServerUrl,
                    username = webDavUsername,
                    password = webDavPassword,
                    onServerUrlChange = { webDavServerUrl = it },
                    onUsernameChange = { webDavUsername = it },
                    onPasswordChange = { webDavPassword = it },
                    onConnect = ::connectWebDav
                )
            }
        } else {
            if (previewProvider == CloudProvider.WEB_DAV && showWebDavSetup) {
                WebDavSetupCard(
                    cardColor = cardColor,
                    cardBorder = cardBorder,
                    serverUrl = webDavServerUrl,
                    username = webDavUsername,
                    password = webDavPassword,
                    onServerUrlChange = { webDavServerUrl = it },
                    onUsernameChange = { webDavUsername = it },
                    onPasswordChange = { webDavPassword = it },
                    onConnect = ::connectWebDav
                )
            }
        }
    }
}
