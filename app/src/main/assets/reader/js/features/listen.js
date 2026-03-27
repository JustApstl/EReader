function clearTtsWordHighlight() {
    if (ttsActiveWord && ttsActiveWord.classList) {
        ttsActiveWord.classList.remove("tts-word-active");
    }
    ttsActiveWord = null;
    if (ttsActiveSentence && ttsActiveSentence.classList) {
        ttsActiveSentence.classList.remove("tts-sentence-active");
    }
    ttsActiveSentence = null;
}

var lastExtractedTextLength = 0;

function clearTtsWordWrappers(doc) {
    if (!doc) {
        return;
    }
    var sentenceSpans = doc.querySelectorAll("span.tts-sentence");
    for (var i = 0; i < sentenceSpans.length; i++) {
        var sentenceSpan = sentenceSpans[i];
        var sentenceParent = sentenceSpan.parentNode;
        if (!sentenceParent) {
            continue;
        }
        sentenceParent.replaceChild(doc.createTextNode(sentenceSpan.textContent || ""), sentenceSpan);
    }
    var spans = doc.querySelectorAll("span.tts-word");
    for (var i = 0; i < spans.length; i++) {
        var span = spans[i];
        var parent = span.parentNode;
        if (!parent) {
            continue;
        }
        parent.replaceChild(doc.createTextNode(span.textContent || ""), span);
    }
    if (doc.body) {
        doc.body.normalize();
    }
}

function prepareTtsWords() {
    if (!rendition) {
        return;
    }
    ttsWordElements = [];
    clearTtsWordHighlight();
    try {
        var pageRange = getCurrentPageRange();
        var count = buildTtsWordElements(pageRange);
        if (count === 0 && pageRange) {
            ttsWordElements = [];
            count = buildTtsWordElements(null);
        }
    } catch (e) {
        log("TTS word prep warning: " + e.message);
    }
}

window.prepareTtsWords = prepareTtsWords;

function buildTtsWordElements(pageRange) {
    var total = 0;
    rendition.getContents().forEach(function(content) {
        var doc = content && content.document;
        if (!doc || !doc.body) {
            return;
        }
        clearTtsWordWrappers(doc);
        var walker = doc.createTreeWalker(doc.body, NodeFilter.SHOW_TEXT, null, false);
        var textNodes = [];
        var node;
        while ((node = walker.nextNode())) {
            if (!node.nodeValue || !node.nodeValue.trim()) {
                continue;
            }
            if (pageRange && node.ownerDocument === pageRange.startContainer.ownerDocument) {
                try {
                    if (!pageRange.intersectsNode(node)) {
                        continue;
                    }
                } catch (e) {
                    // ignore range errors
                }
            }
            var parent = node.parentNode;
            var tagName = parent && parent.tagName ? parent.tagName.toLowerCase() : "";
            if (tagName === "script" || tagName === "style") {
                continue;
            }
            textNodes.push(node);
        }
        textNodes.forEach(function(textNode) {
            var text = textNode.nodeValue;
            if (!text || !text.trim()) {
                return;
            }
            var fragment = doc.createDocumentFragment();
            var sentenceParts = text.match(/[^.!?\n]+[.!?\n]?\s*/g) || [text];
            for (var sentenceIndex = 0; sentenceIndex < sentenceParts.length; sentenceIndex++) {
                var sentenceText = sentenceParts[sentenceIndex];
                if (!sentenceText) {
                    continue;
                }
                var sentenceSpan = doc.createElement("span");
                sentenceSpan.className = "tts-sentence";
                var parts = sentenceText.split(/(\s+)/);
                var hasWord = false;
                for (var i = 0; i < parts.length; i++) {
                    var part = parts[i];
                    if (part.length === 0) {
                        continue;
                    }
                    if (part.trim().length === 0) {
                        sentenceSpan.appendChild(doc.createTextNode(part));
                    } else {
                        var span = doc.createElement("span");
                        span.className = "tts-word";
                        span.setAttribute("data-tts-index", String(ttsWordElements.length));
                        span.textContent = part;
                        ttsWordElements.push(span);
                        total += 1;
                        hasWord = true;
                        sentenceSpan.appendChild(span);
                    }
                }
                if (hasWord) {
                    fragment.appendChild(sentenceSpan);
                } else {
                    fragment.appendChild(doc.createTextNode(sentenceText));
                }
            }
            var parentNode = textNode.parentNode;
            if (parentNode) {
                parentNode.replaceChild(fragment, textNode);
            }
        });
        doc.body.normalize();
    });
    return total;
}

window.setListenActive = function(active) {
    listenActive = !!active;
    if (!listenActive) {
        textExtractionRetryCount = 0;
        if (textExtractionTimer) {
            clearTimeout(textExtractionTimer);
            textExtractionTimer = null;
        }
        if (rendition) {
            rendition.getContents().forEach(function(content) {
                if (content && content.document) {
                    clearTtsWordWrappers(content.document);
                }
            });
        }
        ttsWordElements = [];
        clearTtsWordHighlight();
        return;
    }
    prepareTtsWords();
    window.scheduleListenExtraction();
};

window.setTtsWordIndex = function(index) {
    if (!ttsWordElements || ttsWordElements.length === 0) {
        prepareTtsWords();
    }
    clearTtsWordHighlight();
    var idx = parseInt(index, 10);
    if (isNaN(idx) || idx < 0 || idx >= ttsWordElements.length) {
        return;
    }
    var el = ttsWordElements[idx];
    if (el && el.classList) {
        el.classList.add("tts-word-active");
        ttsActiveWord = el;
        var sentence = el.closest ? el.closest(".tts-sentence") : el.parentNode;
        if (sentence && sentence.classList) {
            sentence.classList.add("tts-sentence-active");
            ttsActiveSentence = sentence;
        }
        try {
            var rect = el.getBoundingClientRect();
            if (rect.bottom < 0 || rect.top > (window.innerHeight || document.documentElement.clientHeight)) {
                el.scrollIntoView({ block: "center", inline: "nearest", behavior: "smooth" });
            }
        } catch (e) {
            // ignore scroll errors
        }
    }
};

function collectCurrentText() {
    if (!rendition) {
        return "";
    }

    var combinedText = "";
    try {
        if (listenActive) {
            prepareTtsWords();
        }

        if (savedSettings && savedSettings.readingMode === "PAGE") {
            var visibleText = collectPagedTextFromRange();
            return visibleText || "";
        }

        var parts = rendition.getContents().map(function(content) {
            if (!content || !content.document || !content.document.body) {
                return "";
            }
            return (content.document.body.innerText || "").trim();
        }).filter(function(text) {
            return text.length > 0;
        });

        combinedText = parts.join("\n\n").replace(/\s+/g, " ").trim();
    } catch (e) {
        log("getCurrentText error: " + e.message);
    }

    if (!combinedText) {
        try {
            combinedText = (document.getElementById("viewer").innerText || "")
                .replace(/\s+/g, " ")
                .trim();
        } catch (e) {
            log("getCurrentText fallback error: " + e.message);
        }
    }
    return combinedText || "";
}

function getCurrentPageRange() {
    if (!rendition || !rendition.currentLocation) {
        return null;
    }
    if (!savedSettings || savedSettings.readingMode !== "PAGE") {
        return null;
    }
    try {
        var location = rendition.currentLocation();
        if (!location || !location.start || !location.end) {
            return null;
        }
        var startRange = rendition.getRange(location.start.cfi);
        var endRange = rendition.getRange(location.end.cfi);
        if (!startRange || !endRange) {
            return null;
        }
        var doc = startRange.startContainer && startRange.startContainer.ownerDocument;
        if (!doc || !doc.createRange) {
            return null;
        }
        var range = doc.createRange();
        range.setStart(startRange.startContainer, startRange.startOffset);
        range.setEnd(endRange.endContainer, endRange.endOffset);
        return range;
    } catch (e) {
        log("page range error: " + e.message);
        return null;
    }
}

function collectPagedTextFromRange() {
    var range = getCurrentPageRange();
    if (!range) {
        return "";
    }
    try {
        var text = range.toString().replace(/\s+/g, " ").trim();
        if (text.length < 80) {
            var visible = collectVisibleTextFromContents();
            if (visible) {
                text = visible;
            }
        }
        if (text.length > 4000) {
            text = text.slice(0, 4000);
        }
        return text;
    } catch (e) {
        log("page range text error: " + e.message);
        return "";
    }
}

function collectVisibleTextFromContents() {
    if (!rendition || !rendition.getContents) {
        return "";
    }
    var chunks = [];
    try {
        rendition.getContents().forEach(function(content) {
            var doc = content && content.document;
            if (!doc || !doc.body) {
                return;
            }
            var win = content.window || window;
            var viewportHeight = win.innerHeight || doc.documentElement.clientHeight || 0;
            var viewportWidth = win.innerWidth || doc.documentElement.clientWidth || 0;
            var walker = doc.createTreeWalker(doc.body, NodeFilter.SHOW_TEXT, null, false);
            var node;
            while ((node = walker.nextNode())) {
                if (!node.nodeValue || !node.nodeValue.trim()) {
                    continue;
                }
                var range = doc.createRange();
                range.selectNodeContents(node);
                var rect = range.getBoundingClientRect();
                if (rect.bottom < 0 || rect.top > viewportHeight || rect.right < 0 || rect.left > viewportWidth) {
                    continue;
                }
                chunks.push(node.nodeValue.trim());
                if (chunks.join(" ").length > 4000) {
                    break;
                }
            }
        });
    } catch (e) {
        log("visible text error: " + e.message);
    }
    return chunks.join(" ").replace(/\s+/g, " ").trim();
}

function deliverTextExtraction(text) {
    var bridge = getBridge();
    lastExtractedTextLength = text ? text.length : 0;
    if (bridge && bridge.onTextExtracted) {
        bridge.onTextExtracted(text || "");
    }
}

window.scheduleListenExtraction = function() {
    if (!listenActive) {
        return;
    }
    if (textExtractionTimer) {
        clearTimeout(textExtractionTimer);
    }
    textExtractionTimer = setTimeout(function() {
        window.requestTextExtraction();
    }, 220);
};

window.requestTextExtraction = function() {
    var text = collectCurrentText();
    if (!text || text.trim().length === 0) {
        if (textExtractionRetryCount < TEXT_EXTRACTION_MAX_RETRIES) {
            textExtractionRetryCount += 1;
            if (textExtractionTimer) {
                clearTimeout(textExtractionTimer);
            }
            textExtractionTimer = setTimeout(function() {
                window.requestTextExtraction();
            }, TEXT_EXTRACTION_RETRY_DELAY);
            return;
        }
    }
    textExtractionRetryCount = 0;
    deliverTextExtraction(text);
};

window.getCurrentText = function() {
    window.requestTextExtraction();
};

function applyTtsHighlight(contents) {
    if (!contents || !contents.document || !contents.document.body) {
        return;
    }
    try {
        var nodes = contents.document.querySelectorAll("p, li, h1, h2, h3, h4, h5, h6, blockquote");
        for (var i = 0; i < nodes.length; i++) {
            if (ttsHighlightActive) {
                nodes[i].classList.add("tts-highlight");
            } else {
                nodes[i].classList.remove("tts-highlight");
            }
        }
    } catch (e) {
        log("TTS highlight warning: " + e.message);
    }
}

window.setTtsHighlightActive = function(active) {
    ttsHighlightActive = !!active;
    if (rendition) {
        rendition.getContents().forEach(function(content) {
            applyTtsHighlight(content);
        });
    }
};
