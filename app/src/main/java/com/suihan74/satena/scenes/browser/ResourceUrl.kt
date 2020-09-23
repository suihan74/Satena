package com.suihan74.satena.scenes.browser

/** リソースURL情報 */
data class ResourceUrl(
    /** 対象URL */
    val url: String,

    /** AdBlock設定によりブロックされた */
    val blocked: Boolean
)
