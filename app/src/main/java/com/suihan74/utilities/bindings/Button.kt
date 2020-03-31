package com.suihan74.utilities.bindings

import android.widget.Button
import androidx.databinding.BindingAdapter
import com.suihan74.satena.models.TapEntryAction
import com.suihan74.satena.scenes.bookmarks2.BookmarksTabType

// --- PreferencesBookmarksFragment ---

/** 「最初に表示するタブ」のボタンテキスト */
@BindingAdapter("bookmarksTabType")
fun Button.setBookmarksTabTypeText(ordinal: Int?) {
    if (ordinal == null) return
    val tab = BookmarksTabType.fromInt(ordinal)
    setText(tab.textId)
}

/** 「リンク文字列をタップしたときの動作」のボタンテキスト */
@BindingAdapter("linkTapAction")
fun Button.setLinkTapActionText(ordinal: Int?) {
    if (ordinal == null) return
    val act = TapEntryAction.fromInt(ordinal)
    setText(act.titleId)
}
