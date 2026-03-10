package com.dyu.ereader.ui.browse

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import coil.compose.AsyncImage
import com.dyu.ereader.data.model.BrowseBook
import com.dyu.ereader.data.model.BrowseCatalog
import com.dyu.ereader.ui.home.HomeViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrowseCatalogScreen(
    modifier: Modifier = Modifier,
    liquidGlassEnabled: Boolean = false,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val browseCatalogs by viewModel.browseCatalogs.collectAsState(initial = emptyList())
    val currentFeed by viewModel.currentFeed.collectAsState()
    val isLoading by viewModel.isLoadingBrowse.collectAsState(initial = false)
    val isDownloading by viewModel.isDownloadingBrowse.collectAsState(initial = false)
    val downloadProgress by viewModel.browseDownloadProgress.collectAsState(initial = 0f)
    val downloadMessage by viewModel.browseDownloadMessage.collectAsState(initial = null)
    val error by viewModel.browseError.collectAsState(initial = null)
    var currentUrl by remember { mutableStateOf("https://standardebooks.org/feeds/opds/all") }
    var showAddSourceDialog by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(downloadMessage) {
        val message = downloadMessage ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(message)
        viewModel.consumeBrowseDownloadMessage()
    }

    if (showAddSourceDialog) {
        AddSourceDialog(
            onDismiss = { showAddSourceDialog = false },
            onAdd = { title, url ->
                val normalizedUrl = normalizeSourceUrl(url) ?: return@AddSourceDialog
                viewModel.addBrowseCatalog(
                    BrowseCatalog(
                        id = normalizedUrl.hashCode().toString(),
                        title = title,
                        url = normalizedUrl,
                        description = "Custom OPDS source",
                        icon = "https://www.google.com/s2/favicons?domain_url=$normalizedUrl&sz=64"
                    )
                )
                showAddSourceDialog = false
            }
        )
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
            Spacer(Modifier.height(8.dp))
            
            // Navigation & URL Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (currentFeed != null || error != null) {
                    IconButton(
                        onClick = { viewModel.clearBrowseFeed() },
                        modifier = Modifier.size(40.dp).background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), CircleShape)
                    ) {
                        Icon(
                            Icons.AutoMirrored.Rounded.ArrowBack, 
                            "Back", 
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Surface(
                    modifier = Modifier.weight(1f).height(52.dp),
                    shape = CircleShape,
                    color = if (liquidGlassEnabled) MaterialTheme.colorScheme.surface.copy(alpha = 0.7f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Rounded.Language, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(12.dp))
                        Box(modifier = Modifier.weight(1f)) {
                            androidx.compose.foundation.text.BasicTextField(
                                value = currentUrl,
                                onValueChange = { currentUrl = it },
                                modifier = Modifier.fillMaxWidth(),
                                textStyle = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface),
                                singleLine = true
                            )
                        }
                        IconButton(
                            onClick = {
                                val normalized = normalizeSourceUrl(currentUrl)
                                if (normalized == null) {
                                    scope.launch {
                                        snackbarHostState.showSnackbar("Enter a valid OPDS source URL")
                                    }
                                } else {
                                    currentUrl = normalized
                                    viewModel.loadBrowseCatalog(BrowseCatalog(id = "custom", title = "Custom", url = normalized))
                                }
                            },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(Icons.Rounded.Search, "Browse", tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }

            if (isDownloading) {
                Spacer(Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { (downloadProgress / 100f).coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(Modifier.height(24.dp))

            AnimatedContent(
                targetState = if (isLoading) "loading" else if (error != null) "error" else if (currentFeed != null) "feed" else "catalogs",
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "BrowseContent"
            ) { state ->
                when (state) {
                    "loading" -> {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(strokeWidth = 3.dp, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                    "error" -> {
                        ErrorState(error = error ?: "Unknown error", onRetry = { 
                            viewModel.loadBrowseCatalog(BrowseCatalog(id = "retry", title = "Retry", url = currentUrl)) 
                        })
                    }
                    "feed" -> {
                        currentFeed?.let { feed ->
                            FeedContent(
                                feed = feed,
                                liquidGlassEnabled = liquidGlassEnabled,
                                onBookClick = { book -> viewModel.downloadBrowseBook(book) }
                            )
                        }
                    }
                    else -> {
                        CatalogList(
                            catalogs = browseCatalogs,
                            liquidGlassEnabled = liquidGlassEnabled,
                            onCatalogClick = { catalog -> 
                                currentUrl = catalog.url
                                viewModel.loadBrowseCatalog(catalog) 
                            },
                            onAddSourceClick = { showAddSourceDialog = true }
                        )
                    }
                }
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 24.dp)
        )
    }
}

private fun normalizeSourceUrl(raw: String): String? {
    val trimmed = raw.trim()
    if (trimmed.isBlank()) return null
    return when {
        trimmed.startsWith("http://", ignoreCase = true) || trimmed.startsWith("https://", ignoreCase = true) -> trimmed
        else -> "https://$trimmed"
    }
}

@Composable
private fun AddSourceDialog(onDismiss: () -> Unit, onAdd: (String, String) -> Unit) {
    var title by remember { mutableStateOf("") }
    var url by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Custom Source", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Catalog Title") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    label = { Text("OPDS URL") },
                    placeholder = { Text("https://...") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { if (title.isNotBlank() && url.isNotBlank()) onAdd(title, url) },
                enabled = title.isNotBlank() && url.isNotBlank(),
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

@Composable
private fun CatalogList(
    catalogs: List<BrowseCatalog>,
    liquidGlassEnabled: Boolean,
    onCatalogClick: (BrowseCatalog) -> Unit,
    onAddSourceClick: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            "Available Sources",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.onSurface
        )
        
        Spacer(Modifier.height(12.dp))

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 100.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(catalogs) { catalog ->
                BrowseCatalogItem(
                    catalog = catalog,
                    liquidGlassEnabled = liquidGlassEnabled,
                    onClick = { onCatalogClick(catalog) }
                )
            }
            
            item {
                AddCatalogButton(onClick = onAddSourceClick)
            }
        }
    }
}

@Composable
private fun FeedContent(
    feed: com.dyu.ereader.data.model.BrowseFeed,
    liquidGlassEnabled: Boolean,
    onBookClick: (BrowseBook) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            feed.title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        
        Spacer(Modifier.height(12.dp))

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 100.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(feed.entries) { book ->
                BrowseBookItem(
                    book = book,
                    liquidGlassEnabled = liquidGlassEnabled,
                    onClick = { onBookClick(book) }
                )
            }
        }
    }
}

@Composable
private fun BrowseBookItem(
    book: BrowseBook,
    liquidGlassEnabled: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = if (liquidGlassEnabled) MaterialTheme.colorScheme.surface.copy(alpha = 0.72f) else MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
    ) {
        Row(modifier = Modifier.padding(12.dp)) {
            Surface(
                modifier = Modifier.size(80.dp, 120.dp),
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {
                AsyncImage(
                    model = book.coverUrl,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
            
            Spacer(Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = book.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = book.author,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = book.summary ?: "",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }
            
            IconButton(onClick = onClick, modifier = Modifier.align(Alignment.Bottom)) {
                Icon(Icons.Rounded.FileDownload, null, tint = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

@Composable
private fun BrowseCatalogItem(
    catalog: BrowseCatalog,
    liquidGlassEnabled: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = if (liquidGlassEnabled) MaterialTheme.colorScheme.surface.copy(alpha = 0.72f) else MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(48.dp),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    if (!catalog.icon.isNullOrBlank()) {
                        AsyncImage(
                            model = catalog.icon,
                            contentDescription = null,
                            modifier = Modifier.size(24.dp).clip(CircleShape),
                            contentScale = ContentScale.Fit
                        )
                    } else {
                        Icon(Icons.Rounded.Language, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                    }
                }
            }
            
            Spacer(Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = catalog.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = catalog.description ?: catalog.url,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            
            Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.outline)
        }
    }
}

@Composable
private fun ErrorState(error: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Rounded.ErrorOutline, null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.error)
        Spacer(Modifier.height(16.dp))
        Text("Connection Failed", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
        Text(error, textAlign = androidx.compose.ui.text.style.TextAlign.Center, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(24.dp))
        Button(onClick = onRetry) { Text("Try Again") }
    }
}

@Composable
private fun AddCatalogButton(onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().height(56.dp),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Icon(Icons.Rounded.Add, null)
        Spacer(Modifier.width(8.dp))
        Text("Add Custom Source")
    }
}
