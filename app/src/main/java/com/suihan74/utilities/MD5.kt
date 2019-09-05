package com.suihan74.utilities

import java.math.BigInteger
import java.security.MessageDigest

fun getMd5Bytes(src: ByteArray) : ByteArray {
    val md = MessageDigest.getInstance("MD5")
    md.update(src)
    return md.digest()
}

fun getMd5(src: String) = getMd5(src.toByteArray())

fun getMd5(srcBytes: ByteArray) : String {
    val md5bytes = getMd5Bytes(srcBytes)
    val bigInt = BigInteger(1, md5bytes)
    return bigInt.toString(16)
}
