package com.suihan74.utilities.bindings

import androidx.databinding.BindingAdapter
import androidx.recyclerview.widget.RecyclerView
import com.suihan74.hatenaLib.Entry
import com.suihan74.satena.models.Category
import com.suihan74.satena.scenes.entries2.BookmarkCommentsAdapter
import com.suihan74.satena.scenes.entries2.CategoriesAdapter
import com.suihan74.satena.scenes.entries2.EntriesAdapter
import com.suihan74.utilities.toVisibility

/** エントリリスト */
@BindingAdapter("src")
fun RecyclerView.setEntries(entries: List<Entry>?) {
    (adapter as? EntriesAdapter)?.submitEntries(entries)
}

/** エントリについたブコメリスト */
@BindingAdapter("src")
fun RecyclerView.setEntryComments(entry: Entry?) {
    if (entry == null) return
    val comments = sequence {
        if (entry.bookmarkedData != null)
            yield(entry.bookmarkedData!!)

        if (entry.myHotEntryComments != null)
            yieldAll(entry.myHotEntryComments!!)
    }.toList()

    visibility = (!comments.isNullOrEmpty()).toVisibility()
    (adapter as? BookmarkCommentsAdapter)?.submitComments(comments)
}

/** カテゴリリスト */
@BindingAdapter("src")
fun RecyclerView.setCategories(categories: Array<Category>?) {
    if (categories == null) return

    val adapter = adapter as CategoriesAdapter
    adapter.submitList(categories.toList())
}
