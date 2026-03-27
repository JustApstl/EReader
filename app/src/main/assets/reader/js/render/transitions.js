function getPageTurnStage() {
    var compositorStage = window.getPagedPreviewCompositorStage ? window.getPagedPreviewCompositorStage() : null;
    if (compositorStage && document.body.classList.contains("compositor-active")) {
        return compositorStage;
    }
    return document.getElementById("viewer-stage") || document.getElementById("viewer");
}

function normalizePageTransitionStyle(rawStyle) {
    var style = String(rawStyle || "DEFAULT").toUpperCase();
    var supported = {
        DEFAULT: true,
        TILT: true,
        CARD: true,
        FLIP: true,
        CUBE: true,
        ROLL: true,
        PAPER: true
    };
    return supported[style] ? style : "DEFAULT";
}

function getTransitionTiming(style) {
    switch (style) {
        case "PAPER":
            return { duration: 208, swap: 102 };
        case "FLIP":
            return { duration: 188, swap: 92 };
        case "CUBE":
            return { duration: 184, swap: 90 };
        case "ROLL":
            return { duration: 180, swap: 88 };
        case "CARD":
            return { duration: 176, swap: 86 };
        case "TILT":
            return { duration: 170, swap: 84 };
        default:
            return { duration: 164, swap: 80 };
    }
}

function clearTurnClasses(viewer) {
    if (!viewer) {
        return;
    }
    viewer.classList.remove(
        "page-turning-next",
        "page-turning-prev",
        "transition-default-next",
        "transition-default-prev",
        "transition-tilt-next",
        "transition-tilt-prev",
        "transition-card-next",
        "transition-card-prev",
        "transition-flip-next",
        "transition-flip-prev",
        "transition-cube-next",
        "transition-cube-prev",
        "transition-roll-next",
        "transition-roll-prev",
        "transition-paper-next",
        "transition-paper-prev"
    );
}

var fallbackPageTurnCleanupTimer = 0;

function runFallbackPageTurn(direction, action, options) {
    var stage = document.getElementById("viewer-stage");
    var transitionStyle = normalizePageTransitionStyle(options && options.style);
    var duration = options && options.duration ? options.duration : 164;
    var swap = options && options.swap ? options.swap : 80;
    var directionClass = direction === "next" ? "page-turning-next" : "page-turning-prev";
    var transitionClass = "transition-" + transitionStyle.toLowerCase() + "-" + direction;
    var actionExecuted = false;

    if (!stage) {
        action();
        return;
    }

    if (fallbackPageTurnCleanupTimer) {
        clearTimeout(fallbackPageTurnCleanupTimer);
        fallbackPageTurnCleanupTimer = 0;
    }

    clearTurnClasses(stage);
    document.body.classList.remove("turning-next", "turning-prev");
    stage.style.setProperty("--stage-turn-duration", duration + "ms");

    window.requestAnimationFrame(function() {
        document.body.classList.add(direction === "next" ? "turning-next" : "turning-prev");
        stage.classList.add(directionClass, transitionClass);
    });

    window.setTimeout(function() {
        if (actionExecuted) {
            return;
        }
        actionExecuted = true;
        try {
            action();
        } catch (e) {
            log("Fallback page turn action error: " + e.message);
        }
    }, swap);

    fallbackPageTurnCleanupTimer = window.setTimeout(function() {
        clearTurnClasses(stage);
        document.body.classList.remove("turning-next", "turning-prev");
        stage.style.removeProperty("--stage-turn-duration");
        fallbackPageTurnCleanupTimer = 0;
    }, duration + 24);
}

function resetPageTurnViewportState() {
    activePeekDirection = null;
    activePeekProgress = 0;
    clearTurnClasses(document.getElementById("viewer-stage"));
    clearTurnClasses(document.getElementById("page-preview-current"));
    if (window.cancelPagedPreviewGesture) {
        window.cancelPagedPreviewGesture(true);
    } else {
        hidePreviewLayer();
    }
}

function clearSwipePeek() {
    activePeekDirection = null;
    activePeekProgress = 0;
    if (window.cancelPagedPreviewGesture) {
        window.cancelPagedPreviewGesture(false);
    } else {
        resetPageTurnViewportState();
    }
}

window.resetPageTurnViewportState = resetPageTurnViewportState;

function applySwipePose(direction, progress, dragging, dragDistancePx, point) {
    if (!savedSettings || !savedSettings.pageTurn3d || savedSettings.readingMode !== "PAGE") {
        return false;
    }
    if (!dragging) {
        return false;
    }
    if (!window.updatePagedPreviewGesture) {
        return false;
    }
    activePeekDirection = direction;
    activePeekProgress = Math.max(0, Math.min(1, progress || 0));
    return !!window.updatePagedPreviewGesture(direction, dragDistancePx || 0, point || null);
}

function updateSwipePeek(direction, progress, dragDistancePx, point) {
    return applySwipePose(direction, progress, true, dragDistancePx, point);
}

function cancelSwipeTurn() {
    clearSwipePeek();
}

function animatePageTurn(direction, action, options) {
    if (!savedSettings || savedSettings.readingMode !== "PAGE") {
        action();
        return;
    }

    var transitionStyle = normalizePageTransitionStyle(savedSettings.pageTransitionStyle);
    var timing = getTransitionTiming(transitionStyle);
    var wantsPreview = !!(
        savedSettings.pageTurn3d &&
        (!options || options.usePreview !== false) &&
        window.commitPagedPreviewGesture &&
        window.isPagedPreviewReady &&
        window.isPagedPreviewReady(direction)
    );

    if (wantsPreview) {
        window.commitPagedPreviewGesture(direction, action, {
            style: transitionStyle,
            duration: timing.duration,
            swap: timing.swap
        });
        return;
    }

    resetPageTurnViewportState();
    runFallbackPageTurn(direction, action, {
        style: transitionStyle,
        duration: timing.duration,
        swap: timing.swap
    });
}

function completeSwipeTurn(direction, action, options) {
    animatePageTurn(direction, action, options || {});
}

window.cancelSwipeTurn = cancelSwipeTurn;
window.completeSwipeTurn = completeSwipeTurn;

window.prevPage = function() {
    if (!rendition) {
        return;
    }
    animatePageTurn("prev", function() {
        rendition.prev();
    }, { usePreview: true });
};

window.nextPage = function() {
    if (!rendition) {
        return;
    }
    animatePageTurn("next", function() {
        rendition.next();
    }, { usePreview: true });
};
