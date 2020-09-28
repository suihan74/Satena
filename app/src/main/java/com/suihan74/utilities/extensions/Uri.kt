package com.suihan74.utilities.extensions

import android.net.Uri

/** googleがキャッシュしているfaviconURLを取得する */
val Uri.faviconUrl : String
    get() =
        if (host == null) ""
        else "https://www.google.com/s2/favicons?domain=$host"
