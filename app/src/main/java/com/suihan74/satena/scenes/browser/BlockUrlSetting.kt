package com.suihan74.satena.scenes.browser

/**
 * ブロックするURLの設定
 */
data class BlockUrlSetting(
    /** patternの内容が含まれるURLをブロックする */
    val pattern: String,

    /** patternが正規表現である */
    val isRegex: Boolean
)

/** ブロック設定を正規表現に変換する */
val List<BlockUrlSetting>.regex
    get() = Regex(
        joinToString(separator = "|") {
            if (it.isRegex) it.pattern
            else Regex.escape(it.pattern)
        }
    )
