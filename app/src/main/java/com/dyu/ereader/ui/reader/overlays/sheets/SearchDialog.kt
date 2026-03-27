package com.dyu.ereader.ui.reader.overlays.sheets

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dyu.ereader.data.model.search.SearchResult
import com.dyu.ereader.ui.components.buttons.AppChromeIconButton
import com.dyu.ereader.ui.components.dialogs.appDialogTextFieldColors
import com.dyu.ereader.ui.components.dialogs.appTextFieldShape
import com.dyu.ereader.ui.reader.overlays.components.ReaderControlBottomSheet
import com.dyu.ereader.ui.reader.overlays.components.ReaderPanelScaffold
import com.dyu.ereader.ui.reader.overlays.components.ReaderSheetSection

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SearchPanelContent(
    onDismiss: () -> Unit,
    onResultSelected: (SearchResult) -> Unit,
    onSearch: (String) -> Unit = {},
    results: List<SearchResult> = emptyList(),
    isSearching: Boolean = false,
    modifier: Modifier = Modifier
) {
    var query by remember { mutableStateOf("") }

    ReaderPanelScaffold(
        title = "Search",
        icon = Icons.Rounded.Search,
        onDismiss = onDismiss,
        closeContentDescription = "Close Search",
        modifier = modifier
    ) {
        ReaderSheetSection(
            title = "Search in Book",
            icon = Icons.Rounded.Search
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = {
                    query = it
                    onSearch(it)
                },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Search", style = MaterialTheme.typography.bodyMedium) },
                leadingIcon = { Icon(Icons.Rounded.Search, null, modifier = Modifier.size(20.dp)) },
                trailingIcon = {
                    if (isSearching) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    } else if (query.isNotEmpty()) {
                        AppChromeIconButton(
                            icon = Icons.Rounded.Close,
                            contentDescription = "Clear search",
                            onClick = { query = ""; onSearch("") },
                            size = 24.dp,
                            iconSize = 14.dp
                        )
                    }
                },
                shape = appTextFieldShape(),
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyMedium,
                colors = appDialogTextFieldColors()
            )
        }

        if (query.isEmpty()) {
            ReaderSheetSection(
                title = "Ready",
                icon = Icons.Rounded.Search
            ) {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Rounded.Search,
                            null,
                            modifier = Modifier.size(42.dp).alpha(0.2f),
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            "Ready to search",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else if (results.isEmpty() && !isSearching) {
            ReaderSheetSection(
                title = "No Results",
                icon = Icons.Rounded.Search
            ) {
                Text(
                    "No matches found",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            ReaderSheetSection(
                title = "Results",
                icon = Icons.Rounded.Search,
                modifier = Modifier.weight(1f, fill = false)
            ) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 420.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(results) { result ->
                        SearchResultItem(result) {
                            onResultSelected(result)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchDialog(
    onDismiss: () -> Unit,
    onResultSelected: (SearchResult) -> Unit,
    onSearch: (String) -> Unit = {},
    results: List<SearchResult> = emptyList(),
    isSearching: Boolean = false
) {
    ReaderControlBottomSheet(
        onDismissRequest = onDismiss
    ) {
        SearchPanelContent(
            onDismiss = onDismiss,
            onResultSelected = onResultSelected,
            onSearch = onSearch,
            results = results,
            isSearching = isSearching,
            modifier = Modifier.fillMaxSize()
        )
    }
}

@Composable
private fun SearchResultItem(result: SearchResult, onClick: () -> Unit) {
    val annotatedText = if (result.matchStart >= 0 && result.matchEnd > result.matchStart && result.matchEnd <= result.textContext.length) {
        buildAnnotatedString {
            append(result.textContext)
            addStyle(
                SpanStyle(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                ),
                start = result.matchStart,
                end = result.matchEnd
            )
        }
    } else {
        buildAnnotatedString { append(result.textContext) }
    }

    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = annotatedText,
                style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 20.sp),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 3
            )
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = result.chapterTitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = "${(result.percentage * 100).toInt()}%",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                        fontWeight = FontWeight.Black
                    )
                }
            }
        }
    }
}
