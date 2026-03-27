function applyImageFilterToContents(contents, filterStyle) {
    if (!contents || !contents.document || !contents.document.body) {
        return;
    }
    var doc = contents.document;
    contents.__imageFilterStyle = filterStyle || "none";
    updateImageFilterOverride(contents, contents.__imageFilterStyle);
    contents.__applyImageFilter = function() {
        var style = contents.__imageFilterStyle || "none";
        var clearFilter = style === "none";
        var nodes = doc.querySelectorAll("img, svg, image");
        for (var i = 0; i < nodes.length; i++) {
            var node = nodes[i];
            if (!node || !node.style) {
                continue;
            }
            if (node.classList && node.classList.contains("epub-cover-image")) {
                if (clearFilter) {
                    node.style.removeProperty("filter");
                } else {
                    node.style.setProperty("filter", "none", "important");
                }
                continue;
            }
            if (clearFilter) {
                node.style.removeProperty("filter");
            } else {
                node.style.setProperty("filter", style, "important");
            }
            if (!clearFilter && node.tagName && node.tagName.toLowerCase() === "img") {
                if (!node.__readerFilterListener) {
                    node.__readerFilterListener = true;
                    node.addEventListener("load", function() {
                        try {
                            if (contents.__applyImageFilter) {
                                contents.__applyImageFilter();
                            }
                        } catch (e) {}
                    }, { once: true });
                }
            }
        }
    };
    contents.__applyImageFilter();

    if (!contents.__imageFilterObserverInstalled) {
        contents.__imageFilterObserverInstalled = true;
        try {
            var observer = new MutationObserver(function() {
                if (contents.__applyImageFilter) {
                    contents.__applyImageFilter();
                }
            });
            observer.observe(doc.body, { childList: true, subtree: true, attributes: true, attributeFilter: ["src", "href", "xlink:href"] });
        } catch (e) {
            log("Image filter observer warning: " + e.message);
        }
    }
}

function updateImageFilterOverride(contents, filterStyle) {
    if (!contents || !contents.document) {
        return;
    }
    var doc = contents.document;
    var styleId = "reader-image-filter-override";
    var styleTag = doc.getElementById(styleId);
    var shouldOverride = (filterStyle || "none") === "none";
    if (!shouldOverride) {
        if (styleTag && styleTag.parentNode) {
            styleTag.parentNode.removeChild(styleTag);
        }
        return;
    }
    if (!styleTag) {
        styleTag = doc.createElement("style");
        styleTag.id = styleId;
        styleTag.type = "text/css";
        (doc.head || doc.documentElement).appendChild(styleTag);
    }
    styleTag.textContent = "img, svg, image { filter: none !important; }";
}

function normalizeCoverRef(href) {
    if (!href || typeof href !== "string") {
        return "";
    }
    var clean = href.split("#")[0].split("?")[0];
    if (clean.startsWith("./")) {
        clean = clean.slice(2);
    }
    return clean;
}

function coverTokenMatch(value) {
    if (!value || typeof value !== "string") {
        return false;
    }
    return value.toLowerCase().indexOf("cover") !== -1;
}

function findCoverImageNode(doc, onlyCoverHints) {
    if (!doc) {
        return null;
    }
    var nodes = doc.querySelectorAll("img, svg, image");
    if (!nodes || nodes.length === 0) {
        return null;
    }
    for (var i = 0; i < nodes.length; i++) {
        var node = nodes[i];
        if (!node) continue;
        if (!onlyCoverHints) {
            return node;
        }
        var id = node.getAttribute ? node.getAttribute("id") : "";
        var cls = node.getAttribute ? node.getAttribute("class") : "";
        var alt = node.getAttribute ? node.getAttribute("alt") : "";
        var title = node.getAttribute ? node.getAttribute("title") : "";
        var src = node.getAttribute ? (node.getAttribute("src") || node.getAttribute("href")) : "";
        if (coverTokenMatch(id) || coverTokenMatch(cls) || coverTokenMatch(alt) || coverTokenMatch(title) || coverTokenMatch(src)) {
            return node;
        }
    }
    return null;
}

function excludeCoverImageOnce(contents) {
    if (coverImageExcluded) {
        return;
    }
    if (!contents || !contents.document) {
        return;
    }
    var doc = contents.document;
    var coverTarget = null;
    try {
        if (typeof resolveCoverTarget === "function") {
            coverTarget = resolveCoverTarget();
        }
    } catch (e) {
        coverTarget = null;
    }

    var coverNode = null;
    var coverTargetClean = normalizeCoverRef(coverTarget);
    var isCoverImageTarget = coverTargetClean && (/\.(png|jpe?g|gif|webp|svg)$/i.test(coverTargetClean));
    var sectionHref = "";
    try {
        sectionHref = contents.section && contents.section.href ? contents.section.href : "";
    } catch (e) {
        sectionHref = "";
    }
    var sectionHrefClean = normalizeCoverRef(sectionHref);
    var isCoverSection = false;

    if (coverTargetClean && !coverTargetClean.startsWith("epubcfi(")) {
        if (isCoverImageTarget) {
            var nodes = doc.querySelectorAll("img, svg, image");
            for (var n = 0; n < nodes.length; n++) {
                var node = nodes[n];
                if (!node || !node.getAttribute) continue;
                var src = normalizeCoverRef(node.getAttribute("src") || node.getAttribute("href") || "");
                if (src && (src.indexOf(coverTargetClean) !== -1 || coverTargetClean.indexOf(src) !== -1)) {
                    coverNode = node;
                    break;
                }
            }
        } else if (sectionHrefClean && (sectionHrefClean.indexOf(coverTargetClean) !== -1 || coverTargetClean.indexOf(sectionHrefClean) !== -1)) {
            isCoverSection = true;
        }
    }

    if (!coverNode) {
        if (isCoverSection) {
            coverNode = findCoverImageNode(doc, false);
        } else {
            coverNode = findCoverImageNode(doc, true);
        }
    }

    if (!coverNode) {
        var sectionIndex = null;
        try {
            if (contents.section && typeof contents.section.index === "number") {
                sectionIndex = contents.section.index;
            }
        } catch (e) {
            sectionIndex = null;
        }
        if (sectionIndex !== null && sectionIndex <= 1) {
            coverNode = findCoverImageNode(doc, false);
        }
    }

    if (!coverNode) {
        return;
    }

    try {
        coverNode.classList.add("epub-cover-image");
        if (coverNode.style && coverNode.style.setProperty) {
            coverNode.style.setProperty("filter", "none", "important");
        }
    } catch (e) {
        log("Cover image exclusion warning: " + e.message);
    }
    coverImageExcluded = true;
}
