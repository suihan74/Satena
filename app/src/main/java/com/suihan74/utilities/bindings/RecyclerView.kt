package com.suihan74.utilities.bindings

import androidx.databinding.BindingAdapter
import androidx.recyclerview.widget.RecyclerView
import com.suihan74.hatenaLib.Entry
import com.suihan74.satena.models.Category
import com.suihan74.satena.scenes.entries2.CategoriesAdapter
import com.suihan74.satena.scenes.entries2.EntriesAdapter

/** エントリリスト */
@BindingAdapter("src")
fun RecyclerView.setEntries(entries: List<Entry>?) {
    if (entries == null) return
    (adapter as? EntriesAdapter)?.submitEntries(entries)
}

/** カテゴリリスト */
@BindingAdapter("src")
fun RecyclerView.setCategories(categories: Array<Category>?) {
    if (categories == null) return

    val adapter = this.adapter as CategoriesAdapter
    adapter.submitList(categories.toList())
}
