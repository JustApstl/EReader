package com.dyu.ereader.ui.reader.formats.epub

import android.view.ActionMode
import android.view.Menu
import android.view.MenuItem

internal class SelectionBlockerCallback(private val originalCallback: ActionMode.Callback?) : ActionMode.Callback {
    override fun onCreateActionMode(mode: ActionMode?, menu: Menu?): Boolean {
        originalCallback?.onCreateActionMode(mode, menu)
        menu?.clear()
        return true
    }

    override fun onPrepareActionMode(mode: ActionMode?, menu: Menu?): Boolean {
        originalCallback?.onPrepareActionMode(mode, menu)
        menu?.clear()
        return true
    }

    override fun onActionItemClicked(mode: ActionMode?, item: MenuItem?): Boolean {
        return false
    }

    override fun onDestroyActionMode(mode: ActionMode?) {
        originalCallback?.onDestroyActionMode(mode)
    }
}
