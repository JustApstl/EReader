(function init() {
    var bridge = getBridge();
    if (!bridge || !bridge.getBookBase64) {
        setTimeout(init, 100);
        return;
    }

    try {
        if (bridge.getSettings) {
            var settingsStr = bridge.getSettings();
            if (settingsStr) {
                savedSettings = JSON.parse(settingsStr);
            }
        }

        requestBookData(0);
    } catch (e) {
        log("Init Error: " + e.message);
    }
})();
