package com.suihan74.utilities.bindings

import android.graphics.drawable.Drawable
import androidx.core.content.ContextCompat
import androidx.databinding.BindingAdapter
import androidx.recyclerview.widget.RecyclerView
import com.suihan74.hatenaLib.Entry
import com.suihan74.hatenaLib.MaintenanceEntry
import com.suihan74.hatenaLib.Notice
import com.suihan74.satena.models.Category
import com.suihan74.satena.scenes.entries2.CommentsAdapter
import com.suihan74.satena.scenes.entries2.EntriesAdapter
import com.suihan74.satena.scenes.entries2.InformationAdapter
import com.suihan74.satena.scenes.entries2.NoticesAdapter
import com.suihan74.utilities.DividerItemDecorator
import com.suihan74.utilities.extensions.toVisibility

/** 区切り線 */
@BindingAdapter("divider")
fun RecyclerView.setDivider(divider: Drawable?) {
    // 既存の区切り線を削除する
    repeat(itemDecorationCount) { idx ->
        val decor = getItemDecorationAt(idx)
        if (decor is DividerItemDecorator) {
            removeItemDecoration(decor)
        }
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

// ------ //

/** エントリリスト */
@BindingAdapter("category", "entries", "notices", "information", "readEntryIds")
fun RecyclerView.setEntries(
    category: Category,
    entries: List<Entry>?,
    notices: List<Notice>?,
    information: List<MaintenanceEntry>?,
    readEntryIds: Set<Long>?
) {
    when (category) {
        Category.Notices ->
            (adapter as? NoticesAdapter)?.submitNotices(notices)

        Category.Maintenance ->
            (adapter as? InformationAdapter)?.submitInformation(information)

        else ->
            (adapter as? EntriesAdapter)?.submitEntries(entries, readEntryIds)
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

