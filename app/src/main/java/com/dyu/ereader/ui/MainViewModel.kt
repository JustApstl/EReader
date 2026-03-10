package com.dyu.ereader.ui

import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dyu.ereader.data.model.AppTheme
import com.dyu.ereader.data.model.BookType
import com.dyu.ereader.data.model.NavigationBarStyle
import com.dyu.ereader.data.storage.ReaderPreferencesStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val preferencesStore: ReaderPreferencesStore
) : ViewModel() {

    private val _theme = MutableStateFlow(AppTheme.LIGHT)
    val theme: StateFlow<AppTheme> = _theme.asStateFlow()

    private val _liquidGlassEnabled = MutableStateFlow(false)
    val liquidGlassEnabled: StateFlow<Boolean> = _liquidGlassEnabled.asStateFlow()

    private val _navBarStyle = MutableStateFlow(NavigationBarStyle.DEFAULT)
    val navBarStyle: StateFlow<NavigationBarStyle> = _navBarStyle.asStateFlow()

    private val _hideStatusBar = MutableStateFlow(false)
    val hideStatusBar: StateFlow<Boolean> = _hideStatusBar.asStateFlow()

    private val _pendingBook = MutableStateFlow<Pair<String, BookType>?>(null)
    val pendingBook: StateFlow<Pair<String, BookType>?> = _pendingBook.asStateFlow()

    init {
        viewModelScope.launch {
            preferencesStore.appThemeFlow.collectLatest { theme ->
                _theme.value = theme
            }
        }
        viewModelScope.launch {
            preferencesStore.liquidGlassEnabledFlow.collectLatest { enabled ->
                _liquidGlassEnabled.value = enabled
            }
        }
        viewModelScope.launch {
            preferencesStore.navBarStyleFlow.collectLatest { style ->
                _navBarStyle.value = style
            }
        }
        viewModelScope.launch {
            preferencesStore.hideStatusBarFlow.collectLatest { hide ->
                _hideStatusBar.value = hide
            }
        }
    }

    fun handleIntent(intent: Intent?) {
        if (intent?.action == Intent.ACTION_VIEW) {
            val uri = intent.data ?: return
            val type = if (uri.toString().lowercase().endsWith(".pdf")) BookType.PDF else BookType.EPUB
            _pendingBook.value = uri.toString() to type
        }
    }

    fun consumePendingBook() {
        _pendingBook.value = null
    }

    fun setTheme(theme: AppTheme) {
        viewModelScope.launch {
            preferencesStore.setAppTheme(theme)
        }
    }

    fun setLiquidGlassEnabled(enabled: Boolean) {
        viewModelScope.launch {
            preferencesStore.setLiquidGlassEnabled(enabled)
        }
    }

    fun setNavigationBarStyle(style: NavigationBarStyle) {
        viewModelScope.launch {
            preferencesStore.setNavigationBarStyle(style)
        }
    }
}
