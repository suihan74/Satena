package com.suihan74.satena.scenes.browser

/**
 * ブロックするURLの設定
 */
data class BlockUrlSetting(
    /** queueの内容が含まれるURLをブロックする */
    val queue: String,

    /** queueが正規表現である */
    val isRegex: Boolean
)

/** ブロック設定を正規表現に変換する */
val List<BlockUrlSetting>.regex
    get() = Regex(
        joinToString(separator = "|") {
            if (it.isRegex) it.queue
            else Regex.escape(it.queue)
        }
    )
