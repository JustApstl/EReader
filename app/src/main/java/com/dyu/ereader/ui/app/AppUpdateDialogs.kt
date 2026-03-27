package com.dyu.ereader.ui.app

import android.content.Intent
import android.net.Uri
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.dyu.ereader.data.model.update.AppReleaseInfo
import com.dyu.ereader.data.model.update.AppUpdateUiState

@Composable
internal fun AppUpdateDialogs(
    updateState: AppUpdateUiState,
    onDismissUpdate: () -> Unit,
    onInstallLatestUpdate: () -> Unit,
    onDismissChangelog: () -> Unit
) {
    val context = LocalContext.current

    fun openRelease(release: AppReleaseInfo) {
        val targetUrl = release.downloadUrl ?: release.htmlUrl
        runCatching {
            context.startActivity(
                Intent(Intent.ACTION_VIEW, Uri.parse(targetUrl)).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            )
        }
    }

    if (updateState.showUpdatePrompt && updateState.latestRelease != null) {
        val release = updateState.latestRelease
        AlertDialog(
            onDismissRequest = onDismissUpdate,
            title = { Text("Update Available") },
            text = {
                Text(
                    text = buildString {
                        append("Version ")
                        append(release.versionName)
                        append(" is available.\n\n")
                        append(release.notes.take(900))
                    },
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (release.downloadUrl != null) {
                            onInstallLatestUpdate()
                        } else {
                            openRelease(release)
                        }
                        onDismissUpdate()
                    }
                ) {
                    Text("Update")
                }
            },
            dismissButton = {
                TextButton(onClick = onDismissUpdate) {
                    Text("Later")
                }
            }
        )
    }

    if (!updateState.showUpdatePrompt && updateState.showChangelogPrompt && updateState.changelogRelease != null) {
        val release = updateState.changelogRelease
        AlertDialog(
            onDismissRequest = onDismissChangelog,
            title = { Text("What's New ✨") },
            text = {
                Text(
                    text = buildString {
                        append("You're now on version ")
                        append(release.versionName)
                        append(" 🎉")
                        append("\n\n")
                        append("Here’s what changed:")
                        append("\n\n")
                        append(release.notes.take(1200))
                    },
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                TextButton(onClick = onDismissChangelog) {
                    Text("Close")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        runCatching {
                            context.startActivity(
                                Intent(Intent.ACTION_VIEW, Uri.parse(release.htmlUrl)).apply {
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                }
                            )
                        }
                    }
                ) {
                    Text("Full Changelog")
                }
            }
        )
    }
}
