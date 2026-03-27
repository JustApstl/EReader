@file:Suppress("DEPRECATION")

package com.dyu.ereader.ui.home.settings.cloud

import android.text.format.Formatter
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CloudSync
import androidx.compose.material.icons.rounded.Dns
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.LinkOff
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Restore
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.dyu.ereader.data.model.cloud.CloudBackupScope
import com.dyu.ereader.data.model.cloud.CloudLinkedAccount
import com.dyu.ereader.data.model.cloud.CloudProvider
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

@Composable
internal fun CloudDriveHero(
    account: CloudLinkedAccount?,
    isOnline: Boolean,
    isSyncing: Boolean,
    statusText: String,
    authMessage: String?,
    cloudMessage: String?,
    cardColor: Color,
    cardBorder: Color,
    title: String,
    description: String
) {
    val statusAccent = when {
        isSyncing -> MaterialTheme.colorScheme.primary
        account != null -> MaterialTheme.colorScheme.secondary
        else -> MaterialTheme.colorScheme.outline
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(30.dp),
        color = cardColor,
        border = BorderStroke(1.dp, cardBorder)
    ) {
        Column(
            modifier = Modifier
                .background(
                    Brush.verticalGradient(
                        listOf(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
                            Color.Transparent
                        )
                    )
                )
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Surface(
                    modifier = Modifier.size(60.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Rounded.CloudSync,
                            contentDescription = null,
                            modifier = Modifier.size(28.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                StatusPill(
                    icon = if (isOnline) Icons.Rounded.Link else Icons.Rounded.LinkOff,
                    label = if (isOnline) "Online" else "Offline",
                    accent = if (isOnline) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                )
                StatusPill(
                    icon = Icons.Rounded.CloudSync,
                    label = when {
                        isSyncing -> "Syncing"
                        account != null -> "Linked"
                        else -> "Not linked"
                    },
                    accent = statusAccent
                )
            }

            authMessage?.let { message ->
                MessageBanner(
                    text = message,
                    accent = MaterialTheme.colorScheme.error
                )
            }

            cloudMessage?.let { message ->
                MessageBanner(
                    text = message,
                    accent = MaterialTheme.colorScheme.primary
                )
            }

            Text(
                text = statusText,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun CloudDriveAccountPanel(
    account: CloudLinkedAccount,
    providerLabel: String,
    storageUsedBytes: Long,
    storageTotalBytes: Long,
    storageAvailable: Boolean,
    isSyncing: Boolean,
    isOnline: Boolean,
    cardColor: Color,
    cardBorder: Color,
    onSyncNow: () -> Unit,
    onRestoreLatest: () -> Unit,
    onSignOut: () -> Unit,
    onScopeToggle: (CloudBackupScope, Boolean) -> Unit,
    onEditConnection: (() -> Unit)?
) {
    val context = LocalContext.current
    val usageRatio = ratioOf(storageUsedBytes, storageTotalBytes)
    val availableBytes = (storageTotalBytes - storageUsedBytes).coerceAtLeast(0L)
    val timestampFormat = remember { SimpleDateFormat("MMM d, yyyy • HH:mm", Locale.getDefault()) }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        color = cardColor,
        border = BorderStroke(1.dp, cardBorder)
    ) {
        Column(
            modifier = Modifier.padding(22.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Surface(
                    modifier = Modifier.size(58.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        if (!account.photoUrl.isNullOrBlank()) {
                            AsyncImage(
                                model = account.photoUrl,
                                contentDescription = account.displayName,
                                modifier = Modifier.fillMaxWidth(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Rounded.Person,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = account.displayName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = account.email ?: "$providerLabel account",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                StatusPill(
                    icon = if (isOnline) Icons.Rounded.Link else Icons.Rounded.LinkOff,
                    label = if (isOnline) "Online" else "Offline",
                    accent = if (isOnline) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "$providerLabel Storage",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "${(usageRatio * 100).roundToInt()}%",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                LinearProgressIndicator(
                    progress = { usageRatio },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(10.dp),
                    trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                )
                Text(
                    text = if (storageAvailable) {
                        "${Formatter.formatShortFileSize(context, storageUsedBytes)} used of ${Formatter.formatShortFileSize(context, storageTotalBytes)}"
                    } else {
                        "Server storage usage is not reported by this WebDAV provider."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = if (storageAvailable) {
                        "${Formatter.formatShortFileSize(context, availableBytes)} available • backup footprint ${Formatter.formatShortFileSize(context, account.backupBytes)}"
                    } else {
                        "Backup footprint ${Formatter.formatShortFileSize(context, account.backupBytes)}"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = if (account.lastBackupTime > 0L) {
                        "Last backup: ${timestampFormat.format(Date(account.lastBackupTime))}"
                    } else {
                        "No backup uploaded yet."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "What To Sync",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CloudBackupScope.entries.forEach { scope ->
                        val enabled = account.enabledBackupScopes.contains(scope)
                        FilterChip(
                            selected = enabled,
                            onClick = { onScopeToggle(scope, !enabled) },
                            label = { Text(scope.label) }
                        )
                    }
                }
                Text(
                    text = "Recommended: keep App Settings, Reader Settings, and Annotations enabled for the smoothest restore.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = onSyncNow,
                    enabled = !isSyncing,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Icon(Icons.Rounded.CloudSync, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(if (isSyncing) "Syncing Backup" else "Sync Backup", fontWeight = FontWeight.Bold)
                }
                OutlinedButton(
                    onClick = onRestoreLatest,
                    enabled = !isSyncing,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Icon(Icons.Rounded.Restore, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Restore Latest Backup", fontWeight = FontWeight.Bold)
                }

                if (onEditConnection != null) {
                    OutlinedButton(
                        onClick = onEditConnection,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp)
                    ) {
                        Icon(Icons.Rounded.Dns, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Edit WebDAV Connection", fontWeight = FontWeight.Bold)
                    }
                }
            }

            OutlinedButton(
                onClick = onSignOut,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.25f))
            ) {
                Icon(Icons.Rounded.LinkOff, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Disconnect $providerLabel", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
internal fun CloudProviderSelector(
    selectedProvider: CloudProvider,
    activeProvider: CloudProvider?,
    cardColor: Color,
    cardBorder: Color,
    onSelect: (CloudProvider) -> Unit
) {
    val providers = listOf(
        ProviderOption(
            provider = CloudProvider.WEB_DAV,
            label = "WebDAV",
            description = "Use Nextcloud, NAS storage, or any WebDAV folder you control.",
            icon = Icons.Rounded.Dns,
            enabled = true,
            status = "Available"
        ),
        ProviderOption(
            provider = CloudProvider.DROPBOX,
            label = "Dropbox",
            description = "Visible for the future, but disabled until the Dropbox app registration is finished.",
            icon = Icons.Rounded.CloudSync,
            enabled = false,
            status = "Disabled"
        ),
        ProviderOption(
            provider = CloudProvider.GOOGLE_DRIVE,
            label = "Google Drive",
            description = "Visible for the future, but disabled until the Google OAuth and Drive setup are finished.",
            icon = Icons.Rounded.Storage,
            enabled = false,
            status = "Disabled"
        )
    )

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = cardColor,
        border = BorderStroke(1.dp, cardBorder)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Providers",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.primary
            )
            providers.forEach { option ->
                val isSelected = activeProvider == option.provider || selectedProvider == option.provider
                CloudProviderOptionCard(
                    option = option,
                    selected = isSelected,
                    active = activeProvider == option.provider,
                    onClick = { onSelect(option.provider) }
                )
            }
        }
    }
}

@Composable
internal fun WebDavSetupCard(
    cardColor: Color,
    cardBorder: Color,
    serverUrl: String,
    username: String,
    password: String,
    onServerUrlChange: (String) -> Unit,
    onUsernameChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onConnect: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = cardColor,
        border = BorderStroke(1.dp, cardBorder)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = "WebDAV Server",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.ExtraBold
            )
            Text(
                text = "Enter the folder URL where backups should be stored, plus the username and password or app password for that server.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            OutlinedTextField(
                value = serverUrl,
                onValueChange = onServerUrlChange,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text("Server URL") },
                placeholder = { Text("https://cloud.example.com/remote.php/dav/files/name/EReader/") }
            )
            OutlinedTextField(
                value = username,
                onValueChange = onUsernameChange,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text("Username") }
            )
            OutlinedTextField(
                value = password,
                onValueChange = onPasswordChange,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text("Password or App Password") },
                visualTransformation = PasswordVisualTransformation()
            )
            Button(
                onClick = onConnect,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp)
            ) {
                Icon(Icons.Rounded.CloudSync, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Save And Connect WebDAV", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
internal fun StatusPill(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    accent: Color
) {
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = accent.copy(alpha = 0.12f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(icon, null, modifier = Modifier.size(14.dp), tint = accent)
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = accent
            )
        }
    }
}

@Composable
internal fun MessageBanner(
    text: String,
    accent: Color
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = accent.copy(alpha = 0.12f),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.18f))
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            style = MaterialTheme.typography.bodySmall,
            color = accent
        )
    }
}

@Composable
private fun CloudProviderOptionCard(
    option: ProviderOption,
    selected: Boolean,
    active: Boolean,
    onClick: () -> Unit
) {
    val containerColor = when {
        active -> MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
        selected -> MaterialTheme.colorScheme.secondary.copy(alpha = 0.08f)
        else -> MaterialTheme.colorScheme.surface
    }
    val outlineColor = when {
        active -> MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
        selected -> MaterialTheme.colorScheme.secondary.copy(alpha = 0.28f)
        else -> MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
    }
    val statusAccent = when {
        active -> MaterialTheme.colorScheme.primary
        option.enabled -> MaterialTheme.colorScheme.secondary
        else -> MaterialTheme.colorScheme.outline
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = containerColor,
        border = BorderStroke(1.dp, outlineColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(enabled = option.enabled, onClick = onClick)
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(48.dp),
                shape = CircleShape,
                color = statusAccent.copy(alpha = 0.12f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = option.icon,
                        contentDescription = null,
                        modifier = Modifier.size(22.dp),
                        tint = statusAccent
                    )
                }
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = option.label,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    text = option.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            StatusPill(
                icon = when {
                    active -> Icons.Rounded.Link
                    option.enabled -> Icons.Rounded.CloudSync
                    else -> Icons.Rounded.Storage
                },
                label = if (active) "Connected" else option.status,
                accent = statusAccent
            )
        }
    }
}

private data class ProviderOption(
    val provider: CloudProvider,
    val label: String,
    val description: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val enabled: Boolean,
    val status: String
)
