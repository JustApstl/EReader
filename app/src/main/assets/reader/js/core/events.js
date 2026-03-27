function enforceSelectionBehavior(contents) {
    var doc = contents.document;
    if (!doc) {
        return;
    }

    doc.documentElement.style.setProperty("-webkit-user-select", "text", "important");
    doc.body.style.setProperty("-webkit-user-select", "text", "important");
    doc.documentElement.style.setProperty("overflow-anchor", "none", "important");
    doc.body.style.setProperty("overflow-anchor", "none", "important");

    var scrollingElement = doc.scrollingElement || doc.documentElement || doc.body;
    if (scrollingElement && scrollingElement.style) {
        scrollingElement.style.setProperty("overflow-anchor", "none", "important");
    }

    try {
        var anchoredNodes = doc.querySelectorAll(".epub-container");
        for (var i = 0; i < anchoredNodes.length; i++) {
            anchoredNodes[i].style.setProperty("overflow-anchor", "none", "important");
        }
    } catch (e) {
        log("Anchoring node warning: " + e.message);
    }
}

function setupExternalLinkHandling(contents) {
    var doc = contents.document;
    if (!doc) {
        return;
    }

    doc.addEventListener("click", function(event) {
        if (doc.__readerSuppressClickUntil && Date.now() < doc.__readerSuppressClickUntil) {
            event.preventDefault();
            event.stopPropagation();
            return;
        }
        var node = event.target;
        while (node && node.tagName !== "A") {
            node = node.parentElement;
        }
        if (!node) {
            return;
        }

        var href = node.getAttribute("href") || "";
        var isExternalLink =
            href.startsWith("http://") ||
            href.startsWith("https://") ||
            href.startsWith("www.") ||
            href.startsWith("mailto:");
        if (isExternalLink) {
            event.preventDefault();
            var bridge = getBridge();
            var resolvedHref = href.startsWith("www.") ? ("https://" + href) : href;
            if (bridge && bridge.openBrowser) {
                bridge.openBrowser(resolvedHref);
            }
        }
    }, true);
}

function blobToDataUrl(blob) {
    return new Promise(function(resolve, reject) {
        try {
            var reader = new FileReader();
            reader.onloadend = function() { resolve(reader.result); };
            reader.onerror = function() { reject(new Error("File read failed")); };
            reader.readAsDataURL(blob);
        } catch (e) {
            reject(e);
        }
    });
}

function resolveTapZoneForPoint(contents, tapX, tapY) {
    var width = contents.document.documentElement.clientWidth || contents.window.innerWidth || 1;
    var height = contents.document.documentElement.clientHeight || contents.window.innerHeight || 1;
    var isPageModeTap = savedSettings && savedSettings.readingMode === "PAGE";
    if (isPageModeTap) {
        return "CENTER";
    }
    var topThreshold = isPageModeTap ? 0.16 : 0.22;
    var bottomThreshold = isPageModeTap ? 0.84 : 0.78;
    var sideThreshold = isPageModeTap ? 0.24 : 0.33;
    if (tapY < height * topThreshold) {
        return "TOP";
    }
    if (tapY > height * bottomThreshold) {
        return "BOTTOM";
    }
    if (tapX < width * sideThreshold) {
        return "LEFT";
    }
    if (tapX > width * (1 - sideThreshold)) {
        return "RIGHT";
    }
    return "CENTER";
}

function dispatchTapZoneForPoint(contents, tapX, tapY) {
    var bridge = getBridge();
    if (!bridge) {
        return;
    }
    if (contents && contents.document) {
        contents.document.__readerSuppressClickUntil = Date.now() + 420;
    }
    var zone = resolveTapZoneForPoint(contents, tapX, tapY);
    log("Tap zone=" + zone + " x=" + Math.round(tapX) + " y=" + Math.round(tapY));
    if (zone === "CENTER" && bridge.toggleMenu) {
        bridge.toggleMenu();
    } else if (bridge.onTapZone) {
        bridge.onTapZone(zone);
    } else if (bridge.toggleMenu) {
        bridge.toggleMenu();
    }
}

function sendImageToAndroid(src, bridge, doc, imgElement) {
    if (!src || !bridge || !bridge.onImageClicked) {
        return;
    }

    var resolvedSrc = src;
    try {
        if (doc && doc.baseURI) {
            resolvedSrc = new URL(src, doc.baseURI).href;
        }
    } catch (e) {
        resolvedSrc = src;
    }

    if (resolvedSrc.indexOf("data:") === 0) {
        bridge.onImageClicked(resolvedSrc);
        return;
    }

    if (imgElement && imgElement.naturalWidth && imgElement.naturalHeight) {
        try {
            var canvas = document.createElement("canvas");
            canvas.width = imgElement.naturalWidth;
            canvas.height = imgElement.naturalHeight;
            var ctx = canvas.getContext("2d");
            ctx.drawImage(imgElement, 0, 0);
            var dataUrl = canvas.toDataURL("image/png");
            if (dataUrl && dataUrl.indexOf("data:") === 0) {
                bridge.onImageClicked(dataUrl);
                return;
            }
        } catch (e) {
            // Fallback to fetch-based resolution
        }
    }

    if (resolvedSrc.indexOf("blob:") === 0 || resolvedSrc.indexOf("file:") === 0 || resolvedSrc.indexOf("content:") === 0) {
        try {
            fetch(resolvedSrc)
                .then(function(resp) { return resp.blob(); })
                .then(function(blob) { return blobToDataUrl(blob); })
                .then(function(dataUrl) { bridge.onImageClicked(dataUrl); })
                .catch(function() { bridge.onImageClicked(resolvedSrc); });
        } catch (e) {
            bridge.onImageClicked(resolvedSrc);
        }
        return;
    }

    // Best effort: try to fetch and convert relative/http(s) resources.
    try {
        fetch(resolvedSrc)
            .then(function(resp) { return resp.blob(); })
            .then(function(blob) { return blobToDataUrl(blob); })
            .then(function(dataUrl) { bridge.onImageClicked(dataUrl); })
            .catch(function() { bridge.onImageClicked(resolvedSrc); });
    } catch (e) {
        bridge.onImageClicked(resolvedSrc);
    }
}

function resolvePagedSwipeDirection(diffX) {
    var baseDirection = diffX > 0 ? "next" : "prev";
    if (savedSettings && savedSettings.invertPageTurns) {
        return baseDirection === "next" ? "prev" : "next";
    }
    return baseDirection;
}

function resolvePagedPhysicalDirection(diffX) {
    return diffX > 0 ? "next" : "prev";
}

function setupImageTapHandling(contents) {
    var doc = contents.document;
    if (!doc || doc.__imageTapInstalled) {
        return;
    }
    doc.__imageTapInstalled = true;

    doc.addEventListener("click", function(event) {
        if (doc.__readerSuppressClickUntil && Date.now() < doc.__readerSuppressClickUntil) {
            event.preventDefault();
            event.stopPropagation();
            return;
        }
        var target = event.target;
        if (!target) {
            return;
        }
        var img = null;
        if (target.tagName && target.tagName.toLowerCase() === "img") {
            img = target;
        } else if (target.closest) {
            img = target.closest("img");
        }
        if (!img) {
            return;
        }

        event.preventDefault();
        event.stopPropagation();

        var src = img.currentSrc || img.src || img.getAttribute("src") || "";
        if (!src) {
            return;
        }
        var bridge = getBridge();
        sendImageToAndroid(src, bridge, doc, img);
    }, true);
}

function setupClickTapFallback(contents) {
    var doc = contents.document;
    if (!doc || doc.__readerTapFallbackInstalled) {
        return;
    }
    doc.__readerTapFallbackInstalled = true;

    doc.addEventListener("click", function(event) {
        if (doc.__readerSuppressClickUntil && Date.now() < doc.__readerSuppressClickUntil) {
            event.preventDefault();
            event.stopPropagation();
            return;
        }

        var target = event.target;
        if (!target) {
            return;
        }

        var link = null;
        if (target.tagName && target.tagName.toLowerCase() === "a") {
            link = target;
        } else if (target.closest) {
            link = target.closest("a");
        }
        if (link) {
            return;
        }

        var img = null;
        if (target.tagName && target.tagName.toLowerCase() === "img") {
            img = target;
        } else if (target.closest) {
            img = target.closest("img");
        }
        if (img) {
            return;
        }

        var selection = contents.window && contents.window.getSelection ? contents.window.getSelection() : null;
        if (selection && selection.toString && selection.toString().trim().length > 0) {
            return;
        }

        event.preventDefault();
        event.stopPropagation();

        dispatchTapZoneForPoint(
            contents,
            event.clientX || 0,
            event.clientY || 0
        );
    }, true);
}

function setupTouchHandling(contents) {
    var startX = 0;
    var startY = 0;
    var selectionTimeout = null;
    var swipePreviewActive = false;
    var swipeGestureLocked = false;
    var lastSwipeDirection = null;
    var lastSwipeProgress = 0;
    var lastSwipeDistance = 0;
    var swipeFrame = 0;
    var pendingSwipeUpdate = null;
    var tapZoneDispatched = false;
    var pagedSwipeLocked = false;
    var swipeLockedDirection = null;
    var touchTargetImage = null;
    var touchTargetLink = null;
    var PAGE_TAP_THRESHOLD = 22;
    var DEFAULT_TAP_THRESHOLD = 12;
    var PAGE_SWIPE_LOCK_THRESHOLD = 18;
    var DEFAULT_SWIPE_LOCK_THRESHOLD = 12;

    function resetPagedViewportScroll() {
        if (!(savedSettings && savedSettings.readingMode === "PAGE")) {
            return;
        }
        try {
            if (contents.window && contents.window.scrollTo) {
                contents.window.scrollTo(0, 0);
            }
        } catch (e) {}
        try {
            var doc = contents.document;
            var scrollingElement = doc.scrollingElement || doc.documentElement || doc.body;
            if (scrollingElement) {
                scrollingElement.scrollLeft = 0;
                scrollingElement.scrollTop = 0;
            }
            if (doc.documentElement) {
                doc.documentElement.scrollLeft = 0;
                doc.documentElement.scrollTop = 0;
            }
            if (doc.body) {
                doc.body.scrollLeft = 0;
                doc.body.scrollTop = 0;
            }
        } catch (e2) {}
    }

    function setPagedSwipeLockState(locked) {
        if (!(savedSettings && savedSettings.readingMode === "PAGE")) {
            return;
        }
        if (pagedSwipeLocked === locked) {
            return;
        }
        pagedSwipeLocked = locked;
        try {
            var doc = contents.document;
            if (doc.documentElement) {
                doc.documentElement.style.setProperty("-webkit-user-select", locked ? "none" : "text", "important");
            }
            if (doc.body) {
                doc.body.style.setProperty("-webkit-user-select", locked ? "none" : "text", "important");
            }
            var selection = contents.window && contents.window.getSelection ? contents.window.getSelection() : null;
            if (locked && selection && selection.removeAllRanges) {
                selection.removeAllRanges();
            }
        } catch (e) {}
    }

    function flushSwipeUpdate() {
        swipeFrame = 0;
        if (!pendingSwipeUpdate || !window.updateSwipePeek) {
            pendingSwipeUpdate = null;
            return;
        }
        var update = pendingSwipeUpdate;
        pendingSwipeUpdate = null;
        window.updateSwipePeek(update.direction, update.progress, update.distance, update.point || null);
    }

    function scheduleSwipeUpdate(direction, progress, distance, point) {
        if (swipeFrame) {
            pendingSwipeUpdate = {
                direction: direction,
                progress: progress,
                distance: distance,
                point: point || null
            };
            return;
        }
        if (window.updateSwipePeek) {
            window.updateSwipePeek(direction, progress, distance, point || null);
        }
        swipeFrame = window.requestAnimationFrame(function() {
            swipeFrame = 0;
            if (pendingSwipeUpdate) {
                flushSwipeUpdate();
            }
        });
    }

    function resetSwipeFrame() {
        if (swipeFrame) {
            window.cancelAnimationFrame(swipeFrame);
            swipeFrame = 0;
        }
        pendingSwipeUpdate = null;
    }

    function dispatchTapZoneFromTouch(touchEvent) {
        var tapX = touchEvent.clientX || touchEvent.pageX || 0;
        var tapY = touchEvent.clientY || touchEvent.pageY || 0;
        dispatchTapZoneForPoint(contents, tapX, tapY);
    }

    function resolveTouchTargetImage(target) {
        if (!target) {
            return null;
        }
        if (target.tagName && target.tagName.toLowerCase() === "img") {
            return target;
        }
        if (target.closest) {
            return target.closest("img");
        }
        return null;
    }

    function resolveTouchTargetLink(target) {
        if (!target) {
            return null;
        }
        if (target.tagName && target.tagName.toLowerCase() === "a") {
            return target;
        }
        if (target.closest) {
            return target.closest("a");
        }
        return null;
    }

    function openTouchTargetImage() {
        if (!touchTargetImage) {
            return false;
        }
        var bridge = getBridge();
        var doc = contents.document;
        if (!bridge || !bridge.onImageClicked || !doc) {
            return false;
        }
        var src = touchTargetImage.currentSrc || touchTargetImage.src || touchTargetImage.getAttribute("src") || "";
        if (!src) {
            return false;
        }
        sendImageToAndroid(src, bridge, doc, touchTargetImage);
        return true;
    }

    contents.document.addEventListener("touchstart", function(e) {
        startX = e.changedTouches[0].clientX;
        startY = e.changedTouches[0].clientY;
        var isPageMode = savedSettings && savedSettings.readingMode === "PAGE";
        var allowPreview = !!(savedSettings && savedSettings.pageTurn3d);
        swipePreviewActive = false;
        swipeGestureLocked = false;
        lastSwipeDirection = null;
        lastSwipeProgress = 0;
        lastSwipeDistance = 0;
        swipeLockedDirection = null;
        touchTargetImage = resolveTouchTargetImage(e.target);
        touchTargetLink = resolveTouchTargetLink(e.target);
        tapZoneDispatched = false;
        resetSwipeFrame();
        if (window.resetPageTurnViewportState) {
            window.resetPageTurnViewportState();
        } else if (window.clearSwipePeek) {
            window.clearSwipePeek();
        }
        setPagedSwipeLockState(false);
        resetPagedViewportScroll();
        if (selectionTimeout) {
            clearTimeout(selectionTimeout);
        }
        if (allowPreview && window.schedulePagedPreviewWarmup) {
            window.schedulePagedPreviewWarmup(20);
        }
    }, { passive: true });

    contents.document.addEventListener("touchmove", function(e) {
        var isPageMode = savedSettings && savedSettings.readingMode === "PAGE";
        if (!isPageMode) {
            return;
        }
        var allowPreview = !!(savedSettings && savedSettings.pageTurn3d);

        var moveX = e.changedTouches[0].clientX;
        var moveY = e.changedTouches[0].clientY;
        var diffX = startX - moveX;
        var diffY = startY - moveY;
        var absX = Math.abs(diffX);
        var absY = Math.abs(diffY);
        var rawDirection = resolvePagedSwipeDirection(diffX);
        var physicalDirection = resolvePagedPhysicalDirection(diffX);
        var swipeLockThreshold = isPageMode ? PAGE_SWIPE_LOCK_THRESHOLD : DEFAULT_SWIPE_LOCK_THRESHOLD;

        if (!swipeGestureLocked && absX > swipeLockThreshold && absX > (absY + 8)) {
            swipeGestureLocked = true;
            swipeLockedDirection = rawDirection;
        }

        if (swipeGestureLocked) {
            var wasPagedSwipeLocked = pagedSwipeLocked;
            setPagedSwipeLockState(true);
            if (!wasPagedSwipeLocked) {
                resetPagedViewportScroll();
            }
            e.preventDefault();
            e.stopPropagation();
        }

        if (!swipeGestureLocked && (absX < 6 || absX <= absY || absY > 120)) {
            if (swipePreviewActive && window.clearSwipePeek) {
                window.clearSwipePeek();
                swipePreviewActive = false;
            }
            return;
        }

        var width = contents.document.documentElement.clientWidth || contents.window.innerWidth || window.innerWidth || 360;
        var direction = swipeLockedDirection || rawDirection;
        var lockedPhysicalDirection = swipeGestureLocked
            ? (swipeLockedDirection === rawDirection ? physicalDirection : (physicalDirection === "next" ? "prev" : "next"))
            : physicalDirection;
        var directionalDistance = lockedPhysicalDirection === "next" ? diffX : -diffX;
        var dragDistance = Math.max(0, directionalDistance);
        var progress = Math.min(1, dragDistance / Math.max(140, width * 0.94));
        swipePreviewActive = progress > 0.02;
        lastSwipeDirection = direction;
        lastSwipeProgress = progress;
        lastSwipeDistance = dragDistance;

        if (!allowPreview) {
            swipePreviewActive = false;
            return;
        }

        if (swipePreviewActive && !pagedSwipeLocked) {
            swipePreviewActive = false;
        }

        if (!swipePreviewActive && progress > 0.02 && window.beginPagedPreviewGesture) {
            swipePreviewActive = !!window.beginPagedPreviewGesture(direction, {
                x: startX,
                y: startY
            });
        }

        if (swipePreviewActive && window.updateSwipePeek) {
            scheduleSwipeUpdate(direction, progress, dragDistance, {
                x: moveX,
                y: moveY
            });
            e.preventDefault();
        } else if (window.clearSwipePeek) {
            window.clearSwipePeek();
        }
    }, { passive: false });

    contents.document.addEventListener("touchend", function(e) {
        var endX = e.changedTouches[0].clientX;
        var endY = e.changedTouches[0].clientY;
        var diffX = startX - endX;
        var diffY = startY - endY;
        var isPageMode = savedSettings && savedSettings.readingMode === "PAGE";
        var allowThreeD = !!(savedSettings && savedSettings.pageTurn3d);
        var pageWidth = contents.document.documentElement.clientWidth || contents.window.innerWidth || window.innerWidth || 360;
        var resolvedSwipeDirection = swipeLockedDirection || lastSwipeDirection || resolvePagedSwipeDirection(diffX);
        var resolvedPhysicalDirection = resolvePagedPhysicalDirection(diffX);
        var finalDirectionalDistance = resolvedPhysicalDirection === "next" ? diffX : -diffX;
        var finalSwipeDistance = Math.max(0, finalDirectionalDistance);
        var releaseProgress = Math.min(1, finalSwipeDistance / Math.max(120, pageWidth * 0.56));

        if (selectionTimeout) {
            clearTimeout(selectionTimeout);
        }
        resetSwipeFrame();
        setPagedSwipeLockState(false);
        resetPagedViewportScroll();
        var absDiffX = Math.abs(diffX);
        var absDiffY = Math.abs(diffY);
        var isPageModeTap = savedSettings && savedSettings.readingMode === "PAGE";
        var isTapLikeGesture = absDiffX < (isPageModeTap ? PAGE_TAP_THRESHOLD + 6 : DEFAULT_TAP_THRESHOLD) && absDiffY < (isPageModeTap ? PAGE_TAP_THRESHOLD + 6 : DEFAULT_TAP_THRESHOLD + 2);

        if (touchTargetImage && isTapLikeGesture && openTouchTargetImage()) {
            e.preventDefault();
            tapZoneDispatched = true;
            touchTargetImage = null;
            if (window.clearSwipePeek) {
                window.clearSwipePeek();
            }
            swipePreviewActive = false;
            swipeGestureLocked = false;
            lastSwipeDirection = null;
            lastSwipeProgress = 0;
            lastSwipeDistance = 0;
            swipeLockedDirection = null;
            return;
        }

        if (touchTargetLink && isTapLikeGesture) {
            touchTargetImage = null;
            touchTargetLink = null;
            swipePreviewActive = false;
            swipeGestureLocked = false;
            lastSwipeDirection = null;
            lastSwipeProgress = 0;
            lastSwipeDistance = 0;
            swipeLockedDirection = null;
            return;
        }

        if (isPageModeTap) {
            var pageSwipeAccepted =
                absDiffX > Math.max(30, absDiffY + 10) &&
                finalSwipeDistance > 34;

            if (pageSwipeAccepted && window.completeSwipeTurn) {
                log(
                    "Page swipe direction=" + resolvedSwipeDirection +
                    " diffX=" + Math.round(diffX) +
                    " diffY=" + Math.round(diffY) +
                    " releaseProgress=" + releaseProgress.toFixed(3) +
                    " threeD=" + allowThreeD
                );
                e.preventDefault();
                var usePreview = allowThreeD && swipePreviewActive;
                if (resolvedSwipeDirection === "next") {
                    window.completeSwipeTurn("next", function() {
                        if (rendition && rendition.next) {
                            rendition.next();
                        }
                    }, { usePreview: usePreview });
                } else {
                    window.completeSwipeTurn("prev", function() {
                        if (rendition && rendition.prev) {
                            rendition.prev();
                        }
                    }, { usePreview: usePreview });
                }
            } else {
                dispatchTapZoneFromTouch(e.changedTouches[0]);
                tapZoneDispatched = true;
                if (window.clearSwipePeek) {
                    window.clearSwipePeek();
                }
            }

            swipePreviewActive = false;
            swipeGestureLocked = false;
            lastSwipeDirection = null;
            lastSwipeProgress = 0;
            lastSwipeDistance = 0;
            swipeLockedDirection = null;
            touchTargetImage = null;
            touchTargetLink = null;
            return;
        }

        var tapThreshold = isPageMode ? PAGE_TAP_THRESHOLD : DEFAULT_TAP_THRESHOLD;
        var isTap = Math.abs(diffX) < tapThreshold && Math.abs(diffY) < tapThreshold && !swipeGestureLocked;
        var isNearTapInPageMode =
            isPageModeTap &&
            Math.abs(diffX) < (PAGE_TAP_THRESHOLD + 6) &&
            Math.abs(diffY) < (PAGE_TAP_THRESHOLD + 6) &&
            finalSwipeDistance < 40;
        var immediateSelection = contents.window.getSelection();
        var shouldRunSelectionDelay = true;

        if ((isTap || isNearTapInPageMode) && (isPageModeTap || !immediateSelection || immediateSelection.toString().trim().length === 0)) {
            e.preventDefault();
            dispatchTapZoneFromTouch(e.changedTouches[0]);
            tapZoneDispatched = true;
            if (isPageModeTap) {
                shouldRunSelectionDelay = false;
            }
        }

        if (shouldRunSelectionDelay) {
            selectionTimeout = setTimeout(function() {
                selectionTimeout = null;
                var selection = contents.window.getSelection();
                if (selection && selection.toString().trim().length > 0) {
                    try {
                        var range = selection.getRangeAt(0);
                        var cfiRange = contents.cfiFromRange(range);
                        if (cfiRange) {
                            triggerSelectionMenu(cfiRange, contents);
                            return;
                        }
                    } catch (err) {
                        log("Selection conversion warning: " + err.message);
                    }
                }

                if (!tapZoneDispatched && isTap && (isPageModeTap || (selection && selection.toString().trim().length === 0))) {
                    dispatchTapZoneFromTouch(e.changedTouches[0]);
                    tapZoneDispatched = true;
                }
            }, 100);
        }

        var horizontalPageSwipe =
            isPageMode &&
            (swipeLockedDirection || lastSwipeDirection || Math.abs(diffX) > 24) &&
            Math.abs(diffY) < 120 &&
            finalSwipeDistance > Math.max(52, Math.abs(diffY) + 12);

        if (horizontalPageSwipe) {
            log(
                "Page swipe direction=" + resolvedSwipeDirection +
                " diffX=" + Math.round(diffX) +
                " diffY=" + Math.round(diffY) +
                " releaseProgress=" + releaseProgress.toFixed(3) +
                " threeD=" + allowThreeD
            );
            if ((releaseProgress > 0.1 || finalSwipeDistance > 48) && window.completeSwipeTurn) {
                e.preventDefault();
                var usePreview = allowThreeD && swipePreviewActive;
                if (resolvedSwipeDirection === "next") {
                    window.completeSwipeTurn("next", function() {
                        if (rendition && rendition.next) {
                            rendition.next();
                        }
                    }, { usePreview: usePreview });
                } else {
                    window.completeSwipeTurn("prev", function() {
                        if (rendition && rendition.prev) {
                            rendition.prev();
                        }
                    }, { usePreview: usePreview });
                }
            } else if (allowThreeD && swipePreviewActive && window.cancelSwipeTurn) {
                window.cancelSwipeTurn();
            } else if (window.clearSwipePeek) {
                window.clearSwipePeek();
            }
        } else if (!tapZoneDispatched && isNearTapInPageMode) {
            dispatchTapZoneFromTouch(e.changedTouches[0]);
            tapZoneDispatched = true;
            if (window.clearSwipePeek) {
                window.clearSwipePeek();
            }
        } else if (window.clearSwipePeek) {
            window.clearSwipePeek();
        }
        swipePreviewActive = false;
        swipeGestureLocked = false;
        lastSwipeDirection = null;
        lastSwipeProgress = 0;
        lastSwipeDistance = 0;
        swipeLockedDirection = null;
        touchTargetImage = null;
        touchTargetLink = null;
    }, { passive: false });

    contents.document.addEventListener("touchcancel", function() {
        swipePreviewActive = false;
        swipeGestureLocked = false;
        lastSwipeDirection = null;
        lastSwipeProgress = 0;
        lastSwipeDistance = 0;
        swipeLockedDirection = null;
        touchTargetImage = null;
        touchTargetLink = null;
        tapZoneDispatched = false;
        resetSwipeFrame();
        setPagedSwipeLockState(false);
        resetPagedViewportScroll();
        if (window.cancelSwipeTurn) {
            window.cancelSwipeTurn();
        } else if (window.clearSwipePeek) {
            window.clearSwipePeek();
        }
    }, { passive: true });

    setupClickTapFallback(contents);
}
