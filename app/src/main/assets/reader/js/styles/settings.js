function collectStyledRenditions() {
    var previewRenditions = window.getPagedPreviewRenditions
        ? window.getPagedPreviewRenditions()
        : [];
    var seen = [];
    return [rendition].concat(previewRenditions).filter(function(targetRendition) {
        if (!targetRendition || seen.indexOf(targetRendition) !== -1) {
            return false;
        }
        seen.push(targetRendition);
        return true;
    });
}

window.applyStylesToRenditionContents = function(targetRendition) {
    if (!targetRendition || !targetRendition.getContents) {
        return;
    }
    try {
        targetRendition.getContents().forEach(function(content) {
            applyStylesToContents(content);
        });
    } catch (e) {
        log("Apply styles warning: " + e.message);
    }
};

function applyImageFilterToAllContents(imageFilterStyle) {
    collectStyledRenditions().forEach(function(targetRendition) {
        if (!targetRendition || !targetRendition.getContents) {
            return;
        }
        try {
            targetRendition.getContents().forEach(function(content) {
                applyImageFilterToContents(content, imageFilterStyle);
            });
        } catch (e) {
            log("Apply image filter warning: " + e.message);
        }
    });
}

window.applySettings = function(settings, isInitial) {
    var oldSettings = savedSettings;
    savedSettings = settings || {};
    applyReaderModeBodyState();
    if (window.resetPageTurnViewportState) {
        window.resetPageTurnViewportState();
    }
    var pagedMarginPx = (
        savedSettings.readingMode === "PAGE" &&
        !savedSettings.usePublisherStyle &&
        typeof getReaderViewportMarginPx === "function"
    )
        ? getReaderViewportMarginPx()
        : 0;
    var pagedMarginValue = pagedMarginPx + "px";
    document.documentElement.style.setProperty("--reader-page-horizontal-margin", pagedMarginValue);
    document.body.style.setProperty("--reader-page-horizontal-margin", pagedMarginValue);

    document.body.classList.remove("filter-invert", "filter-darken", "filter-bw");
    var effectiveFilter = savedSettings.imageFilter === "AUTO"
        ? resolveAutoImageFilterMode(savedSettings)
        : savedSettings.imageFilter;
    if (effectiveFilter === "INVERT") {
        document.body.classList.add("filter-invert");
    } else if (effectiveFilter === "DARKEN") {
        document.body.classList.add("filter-darken");
    } else if (effectiveFilter === "BW") {
        document.body.classList.add("filter-bw");
    }

    if (!isInitial && oldSettings && oldSettings.readingMode !== savedSettings.readingMode) {
        var current = typeof resolveCurrentLocationSafe === "function" ? resolveCurrentLocationSafe(rendition) : null;
        var cfi = current && current.start ? current.start.cfi : null;
        openBook(bookData, cfi);
        return;
    }

    var bgOverlay = document.getElementById("bg-overlay");
    if (savedSettings.readerTheme === "IMAGE" && savedSettings.backgroundImageUri) {
        var bridge = getBridge();
        var dataUrl = bridge && bridge.getBackgroundImageDataUrl
            ? bridge.getBackgroundImageDataUrl(savedSettings.backgroundImageUri)
            : "";

        if (dataUrl) {
            bgOverlay.style.backgroundImage = "url('" + dataUrl + "')";
            bgOverlay.style.display = "block";
            bgOverlay.style.filter = "blur(" + (savedSettings.backgroundImageBlur || 0) + "px)";
            bgOverlay.style.opacity = savedSettings.backgroundImageOpacity !== undefined
                ? savedSettings.backgroundImageOpacity
                : 1;
        } else {
            bgOverlay.style.display = "none";
        }
    } else {
        bgOverlay.style.display = "none";
        bgOverlay.style.backgroundImage = "none";
        document.body.style.backgroundColor = savedSettings.backgroundColor || "transparent";
    }

    var styledRenditions = collectStyledRenditions();
    styledRenditions.forEach(function(targetRendition) {
        window.applyStylesToRenditionContents(targetRendition);
    });
    if (styledRenditions.length > 0) {
        var liveEffectiveFilter = savedSettings.imageFilter === "AUTO"
            ? resolveAutoImageFilterMode(savedSettings)
            : savedSettings.imageFilter;
        var liveFilterStyle = "none";
        if (liveEffectiveFilter === "INVERT") {
            liveFilterStyle = "invert(100%)";
        } else if (liveEffectiveFilter === "DARKEN") {
            liveFilterStyle = "brightness(50%)";
        } else if (liveEffectiveFilter === "BW") {
            liveFilterStyle = "grayscale(100%)";
        }
        applyImageFilterToAllContents(liveFilterStyle);
    }

    setupReaderModePreviewRenderers();
    if (window.applyPagedPreviewSettings) {
        window.applyPagedPreviewSettings();
    }
    safeResize(rendition);
    if (isPageReadingMode() && savedSettings.pageTurn3d) {
        refreshAdjacentPreviews();
    }
};
