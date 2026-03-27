package com.dyu.ereader.data.format.mobi

object MobiNative {
    init {
        System.loadLibrary("mobi_native")
    }

    external fun extractToEpubDir(inputPath: String, outputDir: String): String
}
