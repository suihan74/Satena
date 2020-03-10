package com.suihan74.utilities.bindings

import androidx.databinding.BindingAdapter
import androidx.recyclerview.widget.RecyclerView
import com.suihan74.hatenaLib.Entry
import com.suihan74.satena.models.Category
import com.suihan74.satena.scenes.entries2.CategoriesAdapter
import com.suihan74.satena.scenes.entries2.CommentsAdapter
import com.suihan74.satena.scenes.entries2.EntriesAdapter
import com.suihan74.utilities.toVisibility

/** エントリリスト */
@BindingAdapter("src")
fun RecyclerView.setEntries(entries: List<Entry>?) {
    (adapter as? EntriesAdapter)?.submitEntries(entries)
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
