@file:Suppress("DEPRECATION")

package com.dyu.ereader.ui.home.settings.cloud

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.dyu.ereader.data.model.cloud.CloudProvider
import com.google.android.gms.auth.api.signin.GoogleSignInStatusCodes
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.CommonStatusCodes

internal fun providerLabel(provider: CloudProvider): String {
    return when (provider) {
        CloudProvider.WEB_DAV -> "WebDAV"
        CloudProvider.DROPBOX -> "Dropbox"
        CloudProvider.GOOGLE_DRIVE -> "Google Drive"
        CloudProvider.PROTON_DRIVE -> "Proton Drive"
        CloudProvider.ONE_DRIVE -> "OneDrive"
        CloudProvider.NONE -> "Cloud"
    }
}

internal fun resolveGoogleSignInMessage(error: Throwable): String {
    val apiException = error as? ApiException
    return when (apiException?.statusCode) {
        GoogleSignInStatusCodes.SIGN_IN_CANCELLED ->
            "Google sign-in was cancelled before Drive access was granted."
        GoogleSignInStatusCodes.SIGN_IN_CURRENTLY_IN_PROGRESS ->
            "Google sign-in is already in progress. Please wait a moment and try again."
        CommonStatusCodes.NETWORK_ERROR ->
            "Google sign-in needs an internet connection. Check your network and try again."
        CommonStatusCodes.DEVELOPER_ERROR ->
            "Google Sign-In is misconfigured for this app. Check the Android OAuth client, package name, and SHA certificate."
        CommonStatusCodes.INVALID_ACCOUNT ->
            "This Google account could not be used for Drive backup. Try another account."
        CommonStatusCodes.SIGN_IN_REQUIRED ->
            "Google Drive permission is still required. Please sign in again to continue."
        else -> error.localizedMessage
            ?: apiException?.statusCode?.let(CommonStatusCodes::getStatusCodeString)
            ?: "Google sign-in failed."
    }
}

@Composable
internal fun rememberNetworkAvailability(): Boolean {
    val context = LocalContext.current
    var isOnline by remember { mutableStateOf(context.isNetworkCurrentlyAvailable()) }

    DisposableEffect(context) {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        if (connectivityManager == null) {
            onDispose { }
        } else {
            val callback = object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    isOnline = true
                }

                override fun onLost(network: Network) {
                    isOnline = context.isNetworkCurrentlyAvailable()
                }

                override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
                    isOnline = networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                        networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
                }
            }
            connectivityManager.registerDefaultNetworkCallback(callback)
            onDispose {
                runCatching { connectivityManager.unregisterNetworkCallback(callback) }
            }
        }
    }

    return isOnline
}

internal fun ratioOf(usedBytes: Long, totalBytes: Long): Float {
    if (totalBytes <= 0L) return 0f
    return (usedBytes.toFloat() / totalBytes.toFloat()).coerceIn(0f, 1f)
}

private fun Context.isNetworkCurrentlyAvailable(): Boolean {
    val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false
    val activeNetwork = connectivityManager.activeNetwork ?: return false
    val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork) ?: return false
    return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
        capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
}

internal const val DROPBOX_REDIRECT_SCHEME = "ereader"
internal const val DROPBOX_REDIRECT_HOST = "dropbox-auth"
