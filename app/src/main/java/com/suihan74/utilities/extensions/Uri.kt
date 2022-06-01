package com.suihan74.utilities.extensions

import android.net.Uri

/** googleがキャッシュしているfaviconURLを取得する */
val Uri.faviconUrl : String
    get() =
        if (host.isNullOrBlank()) ""
        else "https://www.google.com/s2/favicons?domain=$host"

/**
 * 同一のfaviconを設定していると思われる階層を取得する
 */
val Uri.estimatedHierarchy : String?
    get() {
        val host = host ?: return null
        val segments = pathSegments ?: return host
        if (segments.isEmpty()) return host
        if (segments.size < 2) return "$host/${segments.first()}"
        return "$host/${segments.dropLast(1).joinToString("/")}"
    }
