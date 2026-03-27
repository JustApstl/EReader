package com.dyu.ereader.core.codec

import android.util.Base64

fun encodeNavArg(raw: String): String {
    return Base64.encodeToString(raw.toByteArray(Charsets.UTF_8), Base64.URL_SAFE or Base64.NO_WRAP)
}

fun decodeNavArg(encoded: String): String {
    return String(Base64.decode(encoded, Base64.URL_SAFE or Base64.NO_WRAP), Charsets.UTF_8)
}
