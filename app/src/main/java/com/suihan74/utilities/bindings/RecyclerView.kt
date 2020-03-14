package com.suihan74.utilities.bindings

import android.graphics.drawable.Drawable
import androidx.core.content.ContextCompat
import androidx.databinding.BindingAdapter
import androidx.recyclerview.widget.RecyclerView
import com.suihan74.hatenaLib.Entry
import com.suihan74.hatenaLib.Notice
import com.suihan74.satena.models.Category
import com.suihan74.satena.scenes.entries2.CategoriesAdapter
import com.suihan74.satena.scenes.entries2.CommentsAdapter
import com.suihan74.satena.scenes.entries2.EntriesAdapter
import com.suihan74.satena.scenes.entries2.NoticesAdapter
import com.suihan74.utilities.DividerItemDecorator
import com.suihan74.utilities.toVisibility

/** 区切り線 */
@BindingAdapter("divider")
fun RecyclerView.setDivider(divider: Drawable?) {
    // 既存の区切り線を削除する
    (0 until itemDecorationCount).mapNotNull { idx ->
        getItemDecorationAt(idx) as? DividerItemDecorator
    }.forEach { decor ->
        removeItemDecoration(decor)
    }

    if (divider != null) {
        addItemDecoration(
            DividerItemDecorator(divider)
        )
    }
}

/** 区切り線 */
@BindingAdapter("divider")
fun RecyclerView.setDivider(dividerId: Int) {
    setDivider(
        if (dividerId == 0) null
        else ContextCompat.getDrawable(context, dividerId)
    )
}


/** エントリリスト */
@BindingAdapter("category", "entries", "notices")
fun RecyclerView.setEntries(category: Category, entries: List<Entry>?, notices: List<Notice>?) {
    when (category) {
        Category.Notices ->
            (adapter as? NoticesAdapter)?.submitNotices(notices)

        else ->
            (adapter as? EntriesAdapter)?.submitEntries(entries)
    }
}

/** エントリについたブコメリスト */
@OptIn(ExperimentalStdlibApi::class)
@BindingAdapter("src")
fun RecyclerView.setEntryComments(entry: Entry?) {
    if (entry == null) return

    val comments = buildList {
        entry.bookmarkedData?.let { add(it) }
        entry.myHotEntryComments?.let { addAll(it) }
    }

    (adapter as? CommentsAdapter)?.submitComments(comments)
    visibility = (!comments.isNullOrEmpty()).toVisibility()
}

/** カテゴリリスト */
@BindingAdapter("src")
fun RecyclerView.setCategories(categories: Array<Category>?) {
    if (categories == null) return

    val adapter = adapter as CategoriesAdapter
    adapter.submitList(categories.toList())
}
