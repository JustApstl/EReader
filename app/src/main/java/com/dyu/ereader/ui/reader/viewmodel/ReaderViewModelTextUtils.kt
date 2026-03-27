package com.dyu.ereader.ui.reader.viewmodel

internal fun computeWordOffsets(text: String): IntArray {
    if (text.isBlank()) return intArrayOf()
    val offsets = ArrayList<Int>()
    var inWord = false
    text.forEachIndexed { index, character ->
        if (!character.isWhitespace()) {
            if (!inWord) {
                offsets.add(index)
                inWord = true
            }
        } else {
            inWord = false
        }
    }
    return offsets.toIntArray()
}

internal fun wordIndexForChar(offsets: IntArray, start: Int): Int? {
    if (offsets.isEmpty()) return null
    if (start <= offsets.first()) return 0
    var low = 0
    var high = offsets.lastIndex
    while (low <= high) {
        val mid = (low + high) ushr 1
        val value = offsets[mid]
        if (value == start) return mid
        if (value < start) {
            low = mid + 1
        } else {
            high = mid - 1
        }
    }
    return (low - 1).coerceIn(0, offsets.lastIndex)
}

internal fun extractSentenceForWord(
    text: String,
    wordIndex: Int?,
    offsets: IntArray = computeWordOffsets(text)
): String {
    if (text.isBlank()) return ""
    val anchor = when {
        wordIndex == null -> 0
        wordIndex < 0 -> 0
        wordIndex >= offsets.size -> text.lastIndex.coerceAtLeast(0)
        else -> offsets[wordIndex]
    }
    var start = 0
    var end = text.length
    for (index in (anchor - 1).coerceAtLeast(0) downTo 0) {
        val character = text[index]
        if (character == '.' || character == '!' || character == '?' || character == '\n') {
            start = index + 1
            break
        }
    }
    for (index in anchor until text.length) {
        val character = text[index]
        if (character == '.' || character == '!' || character == '?' || character == '\n') {
            end = index + 1
            break
        }
    }
    return text.substring(start, end).replace("\\s+".toRegex(), " ").trim()
}
