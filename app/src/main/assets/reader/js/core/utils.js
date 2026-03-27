function normalizeHref(href) {
    if (!href) {
        return "";
    }
    return String(href).split("#")[0];
}

function cacheTocEntries(toc) {
    tocByHref = {};
    if (!toc || !toc.length) {
        return;
    }
    toc.forEach(function(item) {
        if (!item || !item.href) {
            return;
        }
        var key = normalizeHref(item.href);
        tocByHref[key] = (item.label || "").trim();
    });
}

function resolveChapterTitle(href) {
    if (!href) {
        return "";
    }
    var key = normalizeHref(href);
    return tocByHref[key] || tocByHref[href] || "";
}

function base64ToArrayBuffer(base64) {
    var binaryString = window.atob(base64);
    var len = binaryString.length;
    var bytes = new Uint8Array(len);
    for (var i = 0; i < len; i++) {
        bytes[i] = binaryString.charCodeAt(i);
    }
    return bytes.buffer;
}

function getCurrentFrame() {
    var frames = document.querySelectorAll("#viewer iframe");
    if (!frames || frames.length === 0) {
        return null;
    }
    return frames[frames.length - 1];
}

function frameElementForRange(range) {
    if (!range || !range.startContainer || !range.startContainer.ownerDocument) {
        return getCurrentFrame();
    }
    var rangeWindow = range.startContainer.ownerDocument.defaultView;
    return (rangeWindow && rangeWindow.frameElement) ? rangeWindow.frameElement : getCurrentFrame();
}

function frameElementForNode(node) {
    if (!node || !node.ownerDocument) {
        return getCurrentFrame();
    }
    var nodeWindow = node.ownerDocument.defaultView;
    return (nodeWindow && nodeWindow.frameElement) ? nodeWindow.frameElement : getCurrentFrame();
}

function anchorFromRect(rect, frameElement) {
    if (!rect) {
        return null;
    }

    var x = rect.left + (rect.width / 2);
    var y = rect.bottom;
    var frame = frameElement || getCurrentFrame();
    if (frame && frame.getBoundingClientRect) {
        var frameRect = frame.getBoundingClientRect();
        x += frameRect.left;
        y += frameRect.top;
    }
    if (!isFinite(x) || !isFinite(y)) {
        return null;
    }
    return { x: x, y: y };
}

function anchorFromClientPoint(clientX, clientY, frameElement) {
    var x = clientX;
    var y = clientY;
    var frame = frameElement || getCurrentFrame();
    if (frame && frame.getBoundingClientRect) {
        var frameRect = frame.getBoundingClientRect();
        x += frameRect.left;
        y += frameRect.top;
    }
    if (!isFinite(x) || !isFinite(y)) {
        return null;
    }
    return { x: x, y: y };
}

function anchorForCfi(cfi) {
    if (!rendition || !cfi || !rendition.getRange) {
        return null;
    }
    try {
        var range = rendition.getRange(cfi);
        if (!range) {
            return null;
        }
        return anchorFromRect(range.getBoundingClientRect(), frameElementForRange(range));
    } catch (e) {
        return null;
    }
}
