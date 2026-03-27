function deliverSearchResults(token, results) {
    if (token !== activeSearchToken) {
        return;
    }
    var bridge = getBridge();
    if (bridge && bridge.onSearchResults) {
        try {
            bridge.onSearchResults(JSON.stringify(results || []));
        } catch (e) {
            log("Search results bridge error: " + e.message);
        }
    }
}

function percentageFromCfi(cfi) {
    try {
        if (book && book.locations && book.locations.percentageFromCfi && cfi) {
            var p = book.locations.percentageFromCfi(cfi);
            if (typeof p === "number" && !isNaN(p)) {
                return p;
            }
        }
    } catch (e) {
        // Ignore percentage errors
    }
    return 0;
}

function normalizeSearchResults(rawResults, query) {
    var results = [];
    if (!rawResults || !rawResults.length) {
        return results;
    }
    var qLower = String(query || "").toLowerCase();
    for (var i = 0; i < rawResults.length && results.length < 200; i++) {
        var item = rawResults[i] || {};
        var cfi = item.cfi || item.cfiRange || item.epubcfi || item.cfiBase || "";
        var href = item.chapterHref || item.href || "";
        if (!href && cfi && book && book.spine && book.spine.get) {
            try {
                var spineItem = book.spine.get(cfi);
                href = spineItem && spineItem.href ? spineItem.href : href;
            } catch (e) { }
        }
        var chapterTitle = item.chapterTitle || item.chapter || resolveChapterTitle(href);
        var text = item.textContext || item.excerpt || item.text || item.context || item.snippet || "";
        var matchIndex = qLower && text ? text.toLowerCase().indexOf(qLower) : -1;
        var matchStart = matchIndex >= 0 ? matchIndex : 0;
        var matchEnd = matchIndex >= 0 ? matchIndex + query.length : 0;
        var percent = item.percentage;
        if (typeof percent !== "number" || isNaN(percent)) {
            percent = percentageFromCfi(cfi);
        }
        results.push({
            chapterHref: href || "",
            chapterTitle: chapterTitle || "",
            textContext: text || "",
            matchStart: matchStart,
            matchEnd: matchEnd,
            percentage: typeof percent === "number" && !isNaN(percent) ? percent : 0
        });
    }
    return results;
}

function manualSearch(query) {
    if (!book || !book.spine) {
        return Promise.resolve([]);
    }
    var spineItems = book.spine.spineItems || book.spine.items || [];
    var total = spineItems.length || 1;
    var qLower = String(query || "").toLowerCase();
    var promises = spineItems.map(function(item, index) {
        if (!item || !item.load) {
            return Promise.resolve([]);
        }
        return item.load(book.load.bind(book)).then(function(contents) {
            var doc = contents && contents.document ? contents.document : contents;
            if (!doc) {
                return [];
            }
            var text = "";
            try {
                text = (doc.body && doc.body.textContent) ? doc.body.textContent : (doc.textContent || "");
            } catch (e) {
                text = "";
            }
            if (!text) {
                return [];
            }
            var results = [];
            var fromIndex = 0;
            while (results.length < 120) {
                var matchAt = text.toLowerCase().indexOf(qLower, fromIndex);
                if (matchAt === -1) {
                    break;
                }
                var contextStart = Math.max(0, matchAt - 40);
                var contextEnd = Math.min(text.length, matchAt + qLower.length + 40);
                var context = text.substring(contextStart, contextEnd).replace(/\s+/g, " ").trim();
                var approxPercent = total > 1 ? (index / total) : 0;
                results.push({
                    chapterHref: item.href || "",
                    chapterTitle: resolveChapterTitle(item.href || ""),
                    textContext: context,
                    matchStart: Math.max(0, matchAt - contextStart),
                    matchEnd: Math.max(0, matchAt - contextStart) + qLower.length,
                    percentage: approxPercent
                });
                fromIndex = matchAt + qLower.length;
            }
            if (item.unload) {
                try { item.unload(); } catch (e) { }
            }
            return results;
        }).catch(function() { return []; });
    });
    return Promise.all(promises).then(function(parts) {
        return [].concat.apply([], parts);
    });
}

window.searchInBook = function(rawQuery) {
    var query = String(rawQuery || "").trim();
    var token = ++activeSearchToken;

    if (!query) {
        deliverSearchResults(token, []);
        return;
    }
    if (!book) {
        deliverSearchResults(token, []);
        return;
    }

    book.ready.then(function() {
        return ensureLocationsGenerated().then(function() {
            if (book && typeof book.search === "function") {
                var searchPromise = book.search(query);
                return Promise.resolve(searchPromise).then(function(results) {
                    deliverSearchResults(token, normalizeSearchResults(results, query));
                });
            }
            return manualSearch(query).then(function(results) {
                deliverSearchResults(token, normalizeSearchResults(results, query));
            });
        });
    }).catch(function(e) {
        log("Search error: " + e.message);
        deliverSearchResults(token, []);
    });
};
