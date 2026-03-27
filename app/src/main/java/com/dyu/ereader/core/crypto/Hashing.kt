package com.dyu.ereader.core.crypto

import java.security.MessageDigest

fun stableMd5(value: String): String {
    val digest = MessageDigest.getInstance("MD5").digest(value.toByteArray(Charsets.UTF_8))
    return digest.joinToString(separator = "") { "%02x".format(it) }
}
