function isPageReadingMode() {
    return !!(savedSettings && savedSettings.readingMode === "PAGE");
}

function isScrolledReadingMode() {
    return !isPageReadingMode();
}

function disableContinuousScrollAnchoring(targetRendition) {
    if (!targetRendition || !isScrolledReadingMode()) {
        return;
    }

    try {
        var manager = targetRendition.manager;
        var container = manager && manager.container ? manager.container : null;
        var stage = manager && manager.stage && manager.stage.element ? manager.stage.element : null;

        if (container && container.style) {
            container.style.setProperty("overflow-anchor", "none", "important");
        }

        if (stage && stage.style) {
            stage.style.setProperty("overflow-anchor", "none", "important");
        }
    } catch (e) {
        log("Continuous anchoring warning: " + e.message);
    }
}

function getReaderViewportMarginPx() {
    if (!isPageReadingMode() || !savedSettings || savedSettings.usePublisherStyle) {
        return 0;
    }
    var rawMargin = (savedSettings.margin !== undefined && savedSettings.margin !== null)
        ? Number(savedSettings.margin)
        : 20;
    if (!isFinite(rawMargin)) {
        rawMargin = 20;
    }
    var maxMargin = Math.max(0, ((window.innerWidth || 360) - 220) / 2);
    return Math.max(0, Math.min(rawMargin, maxMargin));
}

function getReaderViewportSize() {
    var height = Math.max(1, window.innerHeight || document.documentElement.clientHeight || 1);
    if (!isPageReadingMode()) {
        return {
            width: Math.max(1, window.innerWidth || document.documentElement.clientWidth || 1),
            height: height
        };
    }

    var margin = getReaderViewportMarginPx();
    var width = Math.max(
        220,
        (window.innerWidth || document.documentElement.clientWidth || 1) - (margin * 2)
    );
    return { width: width, height: height };
}

function resolveReaderRenditionOptions() {
    var isPageMode = isPageReadingMode();
    var viewport = getReaderViewportSize();
    return {
        width: viewport.width,
        height: viewport.height,
        flow: isPageMode ? "paginated" : "scrolled",
        manager: isPageMode ? "default" : "continuous",
        spread: isPageMode ? "none" : undefined,
        minSpreadWidth: isPageMode ? 999999 : undefined,
        allowScriptedContent: true
    };
}

function applyReaderModeBodyState() {
    var isPageMode = isPageReadingMode();
    document.body.classList.toggle("is-scrolled", !isPageMode);
    document.body.classList.toggle("is-paged", isPageMode);
}

function prepareScrolledRendition(targetRendition) {
    if (!targetRendition || !isScrolledReadingMode()) {
        return;
    }
    try {
        if (typeof targetRendition.flow === "function") {
            targetRendition.flow("scrolled");
        }
    } catch (e) {
        log("Scrolled flow warning: " + e.message);
    }
    disableContinuousScrollAnchoring(targetRendition);
}

function prepareReaderRendition(targetRendition) {
    if (isPageReadingMode()) {
        configurePagedRendition(targetRendition);
        return;
    }
    prepareScrolledRendition(targetRendition);
    destroyPreviewRenditions();
}

function setupReaderModePreviewRenderers() {
    if (isPageReadingMode() && savedSettings && savedSettings.pageTurn3d) {
        refreshAdjacentPreviews();
        scheduleAdjacentPreviewWarmup();
        return;
    }
    destroyPreviewRenditions();
}

function handleReaderRelocated(location) {
    if (window.resetPageTurnViewportState) {
        window.resetPageTurnViewportState();
    }
    if (initialJumpDone) {
        updateProgress(location);
        updatePagination();
    }
    scheduleActiveMenuPositionUpdate();
    if (isPageReadingMode()) {
        resetPreviewState();
        scheduleAdjacentPreviewWarmup();
    } else {
        disableContinuousScrollAnchoring(rendition);
        destroyPreviewRenditions();
    }
    if (listenActive && window.prepareTtsWords) {
        window.prepareTtsWords();
    }
    if (listenActive && window.scheduleListenExtraction) {
        window.scheduleListenExtraction();
    }
}

function handleReaderResize() {
    safeResize(rendition);
    if (isPageReadingMode()) {
        if (window.resizePagedPreviewCompositor) {
            window.resizePagedPreviewCompositor();
        } else {
            scheduleAdjacentPreviewWarmup();
        }
    } else {
        disableContinuousScrollAnchoring(rendition);
    }
    updatePagination();
    scheduleActiveMenuPositionUpdate();
}
