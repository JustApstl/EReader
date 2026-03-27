function requestBookData(attempt) {
    var bridge = getBridge();
    if (!bridge || !bridge.getBookBase64) {
        return;
    }
    try {
        var b64Data = bridge.getBookBase64();
        if (b64Data && b64Data.length > 0) {
            bookData = base64ToArrayBuffer(b64Data);
            openBook(bookData);
        } else if (attempt < 3) {
            setTimeout(function() { requestBookData(attempt + 1); }, 120);
        } else {
            log("Book data empty after retries");
        }
    } catch (e) {
        log("Book data error: " + e.message);
    }
}

function resolveBookCacheKey() {
    if (bookCacheKey) {
        return bookCacheKey;
    }
    try {
        var bridge = getBridge();
        if (bridge && bridge.getBookCacheKey) {
            bookCacheKey = String(bridge.getBookCacheKey() || "");
        }
    } catch (e) {
        bookCacheKey = "";
    }
    return bookCacheKey;
}

function locationsCacheKey() {
    var key = resolveBookCacheKey();
    return key ? "reader.locations." + key : "";
}

function loadCachedLocations() {
    if (!book || !book.locations || !book.locations.load) {
        return false;
    }
    var key = locationsCacheKey();
    if (!key) {
        return false;
    }
    try {
        var cached = localStorage.getItem(key);
        if (cached) {
            book.locations.load(cached);
            return book.locations.length && book.locations.length() > 0;
        }
    } catch (e) {
        log("Locations cache load warning: " + e.message);
    }
    return false;
}

function saveCachedLocations() {
    if (!book || !book.locations || !book.locations.save) {
        return;
    }
    var key = locationsCacheKey();
    if (!key) {
        return;
    }
    try {
        var serialized = book.locations.save();
        if (serialized && serialized.length > 0) {
            localStorage.setItem(key, serialized);
        }
    } catch (e) {
        log("Locations cache save warning: " + e.message);
    }
}

function flattenTocEntries(entries, target) {
    if (!entries || !entries.length) {
        return target;
    }
    for (var i = 0; i < entries.length; i++) {
        var entry = entries[i];
        if (!entry) {
            continue;
        }
        target.push(entry);
        if (entry.subitems && entry.subitems.length) {
            flattenTocEntries(entry.subitems, target);
        }
    }
    return target;
}

function serializeTocEntries(entries, depth, target) {
    if (!entries || !entries.length) {
        return target;
    }
    for (var i = 0; i < entries.length; i++) {
        var entry = entries[i];
        if (!entry) {
            continue;
        }
        var subitems = entry.subitems && entry.subitems.length ? entry.subitems : [];
        target.push({
            label: (entry.label || "").trim(),
            href: entry.href || "",
            depth: depth || 0,
            hasChildren: subitems.length > 0
        });
        if (subitems.length) {
            serializeTocEntries(subitems, (depth || 0) + 1, target);
        }
    }
    return target;
}

function firstReadableNavigationTarget(nav) {
    var flat = flattenTocEntries(nav && nav.toc ? nav.toc : [], []);
    if (!flat.length) {
        return "";
    }
    var frontMatterPattern = /^(cover|title|title page|copyright|table of contents|contents)$/i;
    for (var i = 0; i < flat.length; i++) {
        var entry = flat[i];
        var href = entry && entry.href ? String(entry.href) : "";
        var label = entry && entry.label ? String(entry.label).trim() : "";
        if (href && !frontMatterPattern.test(label)) {
            return href;
        }
    }
    for (var j = 0; j < flat.length; j++) {
        if (flat[j] && flat[j].href) {
            return String(flat[j].href);
        }
    }
    return "";
}

function resolveInitialDisplayTarget(explicitTarget, bridge) {
    if (explicitTarget) {
        return Promise.resolve(explicitTarget);
    }

    var initialProgress = bridge && bridge.getInitialProgress ? parseFloat(bridge.getInitialProgress()) : 0;
    var hasInitialProgress = !isNaN(initialProgress) && initialProgress > 0.0005 && initialProgress < 0.9995;
    if (hasInitialProgress) {
        return ensureLocationsGenerated().then(function() {
            var generatedCfi = book && book.locations && book.locations.cfiFromPercentage
                ? book.locations.cfiFromPercentage(initialProgress)
                : "";
            if (generatedCfi) {
                log("Display initial percentage=" + initialProgress.toFixed(4));
            }
            return generatedCfi || "";
        }).catch(function() {
            return "";
        });
    }

    if (isPageReadingMode() && book && book.loaded && book.loaded.navigation) {
        return book.loaded.navigation.then(function(nav) {
            var tocTarget = firstReadableNavigationTarget(nav);
            if (tocTarget) {
                log("Display initial toc target=" + tocTarget);
            }
            return tocTarget || "";
        }).catch(function() {
            return "";
        });
    }

    return Promise.resolve("");
}

function collectContentMetrics(contents) {
    var doc = contents && contents.document;
    var body = doc && doc.body;
    var html = doc && doc.documentElement;
    var bodyStyle = body && contents.window ? contents.window.getComputedStyle(body) : null;
    var firstTextElement = doc
        ? doc.querySelector("p, h1, h2, h3, h4, h5, h6, li, article, section, blockquote, span, div")
        : null;
    var firstTextStyle = firstTextElement && contents.window
        ? contents.window.getComputedStyle(firstTextElement)
        : null;
    var firstTextRect = firstTextElement && firstTextElement.getBoundingClientRect
        ? firstTextElement.getBoundingClientRect()
        : null;
    return {
        textLength: body && body.textContent ? body.textContent.trim().length : -1,
        bodyWidth: body ? body.scrollWidth : -1,
        htmlWidth: html ? html.scrollWidth : -1,
        color: bodyStyle ? bodyStyle.color : "n/a",
        display: bodyStyle ? bodyStyle.display : "n/a",
        visibility: bodyStyle ? bodyStyle.visibility : "n/a",
        opacity: bodyStyle ? bodyStyle.opacity : "n/a",
        firstTag: firstTextElement ? firstTextElement.tagName : "n/a",
        firstTextLength: firstTextElement && firstTextElement.textContent
            ? firstTextElement.textContent.trim().length
            : -1,
        firstColor: firstTextStyle ? firstTextStyle.color : "n/a",
        firstFill: firstTextStyle ? firstTextStyle.webkitTextFillColor : "n/a",
        firstDisplay: firstTextStyle ? firstTextStyle.display : "n/a",
        firstOpacity: firstTextStyle ? firstTextStyle.opacity : "n/a",
        firstFontSize: firstTextStyle ? firstTextStyle.fontSize : "n/a",
        firstRectLeft: firstTextRect ? Math.round(firstTextRect.left) : -1,
        firstRectTop: firstTextRect ? Math.round(firstTextRect.top) : -1,
        imageCount: doc && doc.querySelectorAll ? doc.querySelectorAll("img, svg, image").length : 0
    };
}

function openBook(data, cfi) {
    try {
        initialJumpDone = false;
        locationsReadyPromise = null;
        openBookRetryCount = 0;
        clearRenderedHighlights();
        clearRenderedNotes();
        activeSelection = null;
        activeHighlight = null;
        activeNote = null;
        coverImageExcluded = false;
        if (window.resetPageTurnViewportState) {
            window.resetPageTurnViewportState();
        } else {
            clearSwipePeek();
        }
        destroyPreviewRenditions();

        var viewerStage = document.getElementById("viewer-stage");
        var viewer = document.getElementById("viewer");
        if (!viewerStage) {
            viewerStage = document.createElement("div");
            viewerStage.id = "viewer-stage";
            document.body.appendChild(viewerStage);
        }
        if (!viewer) {
            viewer = document.createElement("div");
            viewer.id = "viewer";
            viewerStage.appendChild(viewer);
        } else if (viewer.parentElement !== viewerStage) {
            viewerStage.innerHTML = "";
            viewerStage.appendChild(viewer);
        }

        if (rendition) {
            try {
                rendition.destroy();
            } catch (e) {
                log("Rendition destroy warning: " + e.message);
            }
        }

        viewer.innerHTML = "";

        bookCacheKey = "";
        resolveBookCacheKey();
        book = ePub(data);
        applyReaderModeBodyState();

        try {
            rendition = book.renderTo("viewer", resolveReaderRenditionOptions());
            prepareReaderRendition(rendition);
        } catch (e) {
            log("RenderTo error: " + e.message);
            if (bookData && openBookRetryCount < 1) {
                openBookRetryCount += 1;
                setTimeout(function() { openBook(bookData, cfi); }, 150);
            }
            return;
        }

        setupReaderModePreviewRenderers();

        rendition.hooks.content.register(function(contents) {
            try {
                enforceSelectionBehavior(contents);
                setupTouchHandling(contents);
                setupMenuAnchorTracking(contents);
                setupExternalLinkHandling(contents);
                setupImageTapHandling(contents);
                applyStylesToContents(contents);
                if (listenActive && window.prepareTtsWords) {
                    window.prepareTtsWords();
                }
                if (contents.window) {
                    contents.window.AndroidReader = getBridge();
                }
            } catch (e) {
                log("Content hook error: " + e.message);
            }
        });

        rendition.on("selected", function(cfiRange, contents) {
            triggerSelectionMenu(cfiRange, contents);
        });

        rendition.on("relocated", function(location) {
            handleReaderRelocated(location);
        });

        book.loaded.navigation.then(function(nav) {
            var chapters = serializeTocEntries(nav.toc || [], 0, []);
            cacheTocEntries(chapters);
            var bridge = getBridge();
            if (bridge && bridge.onChaptersLoaded) {
                bridge.onChaptersLoaded(JSON.stringify(chapters));
            }
        }).catch(function(e) {
            log("Navigation parse warning: " + e.message);
        });

        var requestedCfi = cfi || null;
        var localRendition = rendition;
        book.ready.then(function() {
            var bridge = getBridge();
            var targetCfi = requestedCfi || (bridge && bridge.getInitialCfi ? bridge.getInitialCfi() : "");
            if (!localRendition || localRendition !== rendition) {
                log("Rendition missing before display");
                throw new Error("Rendition missing");
            }
            loadCachedLocations();
            if (savedSettings) {
                try {
                    applySettings(savedSettings, true);
                } catch (e) {
                    log("applySettings error: " + e.message);
                }
            }
            safeResize(localRendition);
            return resolveInitialDisplayTarget(targetCfi, bridge).then(function(resolvedTarget) {
                return localRendition.display(resolvedTarget || undefined).catch(function() {
                    log("Display fallback to first spine");
                    return localRendition.display();
                }).then(function(displayResult) {
                    if (window.disableContinuousScrollAnchoring) {
                        window.disableContinuousScrollAnchoring(localRendition);
                    }
                    return displayResult;
                });
            });
        }).then(function() {
            setTimeout(function() {
                if (window.resetPageTurnViewportState) {
                    window.resetPageTurnViewportState();
                }
                if (window.disableContinuousScrollAnchoring) {
                    window.disableContinuousScrollAnchoring(rendition);
                }
                try {
                    var contentsCount = rendition && rendition.getContents ? rendition.getContents().length : -1;
                    var viewer = document.getElementById("viewer");
                    var viewerStage = document.getElementById("viewer-stage");
                    var iframe = viewer ? viewer.querySelector("iframe") : null;
                    var viewerStyle = viewerStage ? window.getComputedStyle(viewerStage) : null;
                    var iframeRect = iframe && iframe.getBoundingClientRect ? iframe.getBoundingClientRect() : null;
                    log(
                        "Display success mode=" + (savedSettings && savedSettings.readingMode ? savedSettings.readingMode : "unknown") +
                        " contents=" + contentsCount +
                        " bodyClasses=" + document.body.className +
                        " viewerClasses=" + (viewerStage ? viewerStage.className : "") +
                        " viewerTransform=" + (viewerStyle ? viewerStyle.transform : "n/a") +
                        " iframeLeft=" + (iframeRect ? Math.round(iframeRect.left) : -1) +
                        " iframeTop=" + (iframeRect ? Math.round(iframeRect.top) : -1) +
                        " iframeWidth=" + (iframeRect ? Math.round(iframeRect.width) : -1) +
                        " iframeHeight=" + (iframeRect ? Math.round(iframeRect.height) : -1)
                    );
                } catch (displayDiagnosticError) {
                    log("Display diagnostic warning: " + displayDiagnosticError.message);
                }
                initialJumpDone = true;
                safeResize(rendition);
                requestHighlightSync();
                updateProgress();
                updatePagination();
                ensureLocationsGenerated().then(function() {
                    updateProgress();
                    updatePagination();
                    if (isPageReadingMode()) {
                        refreshAdjacentPreviews();
                        scheduleAdjacentPreviewWarmup();
                    }
                });
                if (isPageReadingMode()) {
                    refreshAdjacentPreviews();
                    scheduleAdjacentPreviewWarmup();
                }
                if (listenActive && window.scheduleListenExtraction) {
                    window.scheduleListenExtraction();
                } else if (listenActive && window.requestTextExtraction) {
                    window.requestTextExtraction();
                }
                scheduleRenderCheck(requestedCfi);
            }, 120);
        }).catch(function(e) {
            log("Book ready/display error: " + e.message);
            if (bookData && openBookRetryCount < 1) {
                openBookRetryCount += 1;
                setTimeout(function() { openBook(bookData, requestedCfi || undefined); }, 150);
            }
        });
    } catch (e) {
        log("OpenBook Error: " + e.message);
    }
}

function safeResize(target) {
    try {
        var resizeFn = target && target.resize;
        if (typeof resizeFn === "function" && target.manager) {
            target.resize();
        }
    } catch (e) {
        log("Resize warning: " + e.message);
    }
}

function resolveCurrentLocationSafe(targetRendition) {
    try {
        if (!targetRendition) {
            return null;
        }
        var currentLocationFn = targetRendition.currentLocation;
        if (typeof currentLocationFn !== "function") {
            return null;
        }
        return currentLocationFn.call(targetRendition);
    } catch (e) {
        return null;
    }
}

function scheduleRenderCheck(targetCfi) {
    setTimeout(function() {
        var hasContents = false;
        try {
            hasContents = rendition && rendition.getContents && rendition.getContents().length > 0;
        } catch (e) {
            hasContents = false;
        }
        if (!hasContents && bookData && openBookRetryCount < 1) {
            openBookRetryCount += 1;
            log("Render check retry");
            openBook(bookData, targetCfi || undefined);
        }
    }, 800);
}

function ensureLocationsGenerated() {
    if (!book || !book.locations || !book.locations.generate) {
        return Promise.resolve();
    }
    if (book.locations.length && book.locations.length() > 0) {
        return Promise.resolve();
    }
    if (locationsReadyPromise) {
        return locationsReadyPromise;
    }
    locationsReadyPromise = book.locations.generate(1024)
        .catch(function(e) {
            log("Location map generation warning: " + e.message);
        })
        .then(function() {
            saveCachedLocations();
            return true;
        });
    return locationsReadyPromise;
}

function requestHighlightSync() {
    var bridge = getBridge();
    if (bridge && bridge.requestHighlights) {
        bridge.requestHighlights();
    }
    if (bridge && bridge.requestMarginNotes) {
        bridge.requestMarginNotes();
    }
}

function resolveCoverTarget() {
    if (!book) {
        return null;
    }
    try {
        if (book.packaging && book.packaging.metadata && book.packaging.metadata.cover) {
            var coverId = book.packaging.metadata.cover;
            var manifest = book.packaging.manifest || {};
            var coverItem = manifest[coverId];
            if (coverItem && coverItem.href) {
                return coverItem.href;
            }
        }
    } catch (e) {
        log("Cover resolve metadata warning: " + e.message);
    }

    try {
        if (book.cover) {
            if (typeof book.cover === "string") {
                var manifest2 = (book.packaging && book.packaging.manifest) ? book.packaging.manifest : null;
                if (manifest2 && manifest2[book.cover] && manifest2[book.cover].href) {
                    return manifest2[book.cover].href;
                }
                return book.cover;
            }
            if (book.cover.href) {
                return book.cover.href;
            }
        }
    } catch (e) {
        log("Cover resolve warning: " + e.message);
    }

    try {
        if (book.spine && book.spine.first) {
            var first = book.spine.first();
            if (first && first.href) {
                return first.href;
            }
            if (first && first.cfi) {
                return first.cfi;
            }
        }
    } catch (e) {
        log("Cover spine fallback warning: " + e.message);
    }

    return null;
}

window.jumpTo = function(anchor) {
    if (!rendition || !anchor) {
        return;
    }
    try {
        if (anchor === COVER_ANCHOR) {
            var coverTarget = resolveCoverTarget();
            rendition.display(coverTarget || undefined);
            return;
        }
        rendition.display(anchor);
    } catch (e) {
        log("JumpTo Error: " + e.message);
    }
};

window.jumpToPercentage = function(percent) {
    if (!book || !rendition) {
        return;
    }

    try {
        var p = parseFloat(percent);
        if (isNaN(p)) {
            return;
        }
        p = Math.max(0, Math.min(1, p));

        if (p <= 0) {
            rendition.display();
            return;
        }

        if (!book.locations || book.locations.length() === 0) {
            ensureLocationsGenerated().then(function() {
                var generatedCfi = book.locations.cfiFromPercentage(p);
                rendition.display(generatedCfi);
            });
        } else {
            var cfi = book.locations.cfiFromPercentage(p);
            rendition.display(cfi);
        }
    } catch (e) {
        log("JumpToPercentage Error: " + e.message);
    }
};

function updateProgress(location) {
    var bridge = getBridge();
    if (!bridge || !book || !initialJumpDone) {
        return;
    }

    var currentLoc = location || resolveCurrentLocationSafe(rendition);
    if (!currentLoc || !currentLoc.start) {
        return;
    }

    var percent = book.locations && book.locations.length() > 0
        ? book.locations.percentageFromCfi(currentLoc.start.cfi)
        : currentLoc.start.percentage;

    bridge.onLocationChanged(JSON.stringify({
        start: {
            cfi: currentLoc.start.cfi,
            percentage: percent
        },
        explicitPercentage: percent
    }));
}

function updatePagination() {
    if (!rendition || !book) {
        return;
    }

    var bridge = getBridge();
    if (!bridge || !bridge.onPaginationChanged) {
        return;
    }

    var applyPagination = function() {
        var currentLocation = resolveCurrentLocationSafe(rendition);
        if (!currentLocation || !currentLocation.start) {
            return;
        }

        var hasLocations = book.locations && book.locations.length && book.locations.length() > 0;
        var currentPage = currentLocation.start.index + 1;
        var totalPages = book.spine ? book.spine.length : 0;

        if (hasLocations) {
            currentPage = book.locations.locationFromCfi(currentLocation.start.cfi) + 1;
            totalPages = book.locations.length();
        }

        bridge.onPaginationChanged(currentPage, totalPages);
    };

    if (!book.locations || !book.locations.length || book.locations.length() === 0) {
        ensureLocationsGenerated().then(function() {
            applyPagination();
        });
        return;
    }

    applyPagination();
}

window.addEventListener("resize", function() {
    handleReaderResize();
});

window.addEventListener("scroll", scheduleActiveMenuPositionUpdate, { passive: true });
document.addEventListener("scroll", scheduleActiveMenuPositionUpdate, true);
