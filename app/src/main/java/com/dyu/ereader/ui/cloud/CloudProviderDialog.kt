package com.dyu.ereader.ui.cloud

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.dyu.ereader.ui.home.HomeViewModel
import com.dyu.ereader.data.model.CloudProvider

data class CloudProviderInfo(
    val provider: CloudProvider,
    val name: String,
    val iconUrl: String?,
    val fallbackIcon: ImageVector,
    val color: Color,
    val description: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CloudProviderDialog(
    onProviderSelected: (CloudProvider) -> Unit,
    onDismiss: () -> Unit,
    viewModel: HomeViewModel
) {
    val providers = listOf(
        CloudProviderInfo(
            CloudProvider.GOOGLE_DRIVE,
            "Google Drive",
            "https://www.gstatic.com/images/branding/product/2x/drive_2020q4_48dp.png",
            Icons.Rounded.FolderOpen,
            Color(0xFF4285F4),
            "Sync with your Google account"
        ),
        CloudProviderInfo(
            CloudProvider.PROTON_DRIVE,
            "Proton Drive",
            "https://upload.wikimedia.org/wikipedia/commons/thumb/5/5c/Proton_Drive_logo.svg/64px-Proton_Drive_logo.svg.png",
            Icons.Rounded.Security,
            Color(0xFF6D4AFF),
            "End-to-end encrypted storage"
        ),
        CloudProviderInfo(
            CloudProvider.ONE_DRIVE,
            "OneDrive",
            "https://upload.wikimedia.org/wikipedia/commons/thumb/d/d9/Microsoft_Office_OneDrive_%282019-present%29.svg/64px-Microsoft_Office_OneDrive_%282019-present%29.svg.png",
            Icons.Rounded.Cloud,
            Color(0xFF0078D4),
            "Microsoft cloud storage"
        ),
        CloudProviderInfo(
            CloudProvider.DROPBOX,
            "Dropbox",
            "https://upload.wikimedia.org/wikipedia/commons/thumb/c/cb/Dropbox_logo_2017.svg/64px-Dropbox_logo_2017.svg.png",
            Icons.Rounded.CloudDone,
            Color(0xFF0061FF),
            "Simple cloud sync"
        )
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select Cloud Provider") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                providers.forEach { provider ->
                    CloudProviderCard(
                        provider = provider,
                        onClick = { 
                            onProviderSelected(provider.provider)
                            onDismiss()
                        }
                    )
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun CloudProviderCard(
    provider: CloudProviderInfo,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, provider.color.copy(alpha = 0.3f)),
        color = provider.color.copy(alpha = 0.05f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Surface(
                modifier = Modifier.size(48.dp),
                shape = RoundedCornerShape(12.dp),
                color = provider.color,
                tonalElevation = 4.dp
            ) {
                Box(contentAlignment = Alignment.Center) {
                    if (!provider.iconUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = provider.iconUrl,
                            contentDescription = provider.name,
                            modifier = Modifier.size(24.dp),
                            contentScale = ContentScale.Fit
                        )
                    } else {
                        Icon(
                            imageVector = provider.fallbackIcon,
                            contentDescription = provider.name,
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = provider.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = provider.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Icon(
                imageVector = Icons.Rounded.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}
