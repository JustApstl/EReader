(function initRealPageFlipLayer() {
    function resolveActivePageTransitionStyle() {
        if (typeof normalizePageTransitionStyle === "function") {
            return normalizePageTransitionStyle(savedSettings && savedSettings.pageTransitionStyle);
        }
        return String((savedSettings && savedSettings.pageTransitionStyle) || "DEFAULT").toUpperCase();
    }

    function supportsRealPageFlip() {
        return !!(window.St && window.St.PageFlip);
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
            log("Real page flip snapshot warning: " + e.message);
            return "";
        }
    }

    function createSnapshotPage(html) {
        var page = document.createElement("div");
        page.className = "page-flip-snapshot";
        page.dataset.density = "soft";

        var frame = document.createElement("iframe");
        frame.className = "page-flip-snapshot-frame";
        frame.setAttribute("tabindex", "-1");
        frame.setAttribute("aria-hidden", "true");
        frame.srcdoc = html;

        page.appendChild(frame);
        return page;
    }

    function RealPageFlipController() {
        this.overlay = null;
        this.host = null;
        this.flip = null;
        this.direction = null;
        this.active = false;
        this.startPoint = null;
        this.lastPoint = null;
        this.expectedPageIndex = null;
        this.resultPageIndex = null;
        this.pendingAction = null;
        this.committing = false;
        this.cleanupTimer = 0;
    }

    RealPageFlipController.prototype.ensureOverlay = function() {
        if (this.overlay && this.overlay.isConnected && this.host && this.host.isConnected) {
            return this.host;
        }
        this.overlay = document.getElementById("page-flip-overlay");
        if (!this.overlay) {
            this.overlay = document.createElement("div");
            this.overlay.id = "page-flip-overlay";
            document.body.appendChild(this.overlay);
        }
        this.overlay.innerHTML = "";
        this.host = document.createElement("div");
        this.host.className = "page-flip-host";
        this.overlay.appendChild(this.host);
        return this.host;
    };

    RealPageFlipController.prototype.clearCleanupTimer = function() {
        if (this.cleanupTimer) {
            clearTimeout(this.cleanupTimer);
            this.cleanupTimer = 0;
        }
    };

    RealPageFlipController.prototype.toLocalPoint = function(point) {
        var rect = this.host ? this.host.getBoundingClientRect() : null;
        if (!rect) {
            return { x: 0, y: 0 };
        }
        return {
            x: point.x - rect.left,
            y: point.y - rect.top
        };
    };

    RealPageFlipController.prototype.canBegin = function(direction) {
        if (!supportsRealPageFlip() || !savedSettings || savedSettings.readingMode !== "PAGE" || !savedSettings.pageTurn3d) {
            return false;
        }
        if (resolveActivePageTransitionStyle() !== "PAPER") {
            return false;
        }
        if (!rendition) {
            return false;
        }
        var targetRendition = window.getPagedPreviewRendition ? window.getPagedPreviewRendition(direction) : null;
        return !!(snapshotHtmlFromRendition(rendition) && snapshotHtmlFromRendition(targetRendition));
    };

    RealPageFlipController.prototype.buildPages = function(direction) {
        var currentHtml = snapshotHtmlFromRendition(rendition);
        var targetRendition = window.getPagedPreviewRendition ? window.getPagedPreviewRendition(direction) : null;
        var targetHtml = snapshotHtmlFromRendition(targetRendition);
        if (!currentHtml || !targetHtml) {
            return null;
        }

        if (direction === "next") {
            return {
                expectedPageIndex: 1,
                startPage: 0,
                pages: [
                    createSnapshotPage(currentHtml),
                    createSnapshotPage(targetHtml)
                ]
            };
        }

        return {
            expectedPageIndex: 0,
            startPage: 1,
            pages: [
                createSnapshotPage(targetHtml),
                createSnapshotPage(currentHtml)
            ]
        };
    };

    RealPageFlipController.prototype.begin = function(direction, startPoint) {
        if (!this.canBegin(direction) || !startPoint) {
            return false;
        }

        var pageSet = this.buildPages(direction);
        if (!pageSet) {
            return false;
        }

        this.cleanup(true);

        var host = this.ensureOverlay();
        var viewport = typeof getReaderViewportSize === "function"
            ? getReaderViewportSize()
            : {
                width: Math.max(1, window.innerWidth || document.documentElement.clientWidth || 1),
                height: Math.max(1, window.innerHeight || document.documentElement.clientHeight || 1)
            };

        this.direction = direction;
        this.startPoint = startPoint;
        this.lastPoint = startPoint;
        this.expectedPageIndex = pageSet.expectedPageIndex;
        this.resultPageIndex = pageSet.startPage;
        this.pendingAction = null;
        this.committing = false;
        this.clearCleanupTimer();

        try {
            this.flip = new window.St.PageFlip(host, {
                width: viewport.width,
                height: viewport.height,
                size: "fixed",
                minWidth: viewport.width,
                maxWidth: viewport.width,
                minHeight: viewport.height,
                maxHeight: viewport.height,
                drawShadow: true,
                flippingTime: 560,
                usePortrait: true,
                startZIndex: 12,
                autoSize: false,
                maxShadowOpacity: 0.55,
                showCover: false,
                mobileScrollSupport: false,
                swipeDistance: 8,
                clickEventForward: false,
                useMouseEvents: false,
                showPageCorners: false,
                disableFlipByClick: true
            });
            this.flip.loadFromHTML(pageSet.pages);
        } catch (e) {
            log("Real page flip init error: " + e.message);
            this.cleanup(true);
            return false;
        }

        var self = this;
        this.flip.on("flip", function(event) {
            self.resultPageIndex = event.data;
        });
        this.flip.on("changeState", function(event) {
            if (!self.active) {
                return;
            }
            if (event.data === "read") {
                self.finish();
            }
        });

        this.active = true;
        document.body.classList.add("page-flip-live-active");

        try {
            this.flip.startUserTouch(this.toLocalPoint(startPoint));
        } catch (e2) {
            log("Real page flip touch start error: " + e2.message);
            this.cleanup(true);
            return false;
        }

        return true;
    };

    RealPageFlipController.prototype.update = function(point) {
        if (!this.active || !this.flip || !point) {
            return false;
        }
        this.lastPoint = point;
        try {
            this.flip.userMove(this.toLocalPoint(point), true);
            return true;
        } catch (e) {
            log("Real page flip move error: " + e.message);
            this.cleanup(true);
            return false;
        }
    };

    RealPageFlipController.prototype.commit = function(action) {
        if (!this.active || !this.flip) {
            return false;
        }
        this.pendingAction = action || null;
        this.committing = true;
        this.clearCleanupTimer();

        try {
            this.flip.userStop(this.toLocalPoint(this.lastPoint || this.startPoint), false);
        } catch (e) {
            log("Real page flip stop error: " + e.message);
            this.finish();
            return false;
        }

        var self = this;
        this.cleanupTimer = window.setTimeout(function() {
            self.finish();
        }, 520);
        return true;
    };

    RealPageFlipController.prototype.cancel = function(immediate) {
        if (!this.active) {
            return;
        }
        this.pendingAction = null;
        this.committing = false;
        this.clearCleanupTimer();

        if (immediate) {
            this.cleanup(true);
            return;
        }

        try {
            if (this.flip && this.flip.getFlipController) {
                this.flip.getFlipController().stopMove();
            }
        } catch (e) {
            log("Real page flip cancel warning: " + e.message);
            this.cleanup(true);
            return;
        }

        var self = this;
        this.cleanupTimer = window.setTimeout(function() {
            self.cleanup(true);
        }, 360);
    };

    RealPageFlipController.prototype.finish = function() {
        if (!this.active) {
            return;
        }

        var accepted = this.committing && this.resultPageIndex === this.expectedPageIndex;
        var action = accepted ? this.pendingAction : null;

        this.cleanup(true);

        if (action) {
            try {
                action();
            } catch (e) {
                log("Real page flip action error: " + e.message);
            }
        }
    };

    RealPageFlipController.prototype.cleanup = function(removeOverlay) {
        this.clearCleanupTimer();
        document.body.classList.remove("page-flip-live-active");

        try {
            if (this.flip) {
                this.flip.destroy();
            }
        } catch (e) {
            log("Real page flip destroy warning: " + e.message);
        }

        if (removeOverlay && this.overlay) {
            this.overlay.innerHTML = "";
        }

        this.flip = null;
        this.direction = null;
        this.active = false;
        this.startPoint = null;
        this.lastPoint = null;
        this.expectedPageIndex = null;
        this.resultPageIndex = null;
        this.pendingAction = null;
        this.committing = false;
    };

    RealPageFlipController.prototype.isActive = function() {
        return !!this.active;
    };

    RealPageFlipController.prototype.isActiveFor = function(direction) {
        return !!this.active && this.direction === direction;
    };

    var controller = new RealPageFlipController();
    var originalBegin = window.beginPagedPreviewGesture;
    var originalUpdate = window.updatePagedPreviewGesture;
    var originalCancel = window.cancelPagedPreviewGesture;
    var originalCommit = window.commitPagedPreviewGesture;
    var originalIsReady = window.isPagedPreviewReady;

    window.beginPagedPreviewGesture = function(direction, startPoint) {
        if (controller.begin(direction, startPoint || null)) {
            return true;
        }
        return originalBegin ? originalBegin(direction) : false;
    };

    window.updatePagedPreviewGesture = function(direction, dragDistancePx, point) {
        if (controller.isActiveFor(direction) && controller.update(point || null)) {
            return true;
        }
        return originalUpdate ? originalUpdate(direction, dragDistancePx) : false;
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
