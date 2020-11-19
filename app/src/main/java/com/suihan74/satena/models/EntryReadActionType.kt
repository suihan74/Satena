package com.suihan74.satena.models

import com.suihan74.satena.R

/** 「あとで読む」エントリを(エントリ一覧画面から)「読んだ」したときの挙動 */
enum class EntryReadActionType(
    val textId: Int
) {
    /** 無言ブクマ */
    SILENT_BOOKMARK(R.string.entry_read_action_silent),

    /** 「読んだ」タグをつけて無言ブクマ */
    READ_TAG(R.string.entry_read_action_read_tag),

    /** 任意の定型文でブコメする */
    BOILERPLATE(R.string.entry_read_action_boilerplate),

    /** ブコメ投稿ダイアログを表示する */
    DIALOG(R.string.entry_read_action_dialog),

    /** ブクマを削除 */
    REMOVE(R.string.entry_read_action_remove);

    companion object {
        fun fromOrdinal(int: Int) = values().getOrElse(int) { SILENT_BOOKMARK }
    }
}
