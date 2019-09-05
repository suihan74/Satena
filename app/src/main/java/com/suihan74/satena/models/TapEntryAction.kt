package com.suihan74.satena.models

enum class TapEntryAction(val int: Int, val title: String) {
    SHOW_COMMENTS(0, "コメント一覧を開く"),
    SHOW_PAGE(1, "ページを開く"),
    SHOW_PAGE_IN_BROWSER(2, "ページを外部ブラウザで開く"),
    SHOW_MENU(3, "メニューダイアログを開く");

    companion object {
        fun fromInt(i : Int) : TapEntryAction = values().firstOrNull { it.int == i } ?: SHOW_COMMENTS
    }
}
