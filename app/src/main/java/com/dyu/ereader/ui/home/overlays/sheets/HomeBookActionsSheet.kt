package com.dyu.ereader.ui.home.overlays.sheets

import android.graphics.BitmapFactory
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.BookmarkBorder
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.CollectionsBookmark
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.FileOpen
import androidx.compose.material.icons.rounded.FolderCopy
import androidx.compose.material.icons.rounded.IosShare
import androidx.compose.material.icons.rounded.LibraryBooks
import androidx.compose.material.icons.rounded.NoteAlt
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.dyu.ereader.data.model.library.BookCollectionShelf
import com.dyu.ereader.data.model.library.BookItem
import com.dyu.ereader.ui.app.theme.UiTokens
import com.dyu.ereader.ui.components.buttons.AppChromeIconButton
import com.dyu.ereader.ui.components.dialogs.appDialogTextFieldColors
import com.dyu.ereader.ui.components.dialogs.appTextFieldShape
import com.dyu.ereader.ui.components.books.BookMetadataChip
import com.dyu.ereader.ui.components.books.BookPrimaryActionButton
import com.dyu.ereader.ui.components.surfaces.LiquidGlassOverlay
import com.dyu.ereader.ui.components.surfaces.SectionSurface
import com.dyu.ereader.ui.components.surfaces.rememberLiquidGlassStyle
import androidx.compose.ui.window.Dialog

@Composable
fun HomeBookActionsSheet(
    book: BookItem?,
    collections: List<BookCollectionShelf>,
    liquidGlassEnabled: Boolean,
    onDismiss: () -> Unit,
    onRead: (BookItem) -> Unit,
    onShowInfo: (BookItem) -> Unit,
    onToggleFavorite: (BookItem) -> Unit,
    onShare: (BookItem) -> Unit,
    onExportHighlights: (BookItem) -> Unit,
    onOpenFile: (BookItem) -> Unit,
    onDelete: (BookItem) -> Unit,
    onCreateCollection: (String, BookItem) -> Unit,
    onToggleCollection: (String, BookItem) -> Unit,
    onDeleteCollection: (String) -> Unit
) {
    val selectedBook = book ?: return
    var newCollectionName by remember(selectedBook.id) { mutableStateOf("") }
    val coverBitmap = remember(selectedBook.id) {
        selectedBook.coverImage?.let { BitmapFactory.decodeByteArray(it, 0, it.size)?.asImageBitmap() }
    }
    val matchingCollections = remember(collections, selectedBook.id) {
        collections.filter { shelf -> shelf.books.any { it.id == selectedBook.id } }.map { it.name }.toSet()
    }
    val glassStyle = rememberLiquidGlassStyle(strong = true)
    val sectionColor = if (liquidGlassEnabled) {
        glassStyle.containerColor
    } else {
        MaterialTheme.colorScheme.surfaceContainerLow
    }
    val sectionBorder = if (liquidGlassEnabled) {
        glassStyle.border
    } else {
        androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
    }
    val readingStatus = remember(selectedBook.progress) {
        when {
            selectedBook.progress >= 0.98f -> "Finished"
            selectedBook.progress > 0f -> "${(selectedBook.progress * 100).toInt()}% read"
            else -> "Unread"
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 720.dp),
            shape = RoundedCornerShape(32.dp),
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            tonalElevation = 0.dp,
            shadowElevation = 16.dp,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.22f))
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 20.dp),
                contentPadding = PaddingValues(bottom = 8.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
            item {
                SectionSurface(
                    shape = UiTokens.SettingsCardShape,
                    color = sectionColor,
                    border = sectionBorder,
                    tonalElevation = 0.dp,
                    shadowElevation = 0.dp,
                    contentPadding = PaddingValues(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                            Surface(
                                modifier = Modifier
                                    .size(width = 88.dp, height = 128.dp),
                            shape = RoundedCornerShape(18.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerHigh,
                            border = if (liquidGlassEnabled) glassStyle.secondaryBorder else androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
                        ) {
                            Box {
                                if (coverBitmap != null) {
                                    androidx.compose.foundation.Image(
                                        bitmap = coverBitmap,
                                        contentDescription = null,
                                        modifier = Modifier.fillMaxWidth().height(128.dp),
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
                                    Box(
                                        modifier = Modifier.fillMaxWidth().height(128.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Rounded.MenuBook,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                            modifier = Modifier.size(32.dp)
                                        )
                                    }
                                }
                                if (liquidGlassEnabled) {
                                    LiquidGlassOverlay(
                                        shape = RoundedCornerShape(18.dp),
                                        intensity = if (coverBitmap != null) 0.45f else 0.9f
                                    )
                                }
                            }
                        }

                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Top
                            ) {
                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = selectedBook.title,
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = selectedBook.author,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                AppChromeIconButton(
                                    icon = Icons.Rounded.Close,
                                    contentDescription = "Close",
                                    onClick = onDismiss
                                )
                            }

                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                BookMetadataChip(label = selectedBook.type.label)
                                BookMetadataChip(label = readingStatus)
                                if (selectedBook.isFavorite) {
                                    BookMetadataChip(label = "Favorite")
                                }
                            }

                            Text(
                                text = selectedBook.fileName,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            item {
                SectionSurface(
                    shape = UiTokens.SettingsCardShape,
                    color = sectionColor,
                    border = sectionBorder,
                    tonalElevation = 0.dp,
                    shadowElevation = 0.dp,
                    contentPadding = PaddingValues(14.dp)
                ) {
                    Text(
                        text = "Book Actions",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "Keep the next steps close: open, manage, share, or file this book away.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        BookPrimaryActionButton(
                            label = "Read Now",
                            onClick = {
                                onRead(selectedBook)
                                onDismiss()
                            },
                            modifier = Modifier.weight(1f)
                        )
                        BookQuickActionButton(
                            label = "About",
                            icon = Icons.Rounded.Description,
                            onClick = {
                                onShowInfo(selectedBook)
                                onDismiss()
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        BookQuickActionButton(
                            label = if (selectedBook.isFavorite) "Favorited" else "Favorite",
                            icon = if (selectedBook.isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                            onClick = {
                                onToggleFavorite(selectedBook)
                                onDismiss()
                            },
                            modifier = Modifier.weight(1f)
                        )
                        BookQuickActionButton(
                            label = "Share",
                            icon = Icons.Rounded.IosShare,
                            onClick = {
                                onShare(selectedBook)
                                onDismiss()
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        BookQuickActionButton(
                            label = "Open File",
                            icon = Icons.Rounded.FileOpen,
                            onClick = {
                                onOpenFile(selectedBook)
                                onDismiss()
                            },
                            modifier = Modifier.weight(1f)
                        )
                        BookQuickActionButton(
                            label = "Highlights",
                            icon = Icons.Rounded.NoteAlt,
                            onClick = {
                                onExportHighlights(selectedBook)
                                onDismiss()
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            item {
                SectionSurface(
                    shape = UiTokens.SettingsCardShape,
                    color = sectionColor,
                    border = sectionBorder,
                    tonalElevation = 0.dp,
                    shadowElevation = 0.dp,
                    contentPadding = PaddingValues(14.dp)
                ) {
                    Text(
                        text = "Shelves & Collections",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(Modifier.height(6.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CollectionInfoChip(
                            label = if (matchingCollections.isEmpty()) {
                                "Not saved to a collection"
                            } else {
                                "${matchingCollections.size} collection${if (matchingCollections.size == 1) "" else "s"}"
                            }
                        )
                        if (selectedBook.isFavorite) {
                            CollectionInfoChip(label = "Pinned to favorites")
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    OutlinedTextField(
                        value = newCollectionName,
                        onValueChange = { newCollectionName = it },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        label = { Text("Create collection") },
                        shape = appTextFieldShape(),
                        colors = appDialogTextFieldColors(),
                        trailingIcon = {
                            AppChromeIconButton(
                                icon = Icons.Rounded.Add,
                                contentDescription = "Create collection",
                                onClick = {
                                    val name = newCollectionName.trim()
                                    if (name.isNotBlank()) {
                                        onCreateCollection(name, selectedBook)
                                        newCollectionName = ""
                                    }
                                },
                                size = 32.dp,
                                iconSize = 17.dp
                            )
                        }
                    )
                    Spacer(Modifier.height(12.dp))
                    if (collections.isEmpty()) {
                        Text(
                            text = "Create a custom shelf and we’ll keep this book there too.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            collections.forEach { shelf ->
                                val containsBook = matchingCollections.contains(shelf.name)
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(
                                            if (liquidGlassEnabled) {
                                                glassStyle.iconContainerColor
                                            } else {
                                                MaterialTheme.colorScheme.surfaceContainerHigh
                                            }
                                        )
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(24.dp)
                                            .clip(CircleShape)
                                            .background(
                                                if (containsBook) {
                                                    MaterialTheme.colorScheme.primaryContainer
                                                } else {
                                                    MaterialTheme.colorScheme.surface
                                                }
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = if (containsBook) Icons.Rounded.Check else Icons.Rounded.BookmarkBorder,
                                            contentDescription = null,
                                            modifier = Modifier.size(14.dp),
                                            tint = if (containsBook) {
                                                MaterialTheme.colorScheme.onPrimaryContainer
                                            } else {
                                                MaterialTheme.colorScheme.onSurfaceVariant
                                            }
                                        )
                                    }
                                    Column(
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text(
                                            text = shelf.name,
                                            style = MaterialTheme.typography.labelLarge,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Text(
                                            text = "${shelf.books.size} book${if (shelf.books.size == 1) "" else "s"}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    CollectionActionButton(
                                        label = if (containsBook) "Remove" else "Save",
                                        highlighted = containsBook,
                                        onClick = { onToggleCollection(shelf.name, selectedBook) }
                                    )
                                    AppChromeIconButton(
                                        icon = Icons.Rounded.DeleteOutline,
                                        contentDescription = "Delete collection",
                                        onClick = { onDeleteCollection(shelf.name) },
                                        destructive = true,
                                        size = 32.dp,
                                        iconSize = 17.dp
                                    )
                                }
                            }
                        }
                    }
                }
            }

            item {
                Surface(
                    onClick = {
                        onDelete(selectedBook)
                        onDismiss()
                    },
                    shape = RoundedCornerShape(22.dp),
                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.72f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.24f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.DeleteOutline,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error
                        )
                        Column {
                            Text(
                                text = "Delete Book",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.error
                            )
                            Text(
                                text = "Remove this book from your library and collections.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f)
                            )
                        }
                    }
                }
            }
            }
        }
    }
}

@Composable
private fun BookQuickActionButton(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        border = BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.32f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun CollectionInfoChip(
    label: String
) {
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.78f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.CollectionsBookmark,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(14.dp)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun CollectionActionButton(
    label: String,
    highlighted: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(999.dp),
        color = if (highlighted) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.72f)
        } else {
            MaterialTheme.colorScheme.surface.copy(alpha = 0.82f)
        },
        border = BorderStroke(
            1.dp,
            if (highlighted) {
                MaterialTheme.colorScheme.primary.copy(alpha = 0.22f)
            } else {
                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.22f)
            }
        )
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = if (highlighted) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurface
            }
        )
    }
}
