function resolveElementTypography(contents, settings, elementStyle, fallbackColor, fallbackFontFamily) {
    var style = elementStyle || {};
    var resolvedColor = (style.color && String(style.color).length > 0) ? style.color : fallbackColor;
    var resolvedFontFamily = resolveElementFontFamily(
        contents,
        settings,
        style.fontFamily || "",
        fallbackFontFamily
    );
    return {
        color: resolvedColor,
        fontFamily: resolvedFontFamily
    };
}

function applyStylesToContents(contents) {
    var s = savedSettings;
    if (!s || !contents) {
        return;
    }

    var usePublisherStyle = !!s.usePublisherStyle;
    var textColor = s.textColor || "inherit";
    var shouldApplyColor = textColor !== "inherit";
    var marginValue = (s.margin !== undefined && s.margin !== null) ? s.margin : 20;
    var shadowStyle = s.textShadow ? ("0 1.4px 3.2px " + toShadowColor(s.textShadowColor)) : "none";
    var linkDecoration = s.underlineLinks ? "underline" : "none";
    var chapterLinkDecoration = "underline";
    var bionicWeight = (s.focusTextBoldness || 700);
    var bionicEmphasis = (s.focusTextEmphasis !== undefined && s.focusTextEmphasis !== null) ? s.focusTextEmphasis : 0.45;
    var bionicColor = (s.focusTextColor && String(s.focusTextColor).length > 0) ? s.focusTextColor : "inherit";
    var resolvedFontFamily = resolveFontFamily(contents, s);
    var hrColor = resolveHrColor(s, textColor);
    var isPageMode = s.readingMode === "PAGE";
    var pageTextSelector = "p, h1, h2, h3, h4, h5, h6, li, article, section, blockquote, span, strong, em, b, i, small, sup, sub, cite, mark";
    var textAlignValue = s.textAlignment || "";
    var externalLinkSelector = "a[href^='http://'], a[href^='https://'], a[href^='www.'], a[href^='mailto:']";
    var internalLinkSelector = "nav a, a[href^='#'], a[href^='epubcfi('], a:not([href^='http']):not([href^='https']):not([href^='www.']):not([href^='mailto:'])";
    var elementStyles = s.elementStyles || {};

    var effectiveImageFilter = s.imageFilter === "AUTO" ? resolveAutoImageFilterMode(s) : s.imageFilter;
    var imageFilterStyle = "none";
    if (effectiveImageFilter === "INVERT") {
        imageFilterStyle = "invert(100%)";
    } else if (effectiveImageFilter === "DARKEN") {
        imageFilterStyle = "brightness(50%)";
    } else if (effectiveImageFilter === "BW") {
        imageFilterStyle = "grayscale(100%)";
    }

    var paragraphStyle = resolveElementTypography(
        contents,
        s,
        elementStyles.paragraph,
        shouldApplyColor ? textColor : "inherit",
        resolvedFontFamily
    );
    var heading1Style = resolveElementTypography(contents, s, elementStyles.heading1, shouldApplyColor ? textColor : "inherit", resolvedFontFamily);
    var heading2Style = resolveElementTypography(contents, s, elementStyles.heading2, shouldApplyColor ? textColor : "inherit", resolvedFontFamily);
    var heading3Style = resolveElementTypography(contents, s, elementStyles.heading3, shouldApplyColor ? textColor : "inherit", resolvedFontFamily);
    var heading4Style = resolveElementTypography(contents, s, elementStyles.heading4, shouldApplyColor ? textColor : "inherit", resolvedFontFamily);
    var heading5Style = resolveElementTypography(contents, s, elementStyles.heading5, shouldApplyColor ? textColor : "inherit", resolvedFontFamily);
    var heading6Style = resolveElementTypography(contents, s, elementStyles.heading6, shouldApplyColor ? textColor : "inherit", resolvedFontFamily);
    var externalLinkStyle = resolveElementTypography(contents, s, elementStyles.externalLink, "#2F6BFF", resolvedFontFamily);
    var internalLinkStyle = resolveElementTypography(contents, s, elementStyles.internalLink, shouldApplyColor ? textColor : "inherit", resolvedFontFamily);

    var rules = {
        "body": {
            "background-color": "transparent !important",
            "font-kerning": "normal !important",
            "text-rendering": "optimizeLegibility !important",
            "-webkit-font-smoothing": "antialiased !important"
        },
        "a": {
            "text-decoration": "none !important"
        },
        [externalLinkSelector]: {
            "color": externalLinkStyle.color + " !important",
            "-webkit-text-fill-color": externalLinkStyle.color + " !important",
            "font-family": externalLinkStyle.fontFamily + " !important",
            "text-decoration": linkDecoration + " !important"
        },
        [externalLinkSelector + ", " + externalLinkSelector + " *"]: {
            "color": externalLinkStyle.color + " !important",
            "-webkit-text-fill-color": externalLinkStyle.color + " !important"
        },
        [internalLinkSelector]: {
            "color": internalLinkStyle.color + " !important",
            "-webkit-text-fill-color": internalLinkStyle.color + " !important",
            "font-family": internalLinkStyle.fontFamily + " !important",
            "text-decoration": chapterLinkDecoration + " !important"
        },
        "a .bionic-bold, body.bionic-enabled a .bionic-bold, html.bionic-enabled a .bionic-bold": {
            "font-weight": "inherit !important",
            "color": "inherit !important",
            "-webkit-text-fill-color": "inherit !important"
        },
        ".bionic-enabled .bionic-bold": {
            "font-weight": String(bionicWeight) + " !important",
            "color": bionicColor + " !important",
            "-webkit-text-fill-color": bionicColor + " !important"
        },
        "body.bionic-enabled .bionic-bold": {
            "font-weight": String(bionicWeight) + " !important",
            "color": bionicColor + " !important",
            "-webkit-text-fill-color": bionicColor + " !important"
        },
        "html.bionic-enabled .bionic-bold": {
            "font-weight": String(bionicWeight) + " !important",
            "color": bionicColor + " !important",
            "-webkit-text-fill-color": bionicColor + " !important"
        },
        ".tts-highlight": {
            "background-color": "rgba(255, 235, 59, 0.32) !important",
            "border-radius": "3px !important",
            "box-decoration-break": "clone !important"
        },
        ".tts-word-active": {
            "background-color": "rgba(255, 235, 59, 0.65) !important",
            "border-radius": "3px !important",
            "box-decoration-break": "clone !important"
        },
        ".tts-sentence-active": {
            "background": "linear-gradient(180deg, rgba(255, 235, 59, 0.10), rgba(255, 235, 59, 0.20)) !important",
            "border-radius": "8px !important",
            "box-decoration-break": "clone !important",
            "padding": "0.05em 0.08em !important"
        },
        "hr, [role='separator'], .separator, .divider": {
            "border": "none !important",
            "border-top": "1px solid " + hrColor + " !important",
            "border-color": hrColor + " !important",
            "background-color": hrColor + " !important",
            "color": hrColor + " !important",
            "margin": "1.2em 0 !important",
            "opacity": "0.35 !important"
        }
    };
    if (imageFilterStyle !== "none") {
        rules["img, svg, image"] = {
            "filter": imageFilterStyle + " !important"
        };
    }

    if (textAlignValue && textAlignValue.length > 0 && textAlignValue !== "undefined") {
        rules["body.reader-align-custom"] = {
            "text-align": textAlignValue + " !important"
        };
        rules["body.reader-align-custom " + pageTextSelector] = {
            "text-align": textAlignValue + " !important"
        };
    }

    if (!usePublisherStyle) {
        // Apply user preferences when not using publisher style
        if (shouldApplyColor) {
            rules.body["color"] = textColor + " !important";
            rules.body["-webkit-text-fill-color"] = textColor + " !important";
        }
        rules.body["text-shadow"] = shadowStyle + " !important";
        rules.body["font-family"] = resolvedFontFamily + " !important";
        rules.body["font-size"] = (s.fontSize || 15) + "px !important";
        rules.body["line-height"] = (s.lineHeight || 1.55) + " !important";
        if (!isPageMode) {
            rules.body["padding-left"] = marginValue + "px !important";
            rules.body["padding-right"] = marginValue + "px !important";
            rules.body["margin"] = "0 !important";
            rules.body["box-sizing"] = "border-box !important";
        }
        rules.body["letter-spacing"] = "0.01em !important";
        rules.body["word-spacing"] = "0.02em !important";
        rules.body["hyphens"] = "auto !important";
        rules.body["word-break"] = "normal !important";
        rules.body["overflow-wrap"] = "break-word !important";
        rules["html"] = {
            "font-size": (s.fontSize || 15) + "px !important",
            "line-height": (s.lineHeight || 1.55) + " !important"
        };
        rules["span, strong, em, b, i, small, sup, sub, cite, mark, .bionic-bold"] = {
            "display": "inline !important",
            "white-space": "normal !important",
            "word-break": "normal !important",
            "overflow-wrap": "break-word !important"
        };

        if (shouldApplyColor) {
            rules[isPageMode ? pageTextSelector : "p, h1, h2, h3, h4, h5, h6, li, article, section"] = {
                "color": textColor + " !important",
                "-webkit-text-fill-color": textColor + " !important",
                "text-shadow": shadowStyle + " !important"
            };
        }
        if (!isPageMode) {
            var bodyWildcard = {
                "text-shadow": shadowStyle + " !important",
                "font-family": resolvedFontFamily + " !important"
            };
            if (shouldApplyColor) {
                bodyWildcard["color"] = textColor + " !important";
            }
            rules["body *"] = bodyWildcard;
        }
        rules["p"] = {
            "margin": "0 0 0.9em 0 !important",
            "font-family": paragraphStyle.fontFamily + " !important",
            "color": paragraphStyle.color + " !important",
            "-webkit-text-fill-color": paragraphStyle.color + " !important"
        };
        rules["h1"] = {
            "font-family": heading1Style.fontFamily + " !important",
            "color": heading1Style.color + " !important",
            "-webkit-text-fill-color": heading1Style.color + " !important"
        };
        rules["h2"] = {
            "font-family": heading2Style.fontFamily + " !important",
            "color": heading2Style.color + " !important",
            "-webkit-text-fill-color": heading2Style.color + " !important"
        };
        rules["h3"] = {
            "font-family": heading3Style.fontFamily + " !important",
            "color": heading3Style.color + " !important",
            "-webkit-text-fill-color": heading3Style.color + " !important"
        };
        rules["h4"] = {
            "font-family": heading4Style.fontFamily + " !important",
            "color": heading4Style.color + " !important",
            "-webkit-text-fill-color": heading4Style.color + " !important"
        };
        rules["h5"] = {
            "font-family": heading5Style.fontFamily + " !important",
            "color": heading5Style.color + " !important",
            "-webkit-text-fill-color": heading5Style.color + " !important"
        };
        rules["h6"] = {
            "font-family": heading6Style.fontFamily + " !important",
            "color": heading6Style.color + " !important",
            "-webkit-text-fill-color": heading6Style.color + " !important"
        };
        if (shouldApplyColor) {
            rules["a"]["color"] = textColor + " !important";
            rules["a"]["-webkit-text-fill-color"] = textColor + " !important";
        }
    }

    try {
        if (contents.addStylesheetRules) {
            contents.addStylesheetRules(rules);
        }
    } catch (e) {
        log("Stylesheet rules warning: " + e.message);
    }

    try {
        var cssText = "";
        for (var selector in rules) {
            if (!rules.hasOwnProperty(selector)) continue;
            cssText += selector + " {";
            var props = rules[selector];
            for (var prop in props) {
                if (!props.hasOwnProperty(prop)) continue;
                cssText += prop + ":" + props[prop] + ";";
            }
            cssText += "}\n";
        }
        var doc = contents.document;
        if (doc) {
            var styleId = "reader-custom-style";
            var styleTag = doc.getElementById(styleId);
            if (!styleTag) {
                styleTag = doc.createElement("style");
                styleTag.id = styleId;
                styleTag.type = "text/css";
                (doc.head || doc.documentElement).appendChild(styleTag);
            }
            if (styleTag.textContent !== cssText) {
                styleTag.textContent = cssText;
            }
        }
    } catch (e) {
        log("Style tag injection warning: " + e.message);
    }

    // Inline overrides to beat book CSS (including inline styles).
    try {
        if (!usePublisherStyle && contents.document && contents.document.body) {
            var doc2 = contents.document;
            var body = doc2.body;
            var sizePx = (s.fontSize || 15) + "px";
            var lineHeight = String(s.lineHeight || 1.55);
            var marginPx = "0 " + marginValue + "px";
            body.classList.remove("reader-align-custom");
            body.style.setProperty("font-family", resolvedFontFamily, "important");
            body.style.setProperty("font-size", sizePx, "important");
            body.style.setProperty("line-height", lineHeight, "important");
            body.style.setProperty("word-break", "normal", "important");
            body.style.setProperty("overflow-wrap", "break-word", "important");
            if (!isPageMode) {
                body.style.setProperty("padding", marginPx, "important");
                body.style.setProperty("padding-left", marginValue + "px", "important");
                body.style.setProperty("padding-right", marginValue + "px", "important");
                body.style.setProperty("margin", "0", "important");
                body.style.setProperty("box-sizing", "border-box", "important");
            }
            body.style.setProperty("text-shadow", shadowStyle, "important");
            if (shouldApplyColor) {
                body.style.setProperty("color", textColor, "important");
                body.style.setProperty("-webkit-text-fill-color", textColor, "important");
            }

            doc2.documentElement.style.setProperty("font-size", sizePx, "important");
            doc2.documentElement.style.setProperty("line-height", lineHeight, "important");

            body.style.removeProperty("text-align");
            if (textAlignValue && textAlignValue.length > 0 && textAlignValue !== "undefined") {
                body.classList.add("reader-align-custom");
            }

            var nodes = doc2.querySelectorAll(
                isPageMode
                    ? pageTextSelector
                    : "p, h1, h2, h3, h4, h5, h6, li, article, section, blockquote, span, strong, em, b, i, small, sup, sub, cite, mark"
            );
            for (var n = 0; n < nodes.length; n++) {
                var el = nodes[n];
                el.style.setProperty("font-family", resolvedFontFamily, "important");
                el.style.setProperty("text-shadow", shadowStyle, "important");
                if (shouldApplyColor) {
                    el.style.setProperty("color", textColor, "important");
                    el.style.setProperty("-webkit-text-fill-color", textColor, "important");
                }
                el.style.removeProperty("text-align");
            }

            var inlineTextNodes = doc2.querySelectorAll("span, strong, em, b, i, small, sup, sub, cite, mark, .bionic-bold");
            for (var t = 0; t < inlineTextNodes.length; t++) {
                inlineTextNodes[t].style.setProperty("display", "inline", "important");
                inlineTextNodes[t].style.setProperty("white-space", "normal", "important");
                inlineTextNodes[t].style.setProperty("word-break", "normal", "important");
                inlineTextNodes[t].style.setProperty("overflow-wrap", "break-word", "important");
            }

            // hr styling is applied below for all modes
        }
    } catch (e) {
        log("Inline style override warning: " + e.message);
    }

    if (contents.document && contents.document.body) {
        excludeCoverImageOnce(contents);

        applyImageFilterToContents(contents, imageFilterStyle);

        var textAlignValue = s.textAlignment || "";

        // Only apply text alignment when not using publisher style, or when explicitly set
        if (!usePublisherStyle && textAlignValue && textAlignValue.length > 0 && textAlignValue !== "undefined") {
            var applyTextAlign = function() {
                try {
                    var textElements = contents.document.querySelectorAll("p, h1, h2, h3, h4, h5, h6, li, article, section, blockquote");
                    for (var i = 0; i < textElements.length; i++) {
                        textElements[i].style.textAlign = textAlignValue;
                    }
                } catch (e) {
                    log("Apply text align warning: " + e.message);
                }
            };

            applyTextAlign();

            if (!contents.__textAlignObserverInstalled) {
                contents.__textAlignObserverInstalled = true;
                contents.__textAlignApplier = applyTextAlign;

                try {
                    var observer = new MutationObserver(function(mutations) {
                        if (contents.__textAlignApplier) {
                            contents.__textAlignApplier();
                        }
                    });
                    observer.observe(contents.document.body, { childList: true, subtree: true, attributes: false });
                    contents.__textAlignObserver = observer;
                } catch (e) {
                    log("Text alignment observer warning: " + e.message);
                }
            }
        } else {
            try {
                var clearElements = contents.document.querySelectorAll("p, h1, h2, h3, h4, h5, h6, li, article, section, blockquote");
                for (var c = 0; c < clearElements.length; c++) {
                    clearElements[c].style.removeProperty("text-align");
                }
            } catch (e) {
                log("Clear text align warning: " + e.message);
            }
            if (contents.__textAlignObserver) {
                try {
                    contents.__textAlignObserver.disconnect();
                } catch (e) {}
                contents.__textAlignObserver = null;
            }
            contents.__textAlignObserverInstalled = false;
            contents.__textAlignApplier = null;
        }

        var applyHrStyles = function() {
            try {
                var hrNodes = contents.document.querySelectorAll("hr, [role='separator'], .separator, .divider");
                for (var h = 0; h < hrNodes.length; h++) {
                    var hrEl = hrNodes[h];
                    hrEl.style.setProperty("border", "none", "important");
                    hrEl.style.setProperty("border-top", "1px solid " + hrColor, "important");
                    hrEl.style.setProperty("border-color", hrColor, "important");
                    hrEl.style.setProperty("background-color", hrColor, "important");
                    hrEl.style.setProperty("color", hrColor, "important");
                    hrEl.style.setProperty("margin", "1.2em 0", "important");
                    hrEl.style.setProperty("opacity", "0.35", "important");
                }
            } catch (e) {
                log("HR styling warning: " + e.message);
            }
        };

        var applySelectorTypography = function(selector, style, decoration) {
            var nodes = contents.document.querySelectorAll(selector);
            for (var i = 0; i < nodes.length; i++) {
                var node = nodes[i];
                node.style.setProperty("font-family", style.fontFamily, "important");
                node.style.setProperty("color", style.color, "important");
                node.style.setProperty("-webkit-text-fill-color", style.color, "important");
                if (decoration) {
                    node.style.setProperty("text-decoration", decoration, "important");
                }
            }
        };

        var applyLinkStyles = function() {
            try {
                if (!usePublisherStyle) {
                    applySelectorTypography("p", paragraphStyle, null);
                    applySelectorTypography("h1", heading1Style, null);
                    applySelectorTypography("h2", heading2Style, null);
                    applySelectorTypography("h3", heading3Style, null);
                    applySelectorTypography("h4", heading4Style, null);
                    applySelectorTypography("h5", heading5Style, null);
                    applySelectorTypography("h6", heading6Style, null);
                }
                applySelectorTypography(externalLinkSelector, externalLinkStyle, linkDecoration);
                applySelectorTypography(internalLinkSelector, internalLinkStyle, chapterLinkDecoration);
            } catch (e) {
                log("Link styling warning: " + e.message);
            }
        };

        applyLinkStyles();
        applyHrStyles();

        try {
            var linkObserver = new MutationObserver(function(mutations) {
                for (var m = 0; m < mutations.length; m++) {
                    if (mutations[m].type === "childList") {
                        applyLinkStyles();
                        applyHrStyles();
                        break;
                    }
                }
            });
            linkObserver.observe(contents.document.body, { childList: true, subtree: true });
        } catch (e) {
            log("Link observer warning: " + e.message);
        }
    }

    if (s.focusText) {
        contents.addClass("bionic-enabled");
        if (contents.document && contents.document.body) {
            contents.document.body.classList.add("bionic-enabled");
            contents.document.documentElement.classList.add("bionic-enabled");
        }
        var bionicRoot = contents.document && contents.document.body ? contents.document.body : null;
        var currentBionicRatio = bionicRoot ? parseFloat(bionicRoot.dataset.bionicEmphasisRatio || "") : NaN;
        var hasBionicMarkup = false;
        try {
            hasBionicMarkup = !!(bionicRoot && bionicRoot.querySelector && bionicRoot.querySelector(".bionic-bold"));
        } catch (e) {
            hasBionicMarkup = false;
        }
        var needsBionicRebuild = !bionicRoot ||
            bionicRoot.dataset.bionicApplied !== "true" ||
            !hasBionicMarkup ||
            !isFinite(currentBionicRatio) ||
            Math.abs(currentBionicRatio - bionicEmphasis) > 0.001;

        if (needsBionicRebuild) {
            applyBionic(
                bionicRoot,
                s.focusTextBoldness || 700,
                s.focusTextColor || "inherit",
                bionicEmphasis
            );
        }
        updateBionicSpanStyles(contents.document, s.focusTextBoldness || 700, s.focusTextColor || "inherit");
    } else {
        contents.removeClass("bionic-enabled");
        if (contents.document && contents.document.body) {
            contents.document.body.classList.remove("bionic-enabled");
            contents.document.documentElement.classList.remove("bionic-enabled");
            contents.document.body.dataset.bionicApplied = "false";
        }
        clearBionicMarkup(contents.document.body);
        clearBionicSpanStyles(contents.document);
    }

    applyTtsHighlight(contents);
}
