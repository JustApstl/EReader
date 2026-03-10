package com.dyu.ereader.ui.cloud

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import coil.compose.AsyncImage
import com.dyu.ereader.data.model.CloudProvider
import com.dyu.ereader.ui.home.HomeViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CloudSyncSettings(
    modifier: Modifier = Modifier,
    liquidGlassEnabled: Boolean = false,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val isSignedIn by viewModel.isCloudSignedIn.collectAsState(initial = false)
    val provider by viewModel.cloudProvider.collectAsState(initial = CloudProvider.NONE)
    val syncStatus by viewModel.cloudSyncStatus.collectAsState(initial = "Unknown")
    val lastSyncTime by viewModel.lastSyncTime.collectAsState(initial = null)
    val isSyncing by viewModel.isSyncing.collectAsState(initial = false)
    
    val providers = listOf(
        CloudProviderInfo(CloudProvider.GOOGLE_DRIVE, "Google Drive", "https://www.gstatic.com/images/branding/product/2x/drive_2020q4_48dp.png", Icons.Rounded.Storage, Color(0xFF4285F4), "Sync with your Google account"),
        CloudProviderInfo(CloudProvider.PROTON_DRIVE, "Proton Drive", "https://upload.wikimedia.org/wikipedia/commons/thumb/5/5c/Proton_Drive_logo.svg/64px-Proton_Drive_logo.svg.png", Icons.Rounded.Lock, Color(0xFF6D4AFF), "End-to-end encrypted storage"),
        CloudProviderInfo(CloudProvider.ONE_DRIVE, "OneDrive", "https://upload.wikimedia.org/wikipedia/commons/thumb/d/d9/Microsoft_Office_OneDrive_%282019-present%29.svg/64px-Microsoft_Office_OneDrive_%282019-present%29.svg.png", Icons.Rounded.CloudQueue, Color(0xFF0078D4), "Microsoft cloud storage"),
        CloudProviderInfo(CloudProvider.DROPBOX, "Dropbox", "https://upload.wikimedia.org/wikipedia/commons/thumb/c/cb/Dropbox_logo_2017.svg/64px-Dropbox_logo_2017.svg.png", Icons.Rounded.Folder, Color(0xFF0061FF), "File synchronization service")
    )
    val providerLabel = providers.firstOrNull { it.provider == provider }?.name ?: "Unknown"

    @Composable
    fun ProviderItem(provider: CloudProviderInfo, onClick: () -> Unit) {
        Surface(
            onClick = onClick,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 2.dp),
            shape = RoundedCornerShape(20.dp),
            color = if (liquidGlassEnabled) MaterialTheme.colorScheme.surface.copy(alpha = 0.7f) else MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    modifier = Modifier.size(40.dp),
                    shape = CircleShape,
                    color = provider.color.copy(alpha = 0.1f)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        if (!provider.iconUrl.isNullOrBlank()) {
                            AsyncImage(
                                model = provider.iconUrl,
                                contentDescription = provider.name,
                                modifier = Modifier.size(20.dp),
                                contentScale = ContentScale.Fit
                            )
                        } else {
                            Icon(provider.fallbackIcon, null, modifier = Modifier.size(20.dp), tint = provider.color)
                        }
                    }
                }
                Spacer(Modifier.width(16.dp))
                Text(
                    provider.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.outline)
            }
        }
    }

    @Composable
    fun SyncDetailRow(label: String, value: String, valueColor: Color = MaterialTheme.colorScheme.onSurface, isPending: Boolean = false) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (isPending) {
                    CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                }
                Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = valueColor)
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Spacer(Modifier.height(8.dp))
        
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(28.dp),
            color = if (liquidGlassEnabled) MaterialTheme.colorScheme.surface.copy(alpha = 0.55f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = if (isSignedIn) "Cloud Backup Active" else "Cloud Backup Disabled",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                Text(
                    text = if (isSignedIn) "Connected: $providerLabel" else "Choose a provider to sync your progress",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    modifier = Modifier.padding(top = 4.dp)
                )
                
                if (isSignedIn) {
                    Spacer(Modifier.height(16.dp))
                    Button(
                        onClick = { viewModel.signOutFromCloud() },
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.surface,
                            contentColor = MaterialTheme.colorScheme.error
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.3f))
                    ) {
                        Text("Disconnect Account", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        if (!isSignedIn) {
            Text(
                "Available Providers",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 4.dp)
            )

            providers.forEach { provider ->
                ProviderItem(provider) {
                    viewModel.signInToCloud(provider.provider) 
                }
            }
        }

        if (isSignedIn) {
            Text(
                "Sync Details",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 4.dp)
            )

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                color = if (liquidGlassEnabled) MaterialTheme.colorScheme.surface.copy(alpha = 0.72f) else MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
            ) {
                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    SyncDetailRow(
                        label = "Current Status",
                        value = if (isSyncing) "Syncing..." else syncStatus,
                        valueColor = if (syncStatus == "Synced") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                        isPending = isSyncing
                    )
                    
                    lastSyncTime?.let { time ->
                        SyncDetailRow(label = "Last Sync", value = time)
                    }
                    
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
                    
                    Button(
                        onClick = { viewModel.syncNow() },
                        enabled = !isSyncing,
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Rounded.Sync, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Sync Now", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
