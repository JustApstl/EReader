package com.dyu.ereader.ui.reader.formats.mobi

import android.content.Intent
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
fun MobiReaderScreen(
    uri: String,
    onBack: () -> Unit,
    isPageMode: Boolean,
    onRequestScrollMode: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val parsedUri = Uri.parse(uri)
    val openIntent = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(parsedUri, "application/x-mobipocket-ebook")
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    val canOpen = openIntent.resolveActivity(context.packageManager) != null

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically)
    ) {
        Text(
            text = "MOBI support",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = if (isPageMode) {
                "Paged mode isn’t supported for MOBI yet. Switch to scroll mode or open it in another reader."
            } else if (canOpen) {
                "We can open this MOBI file with an installed reader."
            } else {
                "No MOBI reader app found. Install a compatible reader to open this file."
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        if (isPageMode) {
            OutlinedButton(onClick = onRequestScrollMode) {
                Text("Switch to Scroll Mode")
            }
        }
        Button(
            onClick = {
                val chooser = Intent.createChooser(openIntent, "Open MOBI with")
                context.startActivity(chooser)
            },
            enabled = canOpen
        ) {
            Text("Open in Reader App")
        }
        OutlinedButton(onClick = onBack) {
            Text("Back")
        }
    }
}
