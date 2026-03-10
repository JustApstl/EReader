package com.dyu.ereader.util

fun normalizePath(path: String): String {
    val raw = path
        .substringBefore('#')
        .substringBefore('?')
        .replace('\\', '/')

    val stack = ArrayDeque<String>()
    for (segment in raw.split('/')) {
        when {
            segment.isBlank() || segment == "." -> Unit
            segment == ".." -> if (stack.isNotEmpty()) stack.removeLast()
            else -> stack.addLast(segment)
        }
    }

    return stack.joinToString("/")
}

fun resolveRelativePath(basePath: String, targetPath: String): String {
    if (targetPath.isBlank()) return normalizePath(basePath)
    if (targetPath.startsWith("/")) return normalizePath(targetPath.removePrefix("/"))

    val baseDir = basePath.substringBeforeLast('/', missingDelimiterValue = "")
    val merged = if (baseDir.isBlank()) targetPath else "$baseDir/$targetPath"
    return normalizePath(merged)
}
