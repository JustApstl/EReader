package com.dyu.ereader.ui.components

import android.graphics.BitmapFactory
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dyu.ereader.data.model.AppTheme
import com.dyu.ereader.data.model.BookItem

@Composable
fun BookCard(
    book: BookItem,
    onClick: () -> Unit,
    onToggleFavorite: (String, Boolean) -> Unit,
    onShowInfo: (BookItem) -> Unit,
    showBookType: Boolean = true,
    showFavoriteButton: Boolean = true,
    showProgress: Boolean = false,
    gridColumns: Int = 3,
    appTheme: AppTheme = AppTheme.SYSTEM,
    liquidGlassEnabled: Boolean = false,
    modifier: Modifier = Modifier
) {
    val coverBitmap = remember(book.id) {
        book.coverImage?.let {
            BitmapFactory.decodeByteArray(it, 0, it.size)?.asImageBitmap()
        }
    }
    var showMenu by remember { mutableStateOf(false) }

    val isDark = when (appTheme) {
        AppTheme.DARK, AppTheme.BLACK -> true
        AppTheme.LIGHT -> false
        AppTheme.SYSTEM -> isSystemInDarkTheme()
    }

    val cardBgColor = if (liquidGlassEnabled) {
        if (isDark) MaterialTheme.colorScheme.surface.copy(alpha = 0.65f)
        else Color.White.copy(alpha = 0.7f)
    } else MaterialTheme.colorScheme.surface

    val borderColor = if (liquidGlassEnabled) {
        if (isDark) Color.White.copy(alpha = 0.2f)
        else MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)
    } else Color.Transparent

    val borderStroke = if (liquidGlassEnabled) BorderStroke(1.dp, borderColor) else null

    val titleFontSize = when(gridColumns) {
        2 -> 14.sp
        3 -> 12.sp
        4 -> 10.sp
        else -> 12.sp
    }
    val authorFontSize = when(gridColumns) {
        2 -> 12.sp
        3 -> 10.sp
        4 -> 8.sp
        else -> 10.sp
    }

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .padding(bottom = 4.dp)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(0.72f)
                .shadow(
                    elevation = if (liquidGlassEnabled) 2.dp else 4.dp,
                    shape = RoundedCornerShape(16.dp),
                    clip = false
                ),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = cardBgColor),
            border = borderStroke
        ) {
            Box(modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(16.dp))) {
                if (coverBitmap != null) {
                    Image(
                        bitmap = coverBitmap,
                        contentDescription = "${book.title} cover",
                        modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(16.dp)),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(Modifier.fillMaxSize().clip(RoundedCornerShape(16.dp)), contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.MenuBook,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            modifier = Modifier.size(if (gridColumns <= 2) 48.dp else 32.dp)
                        )
                    }
                }
                
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .align(Alignment.BottomCenter)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.45f))
                            )
                        )
                )

                if (showBookType) {
                    Surface(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(8.dp),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.85f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = book.type.name,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Black
                            ),
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp)
                ) {
                    IconButton(
                        onClick = { showMenu = true },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Surface(
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                            shape = CircleShape,
                            modifier = Modifier.size(28.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Rounded.MoreVert,
                                    contentDescription = "Options",
                                    modifier = Modifier.size(18.dp),
                                    tint = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false },
                        shape = RoundedCornerShape(12.dp),
                        containerColor = MaterialTheme.colorScheme.surface,
                        tonalElevation = 4.dp
                    ) {
                        DropdownMenuItem(
                            text = { Text("Details", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold) },
                            onClick = {
                                showMenu = false
                                onShowInfo(book)
                            },
                            leadingIcon = { Icon(Icons.Rounded.Info, contentDescription = null, modifier = Modifier.size(20.dp)) },
                            colors = MenuDefaults.itemColors(
                                textColor = MaterialTheme.colorScheme.onSurface,
                                leadingIconColor = MaterialTheme.colorScheme.primary
                            )
                        )
                        DropdownMenuItem(
                            text = { Text(if (book.isFavorite) "Unfavorite" else "Favorite", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold) },
                            onClick = {
                                showMenu = false
                                onToggleFavorite(book.id, !book.isFavorite)
                            },
                            leadingIcon = { 
                                Icon(
                                    imageVector = if (book.isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder, 
                                    contentDescription = null, 
                                    modifier = Modifier.size(20.dp),
                                    tint = if (book.isFavorite) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                                ) 
                            },
                            colors = MenuDefaults.itemColors(
                                textColor = MaterialTheme.colorScheme.onSurface,
                                leadingIconColor = if (book.isFavorite) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                            )
                        )
                    }
                }

                if (showFavoriteButton) {
                    FavoriteButton(
                        isFavorite = book.isFavorite,
                        onToggle = { onToggleFavorite(book.id, !book.isFavorite) },
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(8.dp),
                        isLiquidGlass = liquidGlassEnabled
                    )
                }

                if (showProgress && book.progress > 0) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.BottomCenter)
                    ) {
                        Surface(
                            modifier = Modifier
                                .align(Alignment.End)
                                .padding(end = 8.dp, bottom = 4.dp),
                            color = Color.Black.copy(alpha = 0.6f),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = "${(book.progress * 100).toInt()}%",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp, fontWeight = FontWeight.Bold),
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                        }
                        LinearProgressIndicator(
                            progress = { book.progress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(4.dp)
                                .clip(RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp)),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = Color.Transparent
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = book.title,
            style = MaterialTheme.typography.titleSmall.copy(
                fontSize = titleFontSize, 
                lineHeight = titleFontSize * 1.3,
                fontWeight = FontWeight.Bold
            ),
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 4.dp)
        )
        Text(
            text = book.author,
            style = MaterialTheme.typography.bodySmall.copy(
                fontSize = authorFontSize,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 4.dp)
        )
    }
}

@Composable
fun BookListItem(
    book: BookItem,
    onClick: () -> Unit,
    onToggleFavorite: (String, Boolean) -> Unit,
    onShowInfo: (BookItem) -> Unit,
    showBookType: Boolean = true,
    showFavoriteButton: Boolean = true,
    showProgress: Boolean = false,
    appTheme: AppTheme = AppTheme.SYSTEM,
    liquidGlassEnabled: Boolean = false,
    modifier: Modifier = Modifier
) {
    val coverBitmap = remember(book.id) {
        book.coverImage?.let {
            BitmapFactory.decodeByteArray(it, 0, it.size)?.asImageBitmap()
        }
    }
    var showMenu by remember { mutableStateOf(false) }
    
    val isDark = when (appTheme) {
        AppTheme.DARK, AppTheme.BLACK -> true
        AppTheme.LIGHT -> false
        AppTheme.SYSTEM -> isSystemInDarkTheme()
    }

    val bgColor = if (liquidGlassEnabled) {
        if (isDark) MaterialTheme.colorScheme.surface.copy(alpha = 0.55f)
        else Color.White.copy(alpha = 0.65f)
    } else MaterialTheme.colorScheme.surface

    val borderColor = if (liquidGlassEnabled) {
        if (isDark) Color.White.copy(alpha = 0.15f)
        else MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
    } else Color.Transparent

    val borderStroke = if (liquidGlassEnabled) BorderStroke(1.dp, borderColor) else null

    Surface(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = bgColor,
        border = borderStroke,
        shadowElevation = if (liquidGlassEnabled) 1.dp else 1.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier
                    .size(width = 64.dp, height = 88.dp)
                    .shadow(4.dp, RoundedCornerShape(8.dp)),
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {
                Box(modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(8.dp)), contentAlignment = Alignment.Center) {
                    if (coverBitmap != null) {
                        Image(
                            bitmap = coverBitmap,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(8.dp)),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Icon(Icons.AutoMirrored.Rounded.MenuBook, contentDescription = null, modifier = Modifier.size(24.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                    }
                    
                    if (showProgress && book.progress > 0) {
                        LinearProgressIndicator(
                            progress = { book.progress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(4.dp)
                                .align(Alignment.BottomCenter)
                                .clip(RoundedCornerShape(bottomStart = 8.dp, bottomEnd = 8.dp)),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = Color.Transparent
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = book.title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, lineHeight = 20.sp),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = book.author,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                Row(
                    modifier = Modifier.padding(top = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (showBookType) {
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = book.type.name,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Black),
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                    
                    if (showProgress && book.progress > 0) {
                        Text(
                            text = "${(book.progress * 100).toInt()}% read",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (showFavoriteButton) {
                    FavoriteButton(
                        isFavorite = book.isFavorite,
                        onToggle = { onToggleFavorite(book.id, !book.isFavorite) },
                        modifier = Modifier.padding(horizontal = 4.dp),
                        isLiquidGlass = liquidGlassEnabled
                    )
                }

                Box {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(Icons.Rounded.MoreVert, contentDescription = "More", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false },
                        shape = RoundedCornerShape(12.dp),
                        containerColor = MaterialTheme.colorScheme.surface,
                        tonalElevation = 4.dp
                    ) {
                        DropdownMenuItem(
                            text = { Text("Details", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold) },
                            onClick = {
                                showMenu = false
                                onShowInfo(book)
                            },
                            leadingIcon = { Icon(Icons.Rounded.Info, contentDescription = null, modifier = Modifier.size(20.dp)) },
                            colors = MenuDefaults.itemColors(
                                textColor = MaterialTheme.colorScheme.onSurface,
                                leadingIconColor = MaterialTheme.colorScheme.primary
                            )
                        )
                        DropdownMenuItem(
                            text = { Text(if (book.isFavorite) "Unfavorite" else "Favorite", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold) },
                            onClick = {
                                showMenu = false
                                onToggleFavorite(book.id, !book.isFavorite)
                            },
                            leadingIcon = { 
                                Icon(
                                    imageVector = if (book.isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder, 
                                    contentDescription = null, 
                                    modifier = Modifier.size(20.dp),
                                    tint = if (book.isFavorite) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                                ) 
                            },
                            colors = MenuDefaults.itemColors(
                                textColor = MaterialTheme.colorScheme.onSurface,
                                leadingIconColor = if (book.isFavorite) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                            )
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AddBookCard(
    appTheme: AppTheme = AppTheme.SYSTEM,
    liquidGlassEnabled: Boolean = false,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val isDark = when (appTheme) {
        AppTheme.DARK, AppTheme.BLACK -> true
        AppTheme.LIGHT -> false
        AppTheme.SYSTEM -> isSystemInDarkTheme()
    }

    val bgColor = if (liquidGlassEnabled) {
        if (isDark) MaterialTheme.colorScheme.surface.copy(alpha = 0.45f)
        else Color.White.copy(alpha = 0.55f)
    } else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)

    val borderColor = if (liquidGlassEnabled) {
        if (isDark) Color.White.copy(alpha = 0.25f)
        else MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
    } else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)

    val borderStroke = BorderStroke(1.2.dp, borderColor)

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .padding(bottom = 4.dp)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(0.72f)
                .shadow(
                    elevation = if (liquidGlassEnabled) 1.dp else 0.dp,
                    shape = RoundedCornerShape(16.dp),
                    clip = false
                ),
            shape = RoundedCornerShape(16.dp),
            color = bgColor,
            border = borderStroke
        ) {
            Box(contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                        modifier = Modifier.size(48.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Rounded.Add,
                                contentDescription = null,
                                modifier = Modifier.size(28.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Add Book",
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
fun AddBookListItem(
    appTheme: AppTheme = AppTheme.SYSTEM,
    liquidGlassEnabled: Boolean = false,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val isDark = when (appTheme) {
        AppTheme.DARK, AppTheme.BLACK -> true
        AppTheme.LIGHT -> false
        AppTheme.SYSTEM -> isSystemInDarkTheme()
    }

    val bgColor = if (liquidGlassEnabled) {
        if (isDark) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
        else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
    } else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)

    Surface(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = bgColor,
        border = BorderStroke(1.2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.Add,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "Import more books",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun FavoriteButton(
    isFavorite: Boolean,
    onToggle: () -> Unit,
    isLiquidGlass: Boolean = false,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .size(32.dp)
            .clip(CircleShape)
            .clickable { onToggle() },
        color = if (isFavorite) {
            MaterialTheme.colorScheme.errorContainer.copy(alpha = if (isLiquidGlass) 0.75f else 0.9f)
        } else {
            Color.Black.copy(alpha = 0.25f)
        },
        shape = CircleShape,
        border = if (isLiquidGlass) BorderStroke(0.8.dp, Color.White.copy(alpha = 0.4f)) else null
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = if (isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                contentDescription = "Favorite",
                tint = if (isFavorite) MaterialTheme.colorScheme.error else Color.White,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}
