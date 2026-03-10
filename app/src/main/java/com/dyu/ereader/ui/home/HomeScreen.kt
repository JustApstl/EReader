package com.dyu.ereader.ui.home

import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.List
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.automirrored.rounded.Sort
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.dyu.ereader.data.model.AppTheme
import com.dyu.ereader.data.model.BookItem
import com.dyu.ereader.data.model.BookType
import com.dyu.ereader.data.model.NavigationBarStyle
import com.dyu.ereader.data.model.ReaderControl
import com.dyu.ereader.ui.components.AddBookCard
import com.dyu.ereader.ui.components.AddBookListItem
import com.dyu.ereader.ui.components.BookCard
import com.dyu.ereader.ui.components.BookListItem
import com.dyu.ereader.ui.browse.BrowseCatalogScreen
import com.dyu.ereader.ui.cloud.CloudSyncSettings
import com.dyu.ereader.util.Logger

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    uiState: HomeUiState,
    appTheme: AppTheme,
    navBarStyle: NavigationBarStyle,
    liquidGlassEnabled: Boolean = false,
    onSearchChanged: (String) -> Unit,
    onSortOrderChanged: (SortOrder) -> Unit,
    onRefresh: () -> Unit,
    onLibraryAccessGranted: (Uri) -> Unit,
    onRevokeLibraryAccess: () -> Unit,
    onOpenBook: (BookItem) -> Unit,
    onAppThemeChange: (AppTheme) -> Unit,
    onNavigationBarStyleChange: (NavigationBarStyle) -> Unit,
    onLiquidGlassToggle: (Boolean) -> Unit = {},
    onAnimationsToggle: (Boolean) -> Unit = {},
    onToggleFavorite: (String, Boolean) -> Unit,
    onToggleLayout: () -> Unit,
    onShowBookTypeChanged: (Boolean) -> Unit,
    onShowRecentReadingChanged: (Boolean) -> Unit,
    onShowFavoritesChanged: (Boolean) -> Unit,
    onShowGenresChanged: (Boolean) -> Unit,
    onHideStatusBarChanged: (Boolean) -> Unit,
    onGridColumnsChanged: (Int) -> Unit = {},
    onToggleTypeFilter: (BookType) -> Unit = {},
    onToggleGenreFilter: (String) -> Unit = {},
    onClearAdvancedFilters: () -> Unit = {},
    onExportSettings: suspend () -> String,
    onImportSettings: (String) -> Unit,
    onToggleReaderSearch: (Boolean) -> Unit,
    onToggleReaderTTS: (Boolean) -> Unit,
    onToggleReaderAccessibility: (Boolean) -> Unit,
    onToggleReaderAnalytics: (Boolean) -> Unit,
    onToggleReaderExport: (Boolean) -> Unit,
    onReaderControlOrderChanged: (List<ReaderControl>) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showSortMenu by remember { mutableStateOf(false) }
    var showFilterSheet by remember { mutableStateOf(false) }
    var selectedBookForInfo by remember { mutableStateOf<BookItem?>(null) }
    var currentTab by remember { mutableIntStateOf(0) } // 0: Library, 1: Browse, 2: Settings, 3: Cloud, 4: Logs
    
    DisposableEffect(uiState.display.hideStatusBar) {
        val window = (context as? ComponentActivity)?.window ?: return@DisposableEffect onDispose {}
        val insetsController = WindowCompat.getInsetsController(window, window.decorView)
        insetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        
        if (uiState.display.hideStatusBar) {
            insetsController.hide(androidx.core.view.WindowInsetsCompat.Type.statusBars())
        } else {
            insetsController.show(androidx.core.view.WindowInsetsCompat.Type.statusBars())
        }
        
        onDispose { 
            insetsController.show(androidx.core.view.WindowInsetsCompat.Type.statusBars()) 
        }
    }

    val treePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if (uri != null) {
            onLibraryAccessGranted(uri)
        }
    }

    if (selectedBookForInfo != null) {
        BookDetailsDialog(
            book = selectedBookForInfo!!,
            onDismiss = { selectedBookForInfo = null },
            onRead = { 
                onOpenBook(selectedBookForInfo!!)
                selectedBookForInfo = null 
            }
        )
    }

    if (showFilterSheet) {
        FilterBottomSheet(
            uiState = uiState,
            onDismiss = { showFilterSheet = false },
            onToggleType = onToggleTypeFilter,
            onToggleGenre = onToggleGenreFilter,
            onReset = onClearAdvancedFilters
        )
    }

    Box(modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        if (liquidGlassEnabled) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                MaterialTheme.colorScheme.background,
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
                            )
                        )
                    )
            )
        }

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            contentWindowInsets = WindowInsets(0.dp),
            containerColor = Color.Transparent,
            bottomBar = {
                if (navBarStyle == NavigationBarStyle.DEFAULT) {
                    NavigationBar(
                        containerColor = if (liquidGlassEnabled) MaterialTheme.colorScheme.surface.copy(alpha = 0.85f) else MaterialTheme.colorScheme.surface,
                        tonalElevation = 8.dp
                    ) {
                        NavigationBarItem(
                            selected = currentTab == 0,
                            onClick = { currentTab = 0 },
                            icon = { Icon(Icons.AutoMirrored.Rounded.MenuBook, "Library") },
                            label = { Text("Library") }
                        )
                        NavigationBarItem(
                            selected = currentTab == 1,
                            onClick = { currentTab = 1 },
                            icon = { Icon(Icons.Rounded.Explore, "Browse") },
                            label = { Text("Browse") }
                        )
                        NavigationBarItem(
                            selected = currentTab == 2,
                            onClick = { currentTab = 2 },
                            icon = { Icon(Icons.Rounded.Settings, "Settings") },
                            label = { Text("Settings") }
                        )
                    }
                }
            }
        ) { paddingValues ->
            Column(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
                // Header
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 20.dp, vertical = 16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = when(currentTab) {
                                    0 -> "Library"
                                    1 -> "Browse"
                                    2 -> "Settings"
                                    3 -> "Cloud Backup"
                                    else -> "System Logs"
                                },
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            if (currentTab == 0) {
                                Text(
                                    text = "${uiState.visibleBooks.size} books available",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (currentTab == 0) {
                                IconButton(
                                    onClick = onToggleLayout,
                                    modifier = Modifier
                                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), CircleShape)
                                        .size(40.dp)
                                ) {
                                    Icon(
                                        imageVector = if (uiState.display.layout == LibraryLayout.GRID) Icons.AutoMirrored.Rounded.List else Icons.Rounded.GridView,
                                        contentDescription = "Layout",
                                        modifier = Modifier.size(20.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                                Box {
                                    IconButton(
                                        onClick = { showSortMenu = true },
                                        modifier = Modifier
                                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), CircleShape)
                                            .size(40.dp)
                                    ) {
                                        Icon(Icons.AutoMirrored.Rounded.Sort, null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
                                    }
                                    DropdownMenu(
                                        expanded = showSortMenu,
                                        onDismissRequest = { showSortMenu = false },
                                        shape = RoundedCornerShape(16.dp),
                                        containerColor = MaterialTheme.colorScheme.surface,
                                        tonalElevation = 4.dp
                                    ) {
                                        SortOrder.entries.forEach { order ->
                                            DropdownMenuItem(
                                                text = { Text(order.name.lowercase().replace("_", " ").replaceFirstChar { it.uppercase() }) },
                                                onClick = { onSortOrderChanged(order); showSortMenu = false },
                                                trailingIcon = { if (uiState.sortOrder == order) Icon(Icons.Rounded.Check, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary) },
                                                colors = MenuDefaults.itemColors(
                                                    textColor = MaterialTheme.colorScheme.onSurface,
                                                    leadingIconColor = MaterialTheme.colorScheme.primary
                                                )
                                            )
                                        }
                                    }
                                }
                            } else if (currentTab == 4) {
                                IconButton(
                                    onClick = { Logger.clear() },
                                    modifier = Modifier
                                        .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f), CircleShape)
                                        .size(40.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Delete, 
                                        contentDescription = "Clear Logs",
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // Main Content
                Box(modifier = Modifier.weight(1f)) {
                    when (currentTab) {
                        1 -> BrowseCatalogScreen(
                            modifier = Modifier.fillMaxSize(),
                            liquidGlassEnabled = liquidGlassEnabled
                        )
                        2 -> SettingsArea(
                            modifier = Modifier.fillMaxSize(),
                            uiState = uiState,
                            appTheme = appTheme,
                            navBarStyle = navBarStyle,
                            liquidGlassEnabled = liquidGlassEnabled,
                            events = SettingsEvents(
                                onAppThemeChange = onAppThemeChange,
                                onLiquidGlassToggle = onLiquidGlassToggle,
                                onNavigationBarStyleChange = onNavigationBarStyleChange,
                                onAnimationsToggle = onAnimationsToggle,
                                onShowBookTypeChanged = onShowBookTypeChanged,
                                onShowRecentReadingChanged = onShowRecentReadingChanged,
                                onShowFavoritesChanged = onShowFavoritesChanged,
                                onShowGenresChanged = onShowGenresChanged,
                                onHideStatusBarChanged = onHideStatusBarChanged,
                                onGridColumnsChanged = onGridColumnsChanged,
                                onSelectFolder = { treePickerLauncher.launch(null) },
                                onRevokeAccess = onRevokeLibraryAccess,
                                onShowLogs = { currentTab = 4 },
                                onShowCloudBackup = { currentTab = 3 },
                                onExportSettings = onExportSettings,
                                onImportSettings = onImportSettings,
                                onToggleReaderSearch = onToggleReaderSearch,
                                onToggleReaderTTS = onToggleReaderTTS,
                                onToggleReaderAccessibility = onToggleReaderAccessibility,
                                onToggleReaderAnalytics = onToggleReaderAnalytics,
                                onToggleReaderExport = onToggleReaderExport,
                                onReaderControlOrderChange = onReaderControlOrderChanged
                            )
                        )
                        3 -> CloudSyncSettings(
                            modifier = Modifier.fillMaxSize(),
                            liquidGlassEnabled = liquidGlassEnabled
                        )
                        4 -> {
                            Column(Modifier.fillMaxSize()) {
                                Row(
                                    Modifier.padding(horizontal = 20.dp, vertical = 8.dp), 
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    IconButton(
                                        onClick = { currentTab = 2 },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            Icons.AutoMirrored.Rounded.ArrowBack, 
                                            "Back",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    Text(
                                        "System Logs", 
                                        style = MaterialTheme.typography.titleMedium, 
                                        fontWeight = FontWeight.Bold, 
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                LogsArea(modifier = Modifier.fillMaxSize().padding(bottom = 80.dp, start = 20.dp, end = 20.dp))
                            }
                        }
                        else -> {
                            LibraryContent(
                                uiState = uiState,
                                appTheme = appTheme,
                                liquidGlassEnabled = liquidGlassEnabled,
                                treePickerLauncher = treePickerLauncher,
                                onSearchChanged = onSearchChanged,
                                onToggleFavorite = onToggleFavorite,
                                onOpenBook = onOpenBook,
                                onRefresh = onRefresh,
                                onShowFilter = { showFilterSheet = true },
                                onToggleTypeFilter = onToggleTypeFilter,
                                onToggleGenreFilter = onToggleGenreFilter,
                                onClearAdvancedFilters = onClearAdvancedFilters,
                                onShowBookInfo = { selectedBookForInfo = it }
                            )
                        }
                    }
                }
            }
        }

        // Custom Floating Navigation Bar
        if (navBarStyle == NavigationBarStyle.FLOATING) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 60.dp, vertical = 24.dp)
                    .navigationBarsPadding(),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    modifier = Modifier
                        .height(64.dp)
                        .fillMaxWidth()
                        .shadow(12.dp, RoundedCornerShape(32.dp)),
                    shape = RoundedCornerShape(32.dp),
                    color = if (liquidGlassEnabled) MaterialTheme.colorScheme.surface.copy(alpha = 0.85f) else MaterialTheme.colorScheme.surface,
                    tonalElevation = 8.dp,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.15f))
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        CustomNavItem(
                            selected = currentTab == 0,
                            onClick = { currentTab = 0 },
                            icon = Icons.AutoMirrored.Rounded.MenuBook,
                            label = "Library",
                            animationsEnabled = uiState.display.animationsEnabled
                        )
                        CustomNavItem(
                            selected = currentTab == 1,
                            onClick = { currentTab = 1 },
                            icon = Icons.Rounded.Explore,
                            label = "Browse",
                            animationsEnabled = uiState.display.animationsEnabled
                        )
                        CustomNavItem(
                            selected = currentTab == 2,
                            onClick = { currentTab = 2 },
                            icon = Icons.Rounded.Settings,
                            label = "Settings",
                            animationsEnabled = uiState.display.animationsEnabled
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LibraryContent(
    uiState: HomeUiState,
    appTheme: AppTheme,
    liquidGlassEnabled: Boolean,
    treePickerLauncher: androidx.activity.result.ActivityResultLauncher<Uri?>,
    onSearchChanged: (String) -> Unit,
    onToggleFavorite: (String, Boolean) -> Unit,
    onOpenBook: (BookItem) -> Unit,
    onRefresh: () -> Unit,
    onShowFilter: () -> Unit,
    onToggleTypeFilter: (BookType) -> Unit,
    onToggleGenreFilter: (String) -> Unit,
    onClearAdvancedFilters: () -> Unit,
    onShowBookInfo: (BookItem) -> Unit
) {
    val favoriteBooks = remember(uiState.allBooks) {
        uiState.allBooks.filter { it.isFavorite }
    }

    if (!uiState.hasLibraryAccess) {
        EmptyPermissionState { treePickerLauncher.launch(null) }
    } else {
        val refreshState = rememberPullToRefreshState()
        PullToRefreshBox(state = refreshState, isRefreshing = uiState.isScanning, onRefresh = onRefresh) {
            if (uiState.allBooks.isEmpty() && !uiState.isScanning) {
                EmptyLibraryState(onRefresh, { treePickerLauncher.launch(null) })
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 100.dp)) {
                    // Search Bar & Filter Chips
                    item {
                        Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                OutlinedTextField(
                                    value = uiState.searchQuery, onValueChange = onSearchChanged, placeholder = { Text("Search books...") },
                                    leadingIcon = { Icon(Icons.Rounded.Search, null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary) },
                                    modifier = Modifier.weight(1f).height(52.dp), shape = CircleShape, singleLine = true
                                )
                                Surface(
                                    onClick = onShowFilter, 
                                    color = if (uiState.selectedTypes.isNotEmpty() || uiState.selectedGenres.isNotEmpty()) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), 
                                    shape = CircleShape, 
                                    modifier = Modifier.size(52.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) { Icon(Icons.Rounded.FilterAlt, "Filter", modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary) }
                                }
                            }
                            
                            val hasFilters = uiState.selectedGenres.isNotEmpty() || uiState.selectedTypes.isNotEmpty() || uiState.searchQuery.isNotEmpty()
                            if (hasFilters) {
                                Spacer(Modifier.height(10.dp))
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                    items(uiState.selectedGenres.toList()) { genre ->
                                        FilterChip(
                                            selected = true,
                                            onClick = { onToggleGenreFilter(genre) },
                                            label = { Text(genre) },
                                            trailingIcon = { Icon(Icons.Rounded.Close, null, Modifier.size(16.dp)) },
                                            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = MaterialTheme.colorScheme.primaryContainer)
                                        )
                                    }
                                    items(uiState.selectedTypes.toList()) { type ->
                                        FilterChip(
                                            selected = true,
                                            onClick = { onToggleTypeFilter(type) },
                                            label = { Text(type.name) },
                                            trailingIcon = { Icon(Icons.Rounded.Close, null, Modifier.size(16.dp)) }
                                        )
                                    }
                                    if (uiState.searchQuery.isNotEmpty()) {
                                        item {
                                            FilterChip(
                                                selected = true,
                                                onClick = { onSearchChanged("") },
                                                label = { Text("Query: ${uiState.searchQuery}") },
                                                trailingIcon = { Icon(Icons.Rounded.Close, null, Modifier.size(16.dp)) }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Continue Reading
                    if (uiState.searchQuery.isEmpty() && uiState.display.showRecentReading && uiState.recentBooks.isNotEmpty() && uiState.selectedTypes.isEmpty() && uiState.selectedGenres.isEmpty()) {
                        item {
                            Text("Continue Reading", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold, modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp), color = MaterialTheme.colorScheme.onSurface)
                            LazyRow(contentPadding = PaddingValues(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                items(uiState.recentBooks) { book -> RecentBookCard(book, { onOpenBook(book) }, Modifier.width(130.dp)) }
                            }
                            Spacer(Modifier.height(8.dp))
                        }
                    }

                    // Genres
                    if (uiState.searchQuery.isEmpty() && uiState.display.showGenres && uiState.availableGenres.isNotEmpty() && uiState.selectedTypes.isEmpty() && uiState.selectedGenres.isEmpty()) {
                        item {
                            Text("Genres", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold, modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp), color = MaterialTheme.colorScheme.onSurface)
                            LazyRow(contentPadding = PaddingValues(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(uiState.availableGenres) { genre ->
                                    Surface(
                                        onClick = { onToggleGenreFilter(genre) },
                                        shape = RoundedCornerShape(12.dp),
                                        color = if (uiState.selectedGenres.contains(genre)) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                        modifier = Modifier.padding(vertical = 4.dp),
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.1f))
                                    ) {
                                        Text(text = genre, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurface)
                                    }
                                }
                            }
                            Spacer(Modifier.height(8.dp))
                        }
                    }

                    // Favorites Section
                    if (uiState.searchQuery.isEmpty() && uiState.display.showFavorites && favoriteBooks.isNotEmpty() && uiState.selectedTypes.isEmpty() && uiState.selectedGenres.isEmpty()) {
                        item {
                            Text("Favorites", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold, modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp), color = MaterialTheme.colorScheme.onSurface)
                            if (uiState.display.layout == LibraryLayout.GRID) {
                                Column(modifier = Modifier.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                    val columns = uiState.display.gridColumns
                                    favoriteBooks.chunked(columns).forEach { row ->
                                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                            row.forEach { book ->
                                                Box(Modifier.weight(1f)) {
                                                    BookCard(book, { onOpenBook(book) }, onToggleFavorite, onShowBookInfo, uiState.display.showBookType, uiState.display.showFavorites, false, columns, appTheme, liquidGlassEnabled)
                                                }
                                            }
                                            repeat(columns - row.size) { Spacer(Modifier.weight(1f)) }
                                        }
                                    }
                                }
                            } else {
                                Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.padding(horizontal = 20.dp)) {
                                    favoriteBooks.forEach { book ->
                                        BookListItem(book, { onOpenBook(book) }, onToggleFavorite, onShowBookInfo, uiState.display.showBookType, uiState.display.showFavorites, false, appTheme, liquidGlassEnabled)
                                    }
                                }
                            }
                            Spacer(Modifier.height(16.dp))
                        }
                    }

                    // Main Books Section
                    item {
                        if (uiState.searchQuery.isEmpty() && uiState.selectedTypes.isEmpty() && uiState.selectedGenres.isEmpty()) {
                            Text("All Books", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold, modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp), color = MaterialTheme.colorScheme.onSurface)
                        }
                    }

                    if (uiState.display.layout == LibraryLayout.GRID) {
                        item {
                            val columns = uiState.display.gridColumns
                            val books = uiState.visibleBooks
                            val showAddButton = uiState.searchQuery.isEmpty() && uiState.selectedTypes.isEmpty() && uiState.selectedGenres.isEmpty()
                            
                            val allItems = if (showAddButton) books.map { it as Any? } + listOf(null) else books.map { it as Any? }
                            
                            Column(modifier = Modifier.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                allItems.chunked(columns).forEach { row ->
                                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                        row.forEach { item ->
                                            Box(modifier = Modifier.weight(1f)) {
                                                if (item == null) {
                                                    AddBookCard(appTheme, liquidGlassEnabled) { treePickerLauncher.launch(null) }
                                                } else {
                                                    val book = item as BookItem
                                                    BookCard(book, { onOpenBook(book) }, onToggleFavorite, onShowBookInfo, uiState.display.showBookType, uiState.display.showFavorites, false, columns, appTheme, liquidGlassEnabled)
                                                }
                                            }
                                        }
                                        repeat(columns - row.size) { Spacer(Modifier.weight(1f)) }
                                    }
                                }
                            }
                        }
                    } else {
                        items(uiState.visibleBooks) { book ->
                            BookListItem(
                                book = book,
                                onClick = { onOpenBook(book) },
                                onToggleFavorite = onToggleFavorite,
                                onShowInfo = onShowBookInfo,
                                showBookType = uiState.display.showBookType,
                                showFavoriteButton = uiState.display.showFavorites,
                                showProgress = false,
                                appTheme = appTheme,
                                liquidGlassEnabled = liquidGlassEnabled,
                                modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
                            )
                        }
                        if (uiState.searchQuery.isEmpty() && uiState.selectedTypes.isEmpty() && uiState.selectedGenres.isEmpty()) {
                            item { AddBookListItem(appTheme = appTheme, liquidGlassEnabled = liquidGlassEnabled, modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)) { treePickerLauncher.launch(null) } }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CustomNavItem(
    selected: Boolean,
    onClick: () -> Unit,
    icon: ImageVector,
    label: String,
    animationsEnabled: Boolean = true
) {
    val alpha by animateFloatAsState(if (selected) 1f else 0.5f)
    val scale by animateFloatAsState(if (selected && animationsEnabled) 1.15f else 1f)
    Column(
        modifier = Modifier.clickable(interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }, indication = null, onClick = onClick).padding(horizontal = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(imageVector = icon, contentDescription = label, tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(22.dp).graphicsLayer(scaleX = scale, scaleY = scale).alpha(alpha))
        Spacer(Modifier.height(2.dp))
        Text(text = label, style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp), color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium, modifier = Modifier.alpha(alpha))
    }
}

@Composable
private fun EmptyPermissionState(onGrantAccess: () -> Unit) {
    Box(Modifier.fillMaxSize().padding(40.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Icon(Icons.Rounded.FolderOpen, null, Modifier.size(64.dp), tint = MaterialTheme.colorScheme.primary)
            Text("Access Required", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface)
            Button(onClick = onGrantAccess) { Text("Select Library Folder") }
        }
    }
}

@Composable
private fun EmptyLibraryState(onRefresh: () -> Unit, onChangeFolder: () -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("No books found", color = MaterialTheme.colorScheme.onSurface)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onRefresh) { Text("Scan Again") }
                OutlinedButton(onClick = onChangeFolder) { Text("Change Folder") }
            }
        }
    }
}

@Composable
private fun LogsArea(modifier: Modifier = Modifier) {
    val logs by Logger.logs.collectAsState()
    LazyColumn(
        modifier = modifier
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
            .padding(8.dp)
    ) {
        items(logs) { log -> 
            Surface(
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.padding(vertical = 4.dp).fillMaxWidth(),
                border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
            ) {
                Text(
                    text = log, 
                    style = MaterialTheme.typography.bodySmall, 
                    modifier = Modifier.padding(12.dp), 
                    color = MaterialTheme.colorScheme.onSurface
                ) 
            }
        }
    }
}

@Composable
private fun RecentBookCard(book: BookItem, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val coverBitmap = remember(book.id) { book.coverImage?.let { BitmapFactory.decodeByteArray(it, 0, it.size)?.asImageBitmap() } }
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Card(onClick = onClick, modifier = modifier.aspectRatio(0.72f), shape = RoundedCornerShape(12.dp)) {
            Box(modifier = Modifier.fillMaxSize()) {
                if (coverBitmap != null) {
                    Image(coverBitmap, null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                } else {
                    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surfaceVariant), contentAlignment = Alignment.Center) {
                        Icon(Icons.AutoMirrored.Rounded.MenuBook, null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                    }
                }
                
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f))))
                        .padding(8.dp)
                ) {
                    LinearProgressIndicator(
                        progress = { book.progress.coerceIn(0f, 1f) },
                        modifier = Modifier.fillMaxWidth().height(4.dp).clip(CircleShape),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = Color.White.copy(alpha = 0.3f)
                    )
                    Text(
                        text = "${(book.progress * 100).toInt()}%",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.align(Alignment.End).padding(top = 2.dp)
                    )
                }
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(
            text = book.title,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.width(130.dp).padding(horizontal = 4.dp),
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BookDetailsDialog(book: BookItem, onDismiss: () -> Unit, onRead: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { Button(onClick = onRead, shape = RoundedCornerShape(12.dp)) { Text("Read Now") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Close") } },
        title = { Text(book.title, fontWeight = FontWeight.ExtraBold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Author: ${book.author}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                if (!book.description.isNullOrBlank()) {
                    Text(book.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                DetailRow(Icons.Rounded.Fingerprint, "ISBN", book.isbn ?: "Unknown")
                DetailRow(Icons.Rounded.Business, "Publisher", book.publisher ?: "Unknown")
                DetailRow(Icons.Rounded.Event, "Published", book.publishedDate ?: "Unknown")
                DetailRow(Icons.Rounded.Language, "Language", book.language ?: "Unknown")
            }
        },
        shape = RoundedCornerShape(28.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 6.dp
    )
}

@Composable
private fun DetailRow(icon: ImageVector, label: String, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Icon(icon, null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
        Text("$label: ", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterBottomSheet(uiState: HomeUiState, onDismiss: () -> Unit, onToggleType: (BookType) -> Unit, onToggleGenre: (String) -> Unit, onReset: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = MaterialTheme.colorScheme.surface, tonalElevation = 8.dp) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(24.dp)) {
            Text("Filter Library", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Formats", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val formats = listOf(BookType.EPUB, BookType.PDF)
                    formats.forEach { type ->
                        FilterChip(
                            selected = uiState.selectedTypes.contains(type),
                            onClick = { onToggleType(type) },
                            label = { Text(type.name) }
                        )
                    }
                }
            }
            
            Button(onClick = { onReset(); onDismiss() }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) { Text("Reset All Filters") }
            Spacer(Modifier.height(24.dp))
        }
    }
}
