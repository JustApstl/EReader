function triggerSelectionMenu(cfiRange, contents) {
    if (!cfiRange || !contents) {
        return;
    }

    var bridge = getBridge();
    if (!bridge || !bridge.onTextSelected) {
        return;
    }

    var selection = contents.window && contents.window.getSelection ? contents.window.getSelection() : null;
    var selectedText = selection ? selection.toString().trim() : "";
    if (!selectedText) {
        return;
    }

    var x = 0;
    var y = 0;
    try {
        var range = selection.rangeCount > 0 ? selection.getRangeAt(0) : null;
        if (range) {
            var selectionAnchor = anchorFromRect(
                range.getBoundingClientRect(),
                frameElementForRange(range)
            );
            if (selectionAnchor) {
                x = selectionAnchor.x;
                y = selectionAnchor.y;
            }
        }
    } catch (e) {
        log("Selection bounds warning: " + e.message);
    }

    var currentLocation = rendition && rendition.currentLocation ? rendition.currentLocation() : null;
    var chapterAnchor = currentLocation && currentLocation.start && currentLocation.start.cfi
        ? currentLocation.start.cfi
        : cfiRange;

    activeSelection = {
        chapterAnchor: chapterAnchor,
        cfiRange: cfiRange,
        text: selectedText
    };
    activeHighlight = null;
    activeNote = null;
    bridge.onTextSelected(chapterAnchor, cfiRange, selectedText, x, y);
}

function clearRenderedHighlights() {
    if (!rendition || !rendition.annotations) {
        renderedHighlightCfis = [];
        return;
    }

    for (var i = 0; i < renderedHighlightCfis.length; i++) {
        try {
            rendition.annotations.remove(renderedHighlightCfis[i], "highlight");
        } catch (e) {
            // Ignore stale annotations
        }
    }
    renderedHighlightCfis = [];
}

function clearRenderedNotes() {
    if (!rendition || !rendition.annotations) {
        renderedNoteCfis = [];
        return;
    }

    for (var i = 0; i < renderedNoteCfis.length; i++) {
        try {
            rendition.annotations.remove(renderedNoteCfis[i], "underline");
        } catch (e) {
            // Ignore stale annotation references
        }
    }
    renderedNoteCfis = [];
}

window.renderHighlights = function(highlightsJson) {
    if (!rendition || !rendition.annotations) {
        return;
    }

    clearRenderedHighlights();

    var highlights = [];
    try {
        highlights = JSON.parse(highlightsJson || "[]");
    } catch (e) {
        log("RenderHighlights parse error: " + e.message);
        return;
    }

    highlights.forEach(function(hl) {
        if (!hl || !hl.selectionJson) {
            return;
        }

        var cfiRange = hl.selectionJson;
        var color = hl.color || "#FFFF00";
        var highlightId = Number(hl.id || 0);

        try {
            rendition.annotations.add(
                "highlight",
                cfiRange,
                { id: highlightId },
                function(event) {
                    var bridge = getBridge();
                    if (!bridge || !bridge.onHighlightClicked) {
                        return;
                    }

                    var x = 0;
                    var y = 0;
                    try {
                        var node = event && (event.target || event.currentTarget || event.srcElement);
                        var frameElement = frameElementForNode(node);
                        var rect = node && node.getBoundingClientRect ? node.getBoundingClientRect() : null;
                        if (rect) {
                            var highlightAnchor = anchorFromRect(rect, frameElement);
                            if (highlightAnchor) {
                                x = highlightAnchor.x;
                                y = highlightAnchor.y;
                            }
                        } else if (event && typeof event.clientX === "number") {
                            var fallbackAnchor = anchorFromClientPoint(event.clientX, event.clientY, frameElement);
                            if (fallbackAnchor) {
                                x = fallbackAnchor.x;
                                y = fallbackAnchor.y;
                            }
                        }
                    } catch (err) {
                        log("Highlight click bounds warning: " + err.message);
                    }

                    activeSelection = null;
                    activeHighlight = { id: highlightId, cfiRange: cfiRange };
                    activeNote = null;
                    bridge.onHighlightClicked(highlightId, x, y);
                },
                "epubjs-hl",
                {
                    fill: color,
                    "fill-opacity": "0.45",
                    "mix-blend-mode": "multiply"
                }
            );
            renderedHighlightCfis.push(cfiRange);
        } catch (e) {
            log("Highlight add error: " + e.message);
        }
    });
};

window.renderNoteIndicators = function(notesJson) {
    if (!rendition || !rendition.annotations) {
        return;
    }

    clearRenderedNotes();

    var notes = [];
    try {
        notes = JSON.parse(notesJson || "[]");
    } catch (e) {
        log("RenderNoteIndicators parse error: " + e.message);
        return;
    }

    notes.forEach(function(note) {
        if (!note || !note.cfi) {
            return;
        }

        var indicatorColor = note.color || "#FF9800";
        var noteId = Number(note.id || 0);
        if (String(indicatorColor).toUpperCase() === "#FFFF00") {
            indicatorColor = "#FF9800";
        }
        try {
            rendition.annotations.add(
                "underline",
                note.cfi,
                { id: noteId },
                function(event) {
                    var bridge = getBridge();
                    if (!bridge || !bridge.onMarginNoteClicked) {
                        return;
                    }

                    var x = 0;
                    var y = 0;
                    try {
                        var node = event && (event.target || event.currentTarget || event.srcElement);
                        var frameElement = frameElementForNode(node);
                        var rect = node && node.getBoundingClientRect ? node.getBoundingClientRect() : null;
                        if (rect) {
                            var noteAnchor = anchorFromRect(rect, frameElement);
                            if (noteAnchor) {
                                x = noteAnchor.x;
                                y = noteAnchor.y;
                            }
                        } else if (event && typeof event.clientX === "number") {
                            var fallbackAnchor = anchorFromClientPoint(event.clientX, event.clientY, frameElement);
                            if (fallbackAnchor) {
                                x = fallbackAnchor.x;
                                y = fallbackAnchor.y;
                            }
                        }
                    } catch (err) {
                        log("Note indicator bounds warning: " + err.message);
                    }

                    activeSelection = null;
                    activeHighlight = null;
                    activeNote = { id: noteId, cfi: note.cfi };
                    bridge.onMarginNoteClicked(noteId, x, y);
                },
                "note-indicator",
                {
                    stroke: indicatorColor,
                    "stroke-width": "2",
                    opacity: "0.95"
                }
            );
            renderedNoteCfis.push(note.cfi);
        } catch (e) {
            log("Note indicator add error: " + e.message);
        }
    });
};
