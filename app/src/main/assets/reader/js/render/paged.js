function usesPagedPreview() {
    return !!(
        savedSettings &&
        savedSettings.readingMode === "PAGE" &&
        savedSettings.pageTurn3d &&
        bookData
    );
}

function configurePagedRendition(targetRendition) {
    if (!targetRendition) {
        return;
    }
    try {
        if (typeof targetRendition.spread === "function") {
            targetRendition.spread("none");
        }
    } catch (e) {
        log("Paged spread warning: " + e.message);
    }
    try {
        if (typeof targetRendition.flow === "function") {
            targetRendition.flow("paginated");
        }
    } catch (e) {
        log("Paged flow warning: " + e.message);
    }
}

function previewContainerId(direction) {
    if (direction === "prev") {
        return "page-preview-prev";
    }
    if (direction === "next") {
        return "page-preview-next";
    }
    return "page-preview-current";
}

function resolveActivePageTransitionStyle() {
    if (typeof normalizePageTransitionStyle === "function") {
        return normalizePageTransitionStyle(savedSettings && savedSettings.pageTransitionStyle);
    }
    var rawStyle = String(
        (savedSettings && savedSettings.pageTransitionStyle) || "DEFAULT"
    ).toUpperCase();
    return rawStyle || "DEFAULT";
}

function createPreviewLayer(direction) {
    return {
        direction: direction,
        container: null,
        book: null,
        rendition: null,
        targetKey: "",
        ready: false,
        loading: false
    };
}

function PageTurnCompositor() {
    this.root = null;
    this.activeDirection = null;
    this.dragging = false;
    this.warmToken = 0;
    this.warmTimer = 0;
    this.cleanupTimer = 0;
    this.layers = {
        current: createPreviewLayer("current"),
        next: createPreviewLayer("next"),
        prev: createPreviewLayer("prev")
    };
}

PageTurnCompositor.prototype.isEnabled = function() {
    return usesPagedPreview();
};

PageTurnCompositor.prototype.getLayer = function(direction) {
    return this.layers[direction] || null;
};

PageTurnCompositor.prototype.ensureRoot = function() {
    if (this.root && this.root.isConnected) {
        return this.root;
    }
    this.root = document.getElementById("page-turn-compositor");
    if (!this.root) {
        this.root = document.createElement("div");
        this.root.id = "page-turn-compositor";
        document.body.appendChild(this.root);
    }
    return this.root;
};

PageTurnCompositor.prototype.ensureContainer = function(direction) {
    var layer = this.getLayer(direction);
    if (!layer) {
        return null;
    }
    if (layer.container && layer.container.isConnected) {
        return layer.container;
    }

    var root = this.ensureRoot();
    var container = document.getElementById(previewContainerId(direction));
    if (!container) {
        container = document.createElement("div");
        container.id = previewContainerId(direction);
        container.className = "page-preview page-preview-" + direction;
        root.appendChild(container);
    } else if (container.parentElement !== root) {
        root.appendChild(container);
    }

    layer.container = container;
    this.resetLayerVisuals(layer);
    this.setLayerVisible(layer, false);
    return container;
};

PageTurnCompositor.prototype.setLayerState = function(layer, ready, loading) {
    if (!layer || !layer.container) {
        return;
    }
    layer.ready = !!ready;
    layer.loading = !!loading;
    layer.container.dataset.ready = ready ? "true" : "false";
    layer.container.dataset.loading = loading ? "true" : "false";
};

PageTurnCompositor.prototype.setLayerVisible = function(layer, visible) {
    if (!layer || !layer.container) {
        return;
    }
    layer.container.dataset.visible = visible ? "true" : "false";
    layer.container.style.setProperty("--preview-opacity", visible ? "1" : "0");
};

PageTurnCompositor.prototype.resetLayerVisuals = function(layer) {
    if (!layer || !layer.container) {
        return;
    }
    layer.container.style.setProperty("--preview-translate-x", "0px");
    layer.container.style.setProperty("--preview-translate-y", "0px");
    layer.container.style.setProperty("--preview-scale", layer.direction === "current" ? "1" : "0.992");
    layer.container.style.setProperty("--preview-opacity", layer.direction === "current" ? "1" : "0");
    layer.container.style.setProperty("--preview-rotate-y", "0deg");
    layer.container.style.setProperty("--preview-rotate-z", "0deg");
    layer.container.style.setProperty("--preview-perspective", "1800px");
    layer.container.style.setProperty("--preview-brightness", "1");
    layer.container.style.setProperty("--preview-shadow-opacity", "0");
    layer.container.style.setProperty("--preview-clip-left", "0px");
    layer.container.style.setProperty("--preview-clip-right", "0px");
    layer.container.style.setProperty("--preview-origin", "center center");
    layer.container.classList.remove(
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
};

PageTurnCompositor.prototype.resolveBaseCfi = function() {
    var current = typeof resolveCurrentLocationSafe === "function"
        ? resolveCurrentLocationSafe(rendition)
        : null;
    if (!current || !current.start) {
        return null;
    }
    return current.start.cfi || (current.end && current.end.cfi) || null;
};

PageTurnCompositor.prototype.destroyLayer = function(layer) {
    if (!layer) {
        return;
    }
    try {
        if (layer.rendition) {
            layer.rendition.destroy();
        }
    } catch (e) {
        log("Preview rendition destroy warning (" + layer.direction + "): " + e.message);
    }
    try {
        if (layer.book) {
            layer.book.destroy();
        }
    } catch (e2) {
        log("Preview book destroy warning (" + layer.direction + "): " + e2.message);
    }
    if (layer.container) {
        layer.container.innerHTML = "";
        this.setLayerState(layer, false, false);
        this.resetLayerVisuals(layer);
        this.setLayerVisible(layer, false);
    }
    layer.book = null;
    layer.rendition = null;
    layer.targetKey = "";
    layer.ready = false;
    layer.loading = false;
};

PageTurnCompositor.prototype.ensureLayer = function(direction) {
    if (!this.isEnabled()) {
        return null;
    }
    var layer = this.getLayer(direction);
    if (!layer) {
        return null;
    }
    if (layer.rendition) {
        return layer;
    }

    var container = this.ensureContainer(direction);
    if (!container) {
        return null;
    }
    container.innerHTML = "";
    this.setLayerState(layer, false, false);

    try {
        var viewport = typeof getReaderViewportSize === "function"
            ? getReaderViewportSize()
            : {
                width: Math.max(1, window.innerWidth || document.documentElement.clientWidth || 1),
                height: Math.max(1, window.innerHeight || document.documentElement.clientHeight || 1)
            };
        layer.book = ePub(bookData);
        layer.rendition = layer.book.renderTo(previewContainerId(direction), {
            width: viewport.width,
            height: viewport.height,
            flow: "paginated",
            manager: "default",
            spread: "none",
            minSpreadWidth: 999999,
            allowScriptedContent: true
        });
        configurePagedRendition(layer.rendition);
    } catch (e) {
        log("Preview renderTo error (" + direction + "): " + e.message);
        layer.book = null;
        layer.rendition = null;
        return null;
    }

    layer.rendition.hooks.content.register(function(contents) {
        try {
            applyStylesToContents(contents);
            if (contents.window) {
                contents.window.AndroidReader = getBridge();
            }
            if (contents.document && contents.document.documentElement) {
                contents.document.documentElement.style.pointerEvents = "none";
            }
            if (contents.document && contents.document.body) {
                contents.document.body.style.pointerEvents = "none";
            }
        } catch (e) {
            log("Preview content hook error (" + direction + "): " + e.message);
        }
    });

    return layer;
};

PageTurnCompositor.prototype.displayLayer = function(direction, baseCfi, warmToken) {
    var self = this;
    var layer = this.ensureLayer(direction);
    if (!layer || !layer.rendition || !layer.book || !baseCfi) {
        return;
    }

    var targetKey = direction + ":" + baseCfi;
    if (
        layer.targetKey === targetKey &&
        (layer.ready || layer.loading)
    ) {
        return;
    }

    layer.targetKey = targetKey;
    this.setLayerState(layer, false, true);

    layer.book.ready
        .then(function() {
            return layer.rendition.display(baseCfi);
        })
        .then(function() {
            safeResize(layer.rendition);
            if (direction === "next" && layer.rendition.next) {
                return layer.rendition.next();
            }
            if (direction === "prev" && layer.rendition.prev) {
                return layer.rendition.prev();
            }
            return null;
        })
        .then(function() {
            if (warmToken !== self.warmToken || layer.targetKey !== targetKey) {
                return;
            }
            if (typeof window.applyStylesToRenditionContents === "function") {
                window.applyStylesToRenditionContents(layer.rendition);
            }
            self.setLayerState(layer, true, false);
        })
        .catch(function(e) {
            if (warmToken === self.warmToken && layer.targetKey === targetKey) {
                self.setLayerState(layer, false, false);
            }
            log("Preview display warning (" + direction + "): " + e.message);
        });
};

PageTurnCompositor.prototype.invalidateTargets = function() {
    var self = this;
    this.warmToken += 1;
    ["current", "next", "prev"].forEach(function(direction) {
        var layer = self.getLayer(direction);
        if (!layer) {
            return;
        }
        layer.targetKey = "";
        if (layer.container) {
            self.setLayerState(layer, false, false);
        }
    });
};

PageTurnCompositor.prototype.warm = function() {
    if (!this.isEnabled()) {
        this.destroy();
        return;
    }

    var baseCfi = this.resolveBaseCfi();
    if (!baseCfi) {
        return;
    }

    this.ensureLayer("current");
    this.ensureLayer("next");
    this.ensureLayer("prev");

    var warmToken = ++this.warmToken;
    this.displayLayer("current", baseCfi, warmToken);
    this.displayLayer("next", baseCfi, warmToken);
    this.displayLayer("prev", baseCfi, warmToken);
};

PageTurnCompositor.prototype.scheduleWarm = function(delayMs) {
    var self = this;
    if (!this.isEnabled()) {
        return;
    }
    if (this.warmTimer) {
        clearTimeout(this.warmTimer);
    }
    this.warmTimer = window.setTimeout(function() {
        self.warmTimer = 0;
        self.warm();
    }, delayMs || 120);
};

PageTurnCompositor.prototype.getRenditions = function() {
    return [
        this.layers.current.rendition,
        this.layers.next.rendition,
        this.layers.prev.rendition
    ].filter(function(item) {
        return !!item;
    });
};

PageTurnCompositor.prototype.isReady = function(direction) {
    var currentLayer = this.getLayer("current");
    var targetLayer = this.getLayer(direction);
    return !!(
        this.isEnabled() &&
        currentLayer &&
        targetLayer &&
        currentLayer.ready &&
        targetLayer.ready
    );
};

PageTurnCompositor.prototype.show = function(direction) {
    var currentLayer = this.getLayer("current");
    var targetLayer = this.getLayer(direction);
    var inactiveLayer = this.getLayer(direction === "next" ? "prev" : "next");
    if (!currentLayer || !targetLayer || !currentLayer.container || !targetLayer.container) {
        return;
    }
    document.body.classList.add("compositor-active", "preview-visible");
    document.body.classList.toggle("preview-next", direction === "next");
    document.body.classList.toggle("preview-prev", direction === "prev");
    this.setLayerVisible(currentLayer, true);
    this.setLayerVisible(targetLayer, true);
    this.resetLayerVisuals(currentLayer);
    this.resetLayerVisuals(targetLayer);
    currentLayer.container.style.setProperty("--preview-scale", "1");
    if (inactiveLayer && inactiveLayer.container) {
        this.resetLayerVisuals(inactiveLayer);
        this.setLayerVisible(inactiveLayer, false);
    }
};

PageTurnCompositor.prototype.resetVisualState = function(immediate) {
    var self = this;
    if (this.cleanupTimer) {
        clearTimeout(this.cleanupTimer);
        this.cleanupTimer = 0;
    }
    this.dragging = false;
    this.activeDirection = null;
    document.body.classList.remove(
        "dragging-page",
        "preview-next",
        "preview-prev",
        "preview-visible",
        "compositor-active",
        "turning-next",
        "turning-prev"
    );
    ["current", "next", "prev"].forEach(function(direction) {
        var layer = self.getLayer(direction);
        if (!layer || !layer.container) {
            return;
        }
        if (immediate) {
            layer.container.style.transition = "none";
        }
        self.resetLayerVisuals(layer);
        self.setLayerVisible(layer, false);
        if (immediate) {
            void layer.container.offsetWidth;
            layer.container.style.transition = "";
        }
    });
};

PageTurnCompositor.prototype.begin = function(direction) {
    if (!this.isReady(direction)) {
        this.scheduleWarm(60);
        return false;
    }
    this.activeDirection = direction;
    this.dragging = true;
    this.show(direction);
    document.body.classList.add("dragging-page");
    return true;
};

PageTurnCompositor.prototype.update = function(progressPx) {
    if (!this.activeDirection || !this.isReady(this.activeDirection)) {
        return false;
    }

    var viewport = typeof getReaderViewportSize === "function"
        ? getReaderViewportSize()
        : { width: window.innerWidth || 360, height: window.innerHeight || 640 };
    var width = viewport.width || 360;
    var distancePx = Math.max(0, Math.min(width * 0.96, progressPx || 0));
    var progress = Math.max(0, Math.min(1, distancePx / Math.max(140, width * 0.92)));
    var eased = 1 - Math.pow(1 - progress, 1.08);
    var direction = this.activeDirection;
    var signed = direction === "next" ? -1 : 1;
    var transitionStyle = resolveActivePageTransitionStyle();
    var paperMode = transitionStyle === "PAPER";
    var flipMode = transitionStyle === "FLIP";
    var cubeMode = transitionStyle === "CUBE";
    var rollMode = transitionStyle === "ROLL";
    var tiltMode = transitionStyle === "TILT";
    var cardMode = transitionStyle === "CARD";
    var currentLayer = this.getLayer("current");
    var targetLayer = this.getLayer(direction);
    var inactiveLayer = this.getLayer(direction === "next" ? "prev" : "next");
    if (!currentLayer || !targetLayer || !currentLayer.container || !targetLayer.container) {
        return false;
    }

    this.show(direction);
    document.body.classList.add("dragging-page");

    var revealWidth = Math.min(width - 36, Math.max(22, distancePx * (paperMode ? 0.98 : 0.92)));
    var currentTranslate = signed * Math.min(width * (paperMode ? 0.3 : 0.78), distancePx * (paperMode ? 0.26 : 0.82));
    var currentRotateY = paperMode
        ? signed * Math.min(22, eased * 22)
        : flipMode
            ? signed * Math.min(18, eased * 18)
            : cubeMode
                ? signed * Math.min(14, eased * 14)
                : signed * Math.min(4, eased * 4);
    var currentRotateZ = rollMode
        ? signed * Math.min(2.8, eased * 2.8)
        : tiltMode
            ? signed * Math.min(1.4, eased * 1.4)
            : paperMode
                ? signed * Math.min(1.6, eased * 1.6)
                : 0;
    var currentScale = paperMode
        ? 1 - (eased * 0.018)
        : cardMode
            ? 1 - (eased * 0.1)
            : 1 - (eased * 0.07);
    var currentOpacity = paperMode ? "1" : (1 - (eased * 0.18)).toFixed(4);
    var targetTranslate = -signed * Math.max(
        0,
        (1 - eased) * (paperMode ? 38 : width * (cubeMode ? 0.18 : 0.22))
    );
    var targetScale = paperMode ? 0.985 + (eased * 0.015) : 0.9 + (eased * 0.1);
    var targetRotateY = cubeMode
        ? -signed * Math.max(0, (1 - eased) * 18)
        : flipMode
            ? -signed * Math.max(0, (1 - eased) * 10)
            : paperMode
                ? -signed * Math.max(0, (1 - eased) * 6)
                : 0;
    var targetBrightness = paperMode ? 0.955 + (eased * 0.045) : 0.93 + (eased * 0.07);
    var targetOpacity = paperMode ? 0.82 + (eased * 0.18) : 0.76 + (eased * 0.24);
    var shadowOpacity = paperMode ? 0.12 + (eased * 0.24) : 0.08 + (eased * 0.22);
    var perspective = paperMode ? "2400px" : flipMode || cubeMode ? "1500px" : "2200px";

    currentLayer.container.style.setProperty("--preview-origin", direction === "next" ? "left center" : "right center");
    currentLayer.container.style.setProperty("--preview-perspective", perspective);
    currentLayer.container.style.setProperty("--preview-translate-x", currentTranslate.toFixed(2) + "px");
    currentLayer.container.style.setProperty("--preview-scale", currentScale.toFixed(4));
    currentLayer.container.style.setProperty("--preview-opacity", currentOpacity);
    currentLayer.container.style.setProperty("--preview-rotate-y", currentRotateY.toFixed(2) + "deg");
    currentLayer.container.style.setProperty("--preview-rotate-z", currentRotateZ.toFixed(2) + "deg");
    currentLayer.container.style.setProperty("--preview-shadow-opacity", shadowOpacity.toFixed(4));
    currentLayer.container.style.setProperty("--preview-clip-left", direction === "prev" ? revealWidth.toFixed(2) + "px" : "0px");
    currentLayer.container.style.setProperty("--preview-clip-right", direction === "next" ? revealWidth.toFixed(2) + "px" : "0px");

    targetLayer.container.style.setProperty("--preview-origin", "center center");
    targetLayer.container.style.setProperty("--preview-perspective", perspective);
    targetLayer.container.style.setProperty("--preview-translate-x", targetTranslate.toFixed(2) + "px");
    targetLayer.container.style.setProperty("--preview-scale", targetScale.toFixed(4));
    targetLayer.container.style.setProperty("--preview-rotate-y", targetRotateY.toFixed(2) + "deg");
    targetLayer.container.style.setProperty("--preview-brightness", targetBrightness.toFixed(4));
    targetLayer.container.style.setProperty("--preview-opacity", targetOpacity.toFixed(4));

    if (inactiveLayer && inactiveLayer.container) {
        this.setLayerVisible(inactiveLayer, false);
    }
    return true;
};

PageTurnCompositor.prototype.cancel = function(immediate) {
    var self = this;
    if (!this.activeDirection) {
        this.resetVisualState(true);
        return;
    }
    document.body.classList.remove("dragging-page");
    if (immediate) {
        this.resetVisualState(true);
        return;
    }

    ["current", "next", "prev"].forEach(function(direction) {
        var layer = self.getLayer(direction);
        if (!layer || !layer.container) {
            return;
        }
        layer.container.style.transition = "transform 180ms cubic-bezier(0.22, 1, 0.36, 1), clip-path 180ms cubic-bezier(0.22, 1, 0.36, 1), opacity 120ms ease-out, filter 120ms ease-out";
    });
    this.resetLayerVisuals(this.getLayer("current"));
    this.resetLayerVisuals(this.getLayer("next"));
    this.resetLayerVisuals(this.getLayer("prev"));

    this.cleanupTimer = window.setTimeout(function() {
        self.resetVisualState(true);
    }, 190);
};

PageTurnCompositor.prototype.commit = function(direction, action, options) {
    var self = this;
    var transitionStyle = options && options.style ? options.style : "DEFAULT";
    var duration = options && options.duration ? options.duration : 180;
    var swap = options && options.swap ? options.swap : 90;

    if (!this.isReady(direction)) {
        this.resetVisualState(true);
        action();
        return;
    }

    var currentLayer = this.getLayer("current");
    if (!currentLayer || !currentLayer.container) {
        action();
        return;
    }

    this.activeDirection = direction;
    this.show(direction);
    document.body.classList.remove("dragging-page");
    document.body.classList.add(direction === "next" ? "turning-next" : "turning-prev");

    var viewerClass = direction === "next" ? "page-turning-next" : "page-turning-prev";
    var transitionClass = "transition-" + String(transitionStyle || "DEFAULT").toLowerCase() + "-" + direction;
    currentLayer.container.classList.add(viewerClass, transitionClass);

    window.requestAnimationFrame(function() {
        setTimeout(function() {
            try {
                action();
            } catch (e) {
                log("Page turn action error: " + e.message);
            }
        }, swap);
    });

    this.cleanupTimer = window.setTimeout(function() {
        currentLayer.container.classList.remove(viewerClass, transitionClass);
        self.resetVisualState(true);
        self.invalidateTargets();
        self.scheduleWarm(80);
    }, duration);
};

PageTurnCompositor.prototype.resize = function() {
    if (!this.isEnabled()) {
        this.destroy();
        return;
    }
    this.getRenditions().forEach(function(targetRendition) {
        safeResize(targetRendition);
    });
    this.scheduleWarm(90);
};

PageTurnCompositor.prototype.applySettings = function() {
    this.getRenditions().forEach(function(targetRendition) {
        if (typeof window.applyStylesToRenditionContents === "function") {
            window.applyStylesToRenditionContents(targetRendition);
        }
        safeResize(targetRendition);
    });
    this.invalidateTargets();
};

PageTurnCompositor.prototype.destroy = function() {
    if (this.warmTimer) {
        clearTimeout(this.warmTimer);
        this.warmTimer = 0;
    }
    if (this.cleanupTimer) {
        clearTimeout(this.cleanupTimer);
        this.cleanupTimer = 0;
    }
    this.warmToken += 1;
    this.resetVisualState(true);
    this.destroyLayer(this.layers.current);
    this.destroyLayer(this.layers.next);
    this.destroyLayer(this.layers.prev);
};

function ensurePageTurnCompositor() {
    if (!pageTurnCompositor) {
        pageTurnCompositor = new PageTurnCompositor();
    }
    return pageTurnCompositor;
}

function hidePreviewLayer() {
    ensurePageTurnCompositor().resetVisualState(true);
}

function resetPreviewState() {
    ensurePageTurnCompositor().invalidateTargets();
}

function destroyPreviewRenditions() {
    ensurePageTurnCompositor().destroy();
}

function refreshAdjacentPreviews() {
    ensurePageTurnCompositor().warm();
}

function ensurePreviewRendition(direction) {
    return ensurePageTurnCompositor().ensureLayer(direction);
}

function scheduleAdjacentPreviewWarmup() {
    ensurePageTurnCompositor().scheduleWarm(140);
}

function updatePreviewPeek(direction, progress, dragDistancePx) {
    var compositor = ensurePageTurnCompositor();
    if (!compositor.begin(direction)) {
        return false;
    }
    var viewport = typeof getReaderViewportSize === "function"
        ? getReaderViewportSize()
        : { width: window.innerWidth || 360, height: window.innerHeight || 640 };
    return compositor.update(dragDistancePx || ((viewport.width || 360) * (progress || 0)));
}

function activatePagedPreviewCompositor(direction) {
    return ensurePageTurnCompositor().begin(direction);
}

function getCompositorStage() {
    return ensurePageTurnCompositor().ensureContainer("current");
}

window.activatePagedPreviewCompositor = activatePagedPreviewCompositor;
window.getPagedPreviewCompositorStage = getCompositorStage;
window.getPagedPreviewRenditions = function() {
    return ensurePageTurnCompositor().getRenditions();
};
window.getPagedPreviewRendition = function(direction) {
    var layer = ensurePageTurnCompositor().getLayer(direction);
    return layer ? layer.rendition : null;
};
window.schedulePagedPreviewWarmup = function(delayMs) {
    ensurePageTurnCompositor().scheduleWarm(delayMs || 60);
};
window.beginPagedPreviewGesture = function(direction) {
    return ensurePageTurnCompositor().begin(direction);
};
window.updatePagedPreviewGesture = function(direction, dragDistancePx) {
    var compositor = ensurePageTurnCompositor();
    if (!compositor.activeDirection) {
        if (!compositor.begin(direction)) {
            return false;
        }
    }
    return compositor.update(dragDistancePx);
};
window.cancelPagedPreviewGesture = function(immediate) {
    ensurePageTurnCompositor().cancel(!!immediate);
};
window.commitPagedPreviewGesture = function(direction, action, options) {
    ensurePageTurnCompositor().commit(direction, action, options || {});
};
window.resizePagedPreviewCompositor = function() {
    ensurePageTurnCompositor().resize();
};
window.applyPagedPreviewSettings = function() {
    ensurePageTurnCompositor().applySettings();
};
window.isPagedPreviewReady = function(direction) {
    return ensurePageTurnCompositor().isReady(direction);
};
