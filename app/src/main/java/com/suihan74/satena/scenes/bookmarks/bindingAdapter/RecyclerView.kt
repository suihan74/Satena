package com.suihan74.satena.scenes.bookmarks.bindingAdapter

import androidx.databinding.BindingAdapter
import androidx.recyclerview.widget.RecyclerView
import com.suihan74.satena.scenes.bookmarks.BookmarksAdapter
import com.suihan74.satena.scenes.bookmarks.Entity
import com.suihan74.utilities.RecyclerState
import com.suihan74.utilities.extensions.alsoAs

/** ブクマリストの表示 */
object BookmarksBindingAdapters {
    @JvmStatic
    @BindingAdapter("entities")
    fun bindBookmarks(
        rv: RecyclerView,
        entities: List<RecyclerState<Entity>>?
    ) {
        if (entities == null) return
        rv.adapter.alsoAs<BookmarksAdapter> { adapter ->
            adapter.setBookmarks(entities)
        }
    }
}
