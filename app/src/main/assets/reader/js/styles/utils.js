function isInsideAnchor(element) {
    try {
        return !!(element && element.closest && element.closest("a"));
    } catch (e) {
        return false;
    }
}

function unwrapReaderBionicWrappers(root) {
    if (!root || !root.querySelectorAll || !root.ownerDocument) {
        return;
    }

    var doc = root.ownerDocument;
    var wrappedNodes = root.querySelectorAll(".reader-bionic-wrapper[data-bionic-source]");
    for (var i = 0; i < wrappedNodes.length; i++) {
        var wrapper = wrappedNodes[i];
        var source = wrapper.getAttribute("data-bionic-source") || wrapper.textContent || "";
        if (!wrapper.parentNode) {
            continue;
        }
        wrapper.parentNode.replaceChild(doc.createTextNode(source), wrapper);
    }

    var legacyCandidates = root.querySelectorAll("span");
    for (var j = legacyCandidates.length - 1; j >= 0; j--) {
        var candidate = legacyCandidates[j];
        if (!candidate || !candidate.parentNode || candidate.classList.contains("bionic-bold")) {
            continue;
        }
        if (isInsideAnchor(candidate)) {
            continue;
        }
        if (candidate.childElementCount !== 1) {
            continue;
        }
        var firstChild = candidate.firstElementChild;
        if (!firstChild || !firstChild.classList || !firstChild.classList.contains("bionic-bold")) {
            continue;
        }
        candidate.parentNode.replaceChild(doc.createTextNode(candidate.textContent || ""), candidate);
    }

    var anchorBionic = root.querySelectorAll("a .bionic-bold");
    for (var k = 0; k < anchorBionic.length; k++) {
        var span = anchorBionic[k];
        if (!span.parentNode) {
            continue;
        }
        span.parentNode.replaceChild(doc.createTextNode(span.textContent || ""), span);
    }
}

function clearBionicMarkup(root) {
    unwrapReaderBionicWrappers(root);
    if (root && root.dataset) {
        root.dataset.bionicApplied = "false";
    }
}

function applyBionic(root, weight, colorHex, emphasisRatio) {
    if (!root || !root.ownerDocument) {
        return;
    }

    var doc = root.ownerDocument;
    var normalizedRatio = parseFloat(emphasisRatio);
    if (!isFinite(normalizedRatio)) {
        normalizedRatio = 0.45;
    }
    normalizedRatio = Math.max(0.15, Math.min(0.8, normalizedRatio));
    doc.documentElement.style.setProperty("--bionic-weight", String(weight || 700));
    doc.documentElement.style.setProperty("--bionic-color", colorHex || "inherit");
    doc.documentElement.style.setProperty("--bionic-emphasis-ratio", String(normalizedRatio));

    clearBionicMarkup(root);
    root.dataset.bionicApplied = "true";
    root.dataset.bionicEmphasisRatio = String(normalizedRatio);

    var walker = doc.createTreeWalker(root, NodeFilter.SHOW_TEXT, null, false);
    var node;
    var nodes = [];
    var skippedTags = {
        SCRIPT: true,
        STYLE: true,
        CODE: true,
        PRE: true,
        IMG: true
    };

    while ((node = walker.nextNode())) {
        var parent = node.parentElement;
        if (!parent || skippedTags[parent.tagName] || isInsideAnchor(parent)) {
            continue;
        }
        nodes.push(node);
    }

    nodes.forEach(function(textNode) {
        var text = textNode.nodeValue;
        if (!text || text.trim().length < 2) {
            return;
        }

        var fragment = doc.createDocumentFragment();
        var lastIndex = 0;
        var hasReplacement = false;

        text.replace(/[A-Za-z0-9]+/g, function(word, offset) {
            if (offset > lastIndex) {
                fragment.appendChild(doc.createTextNode(text.slice(lastIndex, offset)));
            }
            if (word.length <= 1) {
                fragment.appendChild(doc.createTextNode(word));
            } else {
                var boldCount = word.length <= 3
                    ? 1
                    : Math.max(1, Math.min(word.length - 1, Math.round(word.length * normalizedRatio)));
                var boldSpan = doc.createElement("span");
                boldSpan.className = "bionic-bold";
                boldSpan.textContent = word.slice(0, boldCount);
                fragment.appendChild(boldSpan);
                fragment.appendChild(doc.createTextNode(word.slice(boldCount)));
                hasReplacement = true;
            }
            lastIndex = offset + word.length;
            return word;
        });

        if (lastIndex < text.length) {
            fragment.appendChild(doc.createTextNode(text.slice(lastIndex)));
        }

        if (!hasReplacement) {
            return;
        }

        var wrapper = doc.createElement("span");
        wrapper.className = "reader-bionic-wrapper";
        wrapper.setAttribute("data-bionic-source", text);
        wrapper.appendChild(fragment);
        textNode.parentNode.replaceChild(wrapper, textNode);
    });
}

function updateBionicSpanStyles(doc, weight, colorHex) {
    if (!doc || !doc.querySelectorAll) {
        return;
    }
    var spans = doc.querySelectorAll(".bionic-bold");
    if (!spans || spans.length === 0) {
        return;
    }
    for (var i = 0; i < spans.length; i++) {
        var span = spans[i];
        if (isInsideAnchor(span)) {
            span.style.setProperty("font-weight", "inherit", "important");
            span.style.setProperty("color", "inherit", "important");
            span.style.setProperty("-webkit-text-fill-color", "inherit", "important");
            continue;
        }
        span.style.setProperty("font-weight", String(weight || 700), "important");
        span.style.setProperty("color", colorHex || "inherit", "important");
        span.style.setProperty("-webkit-text-fill-color", colorHex || "inherit", "important");
    }
}

function clearBionicSpanStyles(doc) {
    if (!doc || !doc.querySelectorAll) {
        return;
    }
    var spans = doc.querySelectorAll(".bionic-bold");
    if (!spans || spans.length === 0) {
        return;
    }
    for (var i = 0; i < spans.length; i++) {
        var span = spans[i];
        span.style.removeProperty("font-weight");
        span.style.removeProperty("color");
        span.style.removeProperty("-webkit-text-fill-color");
    }
}

function toShadowColor(colorHex) {
    if (!colorHex || typeof colorHex !== "string") {
        return "rgba(0,0,0,0.58)";
    }
    var normalized = colorHex.trim();
    if (!normalized.startsWith("#")) {
        return normalized;
    }
    var hex = normalized.slice(1);
    if (hex.length === 3) {
        hex = hex[0] + hex[0] + hex[1] + hex[1] + hex[2] + hex[2];
    }
    if (hex.length !== 6) {
        return "rgba(0,0,0,0.58)";
    }
    var r = parseInt(hex.slice(0, 2), 16);
    var g = parseInt(hex.slice(2, 4), 16);
    var b = parseInt(hex.slice(4, 6), 16);
    if (!isFinite(r) || !isFinite(g) || !isFinite(b)) {
        return "rgba(0,0,0,0.58)";
    }
    return "rgba(" + r + "," + g + "," + b + ",0.56)";
}

function parseHexColor(colorHex) {
    if (!colorHex || typeof colorHex !== "string") {
        return null;
    }
    var normalized = colorHex.trim();
    if (normalized[0] !== "#") {
        return null;
    }
    var hex = normalized.slice(1);
    if (hex.length === 3) {
        hex = hex[0] + hex[0] + hex[1] + hex[1] + hex[2] + hex[2];
    }
    if (hex.length !== 6) {
        return null;
    }
    var r = parseInt(hex.slice(0, 2), 16);
    var g = parseInt(hex.slice(2, 4), 16);
    var b = parseInt(hex.slice(4, 6), 16);
    if (!isFinite(r) || !isFinite(g) || !isFinite(b)) {
        return null;
    }
    return { r: r, g: g, b: b };
}

function colorLuminance(colorHex) {
    var rgb = parseHexColor(colorHex);
    if (!rgb) {
        return null;
    }
    var r = rgb.r / 255;
    var g = rgb.g / 255;
    var b = rgb.b / 255;
    return (0.2126 * r) + (0.7152 * g) + (0.0722 * b);
}

function resolveAutoImageFilterMode(settings) {
    if (!settings) {
        return "NONE";
    }
    var luminance = colorLuminance(settings.backgroundColor || "");
    if (luminance !== null && luminance < 0.45) {
        return "DARKEN";
    }
    return "NONE";
}

function resolveHrColor(settings, textColor) {
    if (textColor && textColor !== "inherit") {
        return textColor;
    }
    var bg = settings && settings.backgroundColor ? settings.backgroundColor : "";
    var lum = colorLuminance(bg);
    if (lum !== null) {
        return lum < 0.5 ? "rgba(255,255,255,0.45)" : "rgba(0,0,0,0.35)";
    }
    return "currentColor";
}

function loadCustomFontDataUrl(uri) {
    if (!uri) {
        customFontCacheUri = "";
        customFontCacheDataUrl = "";
        return "";
    }
    if (customFontCacheUri === uri && customFontCacheDataUrl) {
        return customFontCacheDataUrl;
    }

    var bridge = getBridge();
    if (!bridge || !bridge.getFontDataUrl) {
        return "";
    }

    try {
        customFontCacheDataUrl = bridge.getFontDataUrl(uri) || "";
        customFontCacheUri = uri;
        return customFontCacheDataUrl;
    } catch (e) {
        log("Custom font load error: " + e.message);
        return "";
    }
}

function resolveFontFamily(contents, settings) {
    var baseFamily = settings && settings.fontFamily ? settings.fontFamily : "serif";
    return resolveConfiguredFontFamily(contents, settings, baseFamily, "serif", true);
}

function resolveElementFontFamily(contents, settings, requestedFamily, fallbackFamily) {
    return resolveConfiguredFontFamily(contents, settings, requestedFamily, fallbackFamily || "serif", false);
}

function resolveConfiguredFontFamily(contents, settings, requestedFamily, fallbackFamily, markBodyClass) {
    var baseFamily = requestedFamily || fallbackFamily || "serif";
    if (baseFamily === "default" || baseFamily === "inherit") {
        baseFamily = fallbackFamily || "serif";
    }
    if (baseFamily !== "custom") {
        if (markBodyClass && contents && contents.document && contents.document.body) {
            contents.document.body.classList.remove("reader-custom-font-enabled");
        }
        return baseFamily;
    }

    var customFontUri = settings && settings.customFontUri ? settings.customFontUri : "";
    var dataUrl = loadCustomFontDataUrl(customFontUri);
    if (!dataUrl || !contents || !contents.document) {
        if (markBodyClass && contents && contents.document && contents.document.body) {
            contents.document.body.classList.remove("reader-custom-font-enabled");
        }
        return fallbackFamily || "serif";
    }

    var doc = contents.document;
    var styleId = "reader-custom-font-face";
    var styleTag = doc.getElementById(styleId);
    if (!styleTag) {
        styleTag = doc.createElement("style");
        styleTag.id = styleId;
        styleTag.type = "text/css";
        (doc.head || doc.documentElement).appendChild(styleTag);
    }
    if (styleTag.getAttribute("data-font-uri") !== customFontUri) {
        styleTag.setAttribute("data-font-uri", customFontUri);
        styleTag.textContent = "@font-face { font-family: 'ReaderCustomFont'; src: url('" + dataUrl + "'); font-display: swap; }";
    }
    if (markBodyClass && doc.body) {
        doc.body.classList.add("reader-custom-font-enabled");
    }
    return "'ReaderCustomFont', serif";
}
