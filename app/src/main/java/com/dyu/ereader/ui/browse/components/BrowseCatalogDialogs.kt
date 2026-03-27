package com.dyu.ereader.ui.browse.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TextButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.dyu.ereader.data.repository.browse.normalizeBrowseCatalogUrl
import com.dyu.ereader.data.repository.browse.validateBrowseCatalogUrl
import com.dyu.ereader.ui.app.theme.UiTokens
import com.dyu.ereader.ui.components.dialogs.appDialogContainerColor
import com.dyu.ereader.ui.components.dialogs.appDialogTextFieldColors
import com.dyu.ereader.ui.components.dialogs.appTextFieldShape

private data class BrowseCatalogTemplate(
    val label: String,
    val suggestedTitle: String,
    val suggestedUrl: String?,
    val note: String,
    val authHint: String? = null
)

@Composable
internal fun AddSourceDialog(
    onDismiss: () -> Unit,
    onAdd: (title: String, url: String, username: String?, password: String?, apiKey: String?) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var url by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var apiKey by remember { mutableStateOf("") }
    var selectedTemplateLabel by remember { mutableStateOf<String?>(null) }
    val templates = remember {
        listOf(
            BrowseCatalogTemplate(
                label = "Komga",
                suggestedTitle = "Komga",
                suggestedUrl = "https://your-komga.example.com/opds/v1.2/catalog",
                note = "Komga's official docs use /opds/v1.2/catalog for OPDS v1 and /opds/v2/catalog for OPDS v2.",
                authHint = "Use either Username + Password or an API key."
            ),
            BrowseCatalogTemplate(
                label = "Calibre-Web",
                suggestedTitle = "Calibre-Web",
                suggestedUrl = "https://your-calibre-web.example.com/opds",
                note = "Calibre-Web exposes its OPDS feed at /opds once your server is set up.",
                authHint = "Most installs use Username + Password."
            ),
            BrowseCatalogTemplate(
                label = "Kavita",
                suggestedTitle = "Kavita",
                suggestedUrl = null,
                note = "Kavita gives each user a unique OPDS URL. Copy it from User Settings > OPDS in your Kavita server.",
                authHint = "Usually the copied OPDS URL already contains the auth key."
            )
        )
    }
    val selectedTemplate = remember(selectedTemplateLabel) {
        templates.firstOrNull { it.label == selectedTemplateLabel }
    }
    val normalizedUrl = remember(url) { normalizeBrowseCatalogUrl(url) }
    val urlError = remember(url) {
        when {
            url.isBlank() -> null
            normalizedUrl == null -> "Enter a valid OPDS catalog URL."
            else -> validateBrowseCatalogUrl(url)
        }
    }
    val canAdd = title.isNotBlank() && normalizedUrl != null && urlError == null

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = UiTokens.SettingsCardShape,
        containerColor = appDialogContainerColor(),
        titleContentColor = MaterialTheme.colorScheme.onSurface,
        textContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        title = { Text("Add Custom Source", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Paste a direct OPDS feed URL from a source you trust. Self-hosted OPDS servers work well for private comic and manga libraries too.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Quick templates",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        templates.forEach { template ->
                            AssistChip(
                                onClick = {
                                    selectedTemplateLabel = template.label
                                    title = template.suggestedTitle
                                    template.suggestedUrl?.let { url = it }
                                },
                                label = { Text(template.label) },
                                colors = AssistChipDefaults.assistChipColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                                    labelColor = MaterialTheme.colorScheme.onSurface
                                )
                            )
                        }
                    }
                    selectedTemplate?.let { template ->
                        Surface(
                            shape = UiTokens.SettingsCardShape,
                            color = MaterialTheme.colorScheme.surfaceContainerLow
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = template.note,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                template.authHint?.let { hint ->
                                    Text(
                                        text = hint,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                }
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Catalog Title") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = appTextFieldShape(),
                    colors = appDialogTextFieldColors()
                )
                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    label = { Text("OPDS URL") },
                    placeholder = { Text("https://example.com/opds") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = appTextFieldShape(),
                    colors = appDialogTextFieldColors(),
                    isError = urlError != null,
                    supportingText = {
                        when {
                            urlError != null -> Text(urlError)
                            normalizedUrl != null && normalizedUrl != url.trim() ->
                                Text("Will save as $normalizedUrl")
                        }
                    }
                )
                Text(
                    text = "Optional authentication",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text("Username") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = appTextFieldShape(),
                    colors = appDialogTextFieldColors()
                )
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = appTextFieldShape(),
                    colors = appDialogTextFieldColors()
                )
                OutlinedTextField(
                    value = apiKey,
                    onValueChange = { apiKey = it },
                    label = { Text("API Key") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = appTextFieldShape(),
                    colors = appDialogTextFieldColors()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val finalUrl = normalizedUrl ?: return@Button
                    if (title.isNotBlank()) {
                        onAdd(
                            title.trim(),
                            finalUrl,
                            username.trim().takeIf { it.isNotBlank() },
                            password.takeIf { it.isNotBlank() },
                            apiKey.trim().takeIf { it.isNotBlank() }
                        )
                    }
                },
                enabled = canAdd,
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
