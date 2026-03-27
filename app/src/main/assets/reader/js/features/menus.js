function updateActiveMenuPositions() {
    var bridge = getBridge();
    if (!bridge) {
        return;
    }

    if (menuState.selectionOpen && activeSelection && bridge.onTextSelected) {
        var selectionAnchor = anchorForCfi(activeSelection.cfiRange);
        if (selectionAnchor) {
            bridge.onTextSelected(
                activeSelection.chapterAnchor,
                activeSelection.cfiRange,
                activeSelection.text,
                selectionAnchor.x,
                selectionAnchor.y
            );
        }
    }

    if (menuState.highlightOpen && activeHighlight && bridge.onHighlightClicked) {
        var highlightAnchor = anchorForCfi(activeHighlight.cfiRange);
        if (highlightAnchor) {
            bridge.onHighlightClicked(activeHighlight.id, highlightAnchor.x, highlightAnchor.y);
        }
    }

    if (menuState.noteOpen && activeNote && bridge.onMarginNoteClicked) {
        var noteAnchor = anchorForCfi(activeNote.cfi);
        if (noteAnchor) {
            bridge.onMarginNoteClicked(activeNote.id, noteAnchor.x, noteAnchor.y);
        }
    }
}

function scheduleActiveMenuPositionUpdate() {
    if (anchorUpdateScheduled) {
        return;
    }
    anchorUpdateScheduled = true;
    window.requestAnimationFrame(function() {
        anchorUpdateScheduled = false;
        updateActiveMenuPositions();
    });
}

function setupMenuAnchorTracking(contents) {
    if (!contents || !contents.window) {
        return;
    }
    var contentWindow = contents.window;
    if (contentWindow.__menuAnchorTrackingInstalled) {
        return;
    }
    contentWindow.__menuAnchorTrackingInstalled = true;

    contentWindow.addEventListener("scroll", scheduleActiveMenuPositionUpdate, { passive: true });
    contentWindow.addEventListener("resize", scheduleActiveMenuPositionUpdate);
}

function clearBrowserSelections() {
    if (!rendition || !rendition.getContents) {
        return;
    }
    try {
        rendition.getContents().forEach(function(contents) {
            var selection = contents && contents.window && contents.window.getSelection
                ? contents.window.getSelection()
                : null;
            if (selection && selection.removeAllRanges) {
                selection.removeAllRanges();
            }
        });
    } catch (e) {
        log("Clear selection warning: " + e.message);
    }
}

window.clearActiveSelection = function() {
    activeSelection = null;
    clearBrowserSelections();
};

window.setNativeMenuState = function(selectionOpen, highlightOpen, noteOpen) {
    menuState.selectionOpen = !!selectionOpen;
    menuState.highlightOpen = !!highlightOpen;
    menuState.noteOpen = !!noteOpen;

    if (!menuState.selectionOpen) {
        activeSelection = null;
        clearBrowserSelections();
    }
    if (!menuState.highlightOpen) {
        activeHighlight = null;
    }
    if (!menuState.noteOpen) {
        activeNote = null;
    }

    updateActiveMenuPositions();
};
