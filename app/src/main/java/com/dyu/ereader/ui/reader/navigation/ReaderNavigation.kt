package com.dyu.ereader.ui.reader.navigation

import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.dyu.ereader.data.model.library.BookType
import com.dyu.ereader.ui.home.viewmodel.HomeViewModel
import com.dyu.ereader.ui.reader.viewmodel.ReaderViewModel
import com.dyu.ereader.core.codec.decodeNavArg
import com.dyu.ereader.core.codec.encodeNavArg

const val READER_ROUTE = "reader/{bookUriArg}/{bookTypeArg}"
private const val READER_BASE = "reader"

fun NavController.navigateToReader(uri: String, typeName: String) {
    this.navigate("${READER_BASE}/${encodeNavArg(uri)}/${typeName}")
}

fun NavGraphBuilder.readerScreen(
    isDarkTheme: Boolean,
    onBack: () -> Unit,
    onNavigateHome: () -> Unit
) {
    composable(
        route = READER_ROUTE,
        arguments = listOf(
            navArgument("bookUriArg") { type = NavType.StringType },
            navArgument("bookTypeArg") { type = NavType.StringType }
        )
    ) { backStackEntry ->
        val bookArg = backStackEntry.arguments?.getString("bookUriArg") ?: return@composable
        val typeArg = backStackEntry.arguments?.getString("bookTypeArg") ?: BookType.EPUB.name
        val bookUri = decodeNavArg(bookArg)
        val bookType = BookType.valueOf(typeArg)

        val readerViewModel: ReaderViewModel = hiltViewModel(
            key = "reader_${bookArg}"
        )
        val homeViewModel: HomeViewModel = hiltViewModel()

        ReaderRouteContent(
            bookUri = bookUri,
            bookType = bookType,
            isDarkTheme = isDarkTheme,
            onBack = onBack,
            onNavigateHome = onNavigateHome,
            readerViewModel = readerViewModel,
            homeViewModel = homeViewModel
        )
    }
}
