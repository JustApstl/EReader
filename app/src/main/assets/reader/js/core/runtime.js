var book = null;
var rendition = null;
var savedSettings = null;
var initialJumpDone = false;
var bookData = null;
var renderedHighlightCfis = [];
var renderedNoteCfis = [];
var customFontCacheUri = "";
var customFontCacheDataUrl = "";
var locationsReadyPromise = null;
var openBookRetryCount = 0;
var ttsHighlightActive = false;
var listenActive = false;
var ttsWordElements = [];
var ttsActiveWord = null;
var ttsActiveSentence = null;
var textExtractionRetryCount = 0;
var textExtractionTimer = null;
var TEXT_EXTRACTION_MAX_RETRIES = 5;
var TEXT_EXTRACTION_RETRY_DELAY = 180;
var menuState = {
    selectionOpen: false,
    highlightOpen: false,
    noteOpen: false
};
var activeSelection = null;
var activeHighlight = null;
var activeNote = null;
var anchorUpdateScheduled = false;
var COVER_ANCHOR = "__cover__";
var tocByHref = {};
var activeSearchToken = 0;
var coverImageExcluded = false;
var bookCacheKey = "";
var activePeekDirection = null;
var activePeekProgress = 0;
var pageTurnCompositor = null;

function getBridge() {
    return window.AndroidReader || (window.parent && window.parent.AndroidReader);
}

function log(message) {
    var bridge = getBridge();
    if (bridge && bridge.log) {
        bridge.log(message);
    }
    console.log(message);
}
