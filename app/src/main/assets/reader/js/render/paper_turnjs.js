(function initTurnJsPaperLayer() {
    function resolveActivePageTransitionStyle() {
        if (typeof normalizePageTransitionStyle === "function") {
            return normalizePageTransitionStyle(savedSettings && savedSettings.pageTransitionStyle);
        }
        return String((savedSettings && savedSettings.pageTransitionStyle) || "DEFAULT").toUpperCase();
    }

    function supportsTurnJsPaper() {
        return !!(window.jQuery && window.jQuery.fn && window.jQuery.fn.turn);
    }

    function injectBaseHref(html, baseHref) {
        if (!html || !baseHref || html.indexOf("<base ") !== -1) {
            return html;
        }
        var headMatch = html.match(/<head([^>]*)>/i);
        if (!headMatch) {
            return html;
        }
        var baseTag = '<base href="' + String(baseHref).replace(/"/g, "&quot;") + '">';
        return html.replace(headMatch[0], headMatch[0] + baseTag);
    }

    function snapshotHtmlFromRendition(targetRendition) {
        if (!targetRendition || !targetRendition.getContents) {
            return "";
        }
        try {
            var contents = targetRendition.getContents();
            if (!contents || !contents.length || !contents[0].document || !contents[0].document.documentElement) {
                return "";
            }
            var doc = contents[0].document;
            var html = "<!DOCTYPE html>\n" + doc.documentElement.outerHTML;
            return injectBaseHref(html, doc.baseURI || "");
        } catch (e) {
            log("Turn.js snapshot warning: " + e.message);
            return "";
        }
    }

    function createSnapshotPage(html, pageClassName) {
        var page = document.createElement("div");
        page.className = "paper-turn-page " + (pageClassName || "");

        var frame = document.createElement("iframe");
        frame.className = "paper-turn-frame";
        frame.setAttribute("tabindex", "-1");
        frame.setAttribute("aria-hidden", "true");
        frame.srcdoc = html;

        page.appendChild(frame);
        return page;
    }

    function buildTouchList(target, point, identifier) {
        var x = point.x;
        var y = point.y;

        if (window.Touch) {
            try {
                return [
                    new window.Touch({
                        identifier: identifier,
                        target: target,
                        clientX: x,
                        clientY: y,
                        screenX: x,
                        screenY: y,
                        pageX: x + (window.scrollX || 0),
                        pageY: y + (window.scrollY || 0),
                        radiusX: 1,
                        radiusY: 1,
                        rotationAngle: 0,
                        force: 0.5
                    })
                ];
            } catch (e) {
                // Fall through to a touch-like object.
            }
        }

        return [{
            identifier: identifier,
            target: target,
            clientX: x,
            clientY: y,
            screenX: x,
            screenY: y,
            pageX: x + (window.scrollX || 0),
            pageY: y + (window.scrollY || 0)
        }];
    }

    function dispatchSyntheticTouch(target, type, point, identifier) {
        if (!target || !point) {
            return false;
        }

        var touches = type === "touchend" ? [] : buildTouchList(target, point, identifier);
        var changedTouches = buildTouchList(target, point, identifier);
        var event = null;

        if (window.TouchEvent) {
            try {
                event = new window.TouchEvent(type, {
                    bubbles: true,
                    cancelable: true,
                    touches: touches,
                    targetTouches: touches,
                    changedTouches: changedTouches
                });
            } catch (e) {
                event = null;
            }
        }

        if (!event) {
            event = document.createEvent("Event");
            event.initEvent(type, true, true);
            try {
                Object.defineProperty(event, "touches", { value: touches });
                Object.defineProperty(event, "targetTouches", { value: touches });
                Object.defineProperty(event, "changedTouches", { value: changedTouches });
            } catch (e2) {
                event.touches = touches;
                event.targetTouches = touches;
                event.changedTouches = changedTouches;
            }
        }

        return target.dispatchEvent(event);
    }

    function TurnJsPaperController() {
        this.overlay = null;
        this.host = null;
        this.turnRoot = null;
        this.turnEl = null;
        this.direction = null;
        this.active = false;
        this.committing = false;
        this.pendingAction = null;
        this.expectedPage = null;
        this.startPage = null;
        this.touchId = 0;
        this.startPoint = null;
        this.lastPoint = null;
        this.releaseTimer = 0;
        this.cleanupTimer = 0;
    }

    TurnJsPaperController.prototype.clearTimers = function() {
        if (this.releaseTimer) {
            clearTimeout(this.releaseTimer);
            this.releaseTimer = 0;
        }
        if (this.cleanupTimer) {
            clearTimeout(this.cleanupTimer);
            this.cleanupTimer = 0;
        }
    };

    TurnJsPaperController.prototype.ensureOverlay = function() {
        if (this.overlay && this.overlay.isConnected && this.host && this.host.isConnected) {
            return this.host;
        }
        this.overlay = document.getElementById("paper-turn-overlay");
        if (!this.overlay) {
            this.overlay = document.createElement("div");
            this.overlay.id = "paper-turn-overlay";
            document.body.appendChild(this.overlay);
        }
        this.host = this.overlay.querySelector(".paper-turn-host");
        if (!this.host) {
            this.host = document.createElement("div");
            this.host.className = "paper-turn-host";
            this.overlay.appendChild(this.host);
        }
        return this.host;
    };

    TurnJsPaperController.prototype.canBegin = function(direction) {
        if (!supportsTurnJsPaper() || !savedSettings || !rendition) {
            return false;
        }
        if (savedSettings.readingMode !== "PAGE" || !savedSettings.pageTurn3d) {
            return false;
        }
        if (resolveActivePageTransitionStyle() !== "PAPER") {
            return false;
        }
        var targetRendition = window.getPagedPreviewRendition ? window.getPagedPreviewRendition(direction) : null;
        return !!(snapshotHtmlFromRendition(rendition) && snapshotHtmlFromRendition(targetRendition));
    };

    TurnJsPaperController.prototype.buildPages = function(direction) {
        var currentHtml = snapshotHtmlFromRendition(rendition);
        var targetRendition = window.getPagedPreviewRendition ? window.getPagedPreviewRendition(direction) : null;
        var targetHtml = snapshotHtmlFromRendition(targetRendition);
        if (!currentHtml || !targetHtml) {
            return null;
        }

        if (direction === "next") {
            return {
                expectedPage: 2,
                startPage: 1,
                pages: [
                    createSnapshotPage(currentHtml, "paper-turn-current"),
                    createSnapshotPage(targetHtml, "paper-turn-target")
                ]
            };
        }

        return {
            expectedPage: 1,
            startPage: 2,
            pages: [
                createSnapshotPage(targetHtml, "paper-turn-target"),
                createSnapshotPage(currentHtml, "paper-turn-current")
            ]
        };
    };

    TurnJsPaperController.prototype.resolveViewport = function() {
        return typeof getReaderViewportSize === "function"
            ? getReaderViewportSize()
            : {
                width: Math.max(1, window.innerWidth || document.documentElement.clientWidth || 1),
                height: Math.max(1, window.innerHeight || document.documentElement.clientHeight || 1)
            };
    };

    TurnJsPaperController.prototype.resolveRect = function() {
        return this.host ? this.host.getBoundingClientRect() : null;
    };

    TurnJsPaperController.prototype.resolveGlobalPoint = function(localPoint) {
        var rect = this.resolveRect();
        if (!rect) {
            return localPoint;
        }
        return {
            x: rect.left + localPoint.x,
            y: rect.top + localPoint.y
        };
    };

    TurnJsPaperController.prototype.resolveLocalPoint = function(globalPoint) {
        var rect = this.resolveRect();
        if (!rect) {
            return { x: 0, y: 0 };
        }
        return {
            x: globalPoint.x - rect.left,
            y: globalPoint.y - rect.top
        };
    };

    TurnJsPaperController.prototype.resolveCornerLocalPoint = function(globalPoint) {
        var viewport = this.resolveViewport();
        var local = this.resolveLocalPoint(globalPoint);
        var vertical = local.y < viewport.height * 0.5 ? "t" : "b";
        return {
            x: this.direction === "next" ? Math.max(8, viewport.width - 14) : 14,
            y: vertical === "t" ? 14 : Math.max(8, viewport.height - 14)
        };
    };

    TurnJsPaperController.prototype.resolveReleaseLocalPoint = function() {
        var viewport = this.resolveViewport();
        var local = this.lastPoint ? this.resolveLocalPoint(this.lastPoint) : { x: viewport.width * 0.5, y: viewport.height * 0.7 };
        return {
            x: this.direction === "next" ? -36 : viewport.width + 36,
            y: Math.max(14, Math.min(viewport.height - 14, local.y))
        };
    };

    TurnJsPaperController.prototype.buildTurnRoot = function(direction) {
        var pageSet = this.buildPages(direction);
        if (!pageSet) {
            return null;
        }

        var host = this.ensureOverlay();
        var turnRoot = this.turnRoot;
        var frames;
        var i;

        if (!turnRoot || !turnRoot.isConnected) {
            turnRoot = document.createElement("div");
            turnRoot.className = "paper-turn-book";
            for (i = 0; i < pageSet.pages.length; i++) {
                turnRoot.appendChild(pageSet.pages[i]);
            }
            host.innerHTML = "";
            host.appendChild(turnRoot);
            this.turnRoot = turnRoot;
        } else {
            frames = turnRoot.querySelectorAll(".paper-turn-frame");
            if (!frames || frames.length < 2) {
                host.innerHTML = "";
                turnRoot = document.createElement("div");
                turnRoot.className = "paper-turn-book";
                for (i = 0; i < pageSet.pages.length; i++) {
                    turnRoot.appendChild(pageSet.pages[i]);
                }
                host.appendChild(turnRoot);
                this.turnRoot = turnRoot;
            } else {
                frames[0].srcdoc = pageSet.pages[0].querySelector("iframe").srcdoc;
                frames[1].srcdoc = pageSet.pages[1].querySelector("iframe").srcdoc;
            }
        }

        return {
            root: turnRoot,
            expectedPage: pageSet.expectedPage,
            startPage: pageSet.startPage
        };
    };

    TurnJsPaperController.prototype.getActivePageElement = function() {
        if (!this.turnRoot || !this.startPage) {
            return null;
        }
        return this.turnRoot.querySelector(".turn-page.p" + this.startPage) ||
            this.turnRoot.querySelector(".paper-turn-page:nth-child(" + this.startPage + ")");
    };

    TurnJsPaperController.prototype.initTurn = function(direction) {
        var built = this.buildTurnRoot(direction);
        if (!built) {
            return false;
        }
        var viewport = this.resolveViewport();
        var $ = window.jQuery;
        var self = this;

        this.turnRoot = built.root;
        this.expectedPage = built.expectedPage;
        this.startPage = built.startPage;

        try {
            if (!this.turnEl) {
                this.turnEl = $(this.turnRoot);
                this.turnEl.turn({
                    width: viewport.width,
                    height: viewport.height,
                    autoCenter: false,
                    display: "single",
                    page: built.startPage,
                    pages: 2,
                    gradients: true,
                    acceleration: true,
                    duration: 880,
                    elevation: 68,
                    when: {
                        turned: function(event, page) {
                            if (!self.active || !self.committing) {
                                return;
                            }
                            if (page === self.expectedPage) {
                                self.finish(true);
                            }
                        }
                    }
                });
            } else {
                this.turnEl.turn("stop");
                this.turnEl.turn("size", viewport.width, viewport.height);
                this.turnEl.turn("page", built.startPage);
            }
        } catch (e) {
            log("Turn.js init error: " + e.message);
            return false;
        }

        return true;
    };

    TurnJsPaperController.prototype.begin = function(direction, startPoint) {
        if (!this.canBegin(direction) || !startPoint) {
            return false;
        }

        this.cleanup(true);
        this.clearTimers();

        this.direction = direction;
        this.startPoint = startPoint;
        this.lastPoint = startPoint;
        this.pendingAction = null;
        this.committing = false;
        this.touchId = Date.now();

        if (!this.initTurn(direction)) {
            this.cleanup(true);
            return false;
        }

        var pageElement = this.getActivePageElement();
        if (!pageElement) {
            this.cleanup(true);
            return false;
        }

        this.active = true;
        document.body.classList.add("paper-turn-live-active");

        var startLocal = this.resolveCornerLocalPoint(startPoint);
        var startGlobal = this.resolveGlobalPoint(startLocal);
        dispatchSyntheticTouch(pageElement, "touchstart", startGlobal, this.touchId);
        this.lastPoint = startGlobal;
        return true;
    };

    TurnJsPaperController.prototype.update = function(point) {
        if (!this.active || !point) {
            return false;
        }
        this.lastPoint = point;
        dispatchSyntheticTouch(document, "touchmove", point, this.touchId);
        return true;
    };

    TurnJsPaperController.prototype.cancel = function(immediate) {
        if (!this.active) {
            return;
        }
        this.pendingAction = null;
        this.committing = false;
        if (immediate) {
            this.cleanup(true);
            return;
        }

        var self = this;
        this.cleanupTimer = window.setTimeout(function() {
            self.cleanup(true);
        }, 40);
    };

    TurnJsPaperController.prototype.commit = function(action) {
        if (!this.active) {
            return false;
        }
        this.pendingAction = action || null;
        this.committing = true;

        var releasePoint = this.resolveGlobalPoint(this.resolveReleaseLocalPoint());
        dispatchSyntheticTouch(document, "touchmove", releasePoint, this.touchId);
        dispatchSyntheticTouch(document, "touchend", releasePoint, this.touchId);

        var self = this;
        this.releaseTimer = window.setTimeout(function() {
            self.finish(self.committing);
        }, 980);
        return true;
    };

    TurnJsPaperController.prototype.finish = function(accepted) {
        if (!this.active) {
            return;
        }

        var action = accepted ? this.pendingAction : null;
        this.cleanup(false);

        if (action) {
            try {
                action();
            } catch (e) {
                log("Turn.js action error: " + e.message);
            }
        }

        var self = this;
        this.cleanupTimer = window.setTimeout(function() {
            self.cleanup(true);
        }, 40);
    };

    TurnJsPaperController.prototype.cleanup = function(removeOverlay) {
        this.clearTimers();
        document.body.classList.remove("paper-turn-live-active");

        try {
            if (this.turnEl && this.turnEl.turn) {
                this.turnEl.turn("stop");
            }
        } catch (e) {
            log("Turn.js stop warning: " + e.message);
        }

        if (removeOverlay && this.overlay) {
            this.overlay.style.opacity = "";
        }

        this.direction = null;
        this.active = false;
        this.committing = false;
        this.pendingAction = null;
        this.expectedPage = null;
        this.startPage = null;
        this.startPoint = null;
        this.lastPoint = null;
    };

    TurnJsPaperController.prototype.isActive = function() {
        return !!this.active;
    };

    TurnJsPaperController.prototype.isActiveFor = function(direction) {
        return !!this.active && this.direction === direction;
    };

    var controller = new TurnJsPaperController();
    var originalBegin = window.beginPagedPreviewGesture;
    var originalUpdate = window.updatePagedPreviewGesture;
    var originalCancel = window.cancelPagedPreviewGesture;
    var originalCommit = window.commitPagedPreviewGesture;
    var originalIsReady = window.isPagedPreviewReady;

    window.beginPagedPreviewGesture = function(direction, startPoint) {
        if (controller.begin(direction, startPoint || null)) {
            return true;
        }
        return originalBegin ? originalBegin(direction, startPoint) : false;
    };

    window.updatePagedPreviewGesture = function(direction, dragDistancePx, point) {
        if (controller.isActiveFor(direction) && controller.update(point || null)) {
            return true;
        }
        return originalUpdate ? originalUpdate(direction, dragDistancePx, point) : false;
    };

    window.cancelPagedPreviewGesture = function(immediate) {
        if (controller.isActive()) {
            controller.cancel(!!immediate);
            if (immediate) {
                return;
            }
        }
        if (originalCancel) {
            originalCancel(!!immediate);
        }
    };

    window.commitPagedPreviewGesture = function(direction, action, options) {
        if (controller.isActiveFor(direction) && controller.commit(action)) {
            return;
        }
        if (originalCommit) {
            originalCommit(direction, action, options || {});
        }
    };

    window.isPagedPreviewReady = function(direction) {
        if (controller.canBegin(direction)) {
            return true;
        }
        return originalIsReady ? originalIsReady(direction) : false;
    };
})();
