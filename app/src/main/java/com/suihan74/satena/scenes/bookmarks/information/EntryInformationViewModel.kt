package com.suihan74.satena.scenes.bookmarks.information

import android.content.Context
import android.content.Intent
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import com.suihan74.hatenaLib.Entry
import com.suihan74.hatenaLib.HatenaClient
import com.suihan74.satena.scenes.bookmarks.BookmarksActivity
import com.suihan74.satena.startInnerBrowser

class EntryInformationViewModel(
    private val entry: LiveData<Entry?>
) : ViewModel() {

    fun onClickPageUrl(activity: BookmarksActivity) {
        val entry = entry.value ?: return
        activity.let {
            it.closeDrawer()
            it.startInnerBrowser(entry)
        }
    }

    fun onLongClickPageUrl(activity: BookmarksActivity) : Boolean {
        return entry.value?.let { entry ->
            activity.startActivity(
                Intent(Intent.ACTION_SEND).also {
                    it.putExtra(Intent.EXTRA_TEXT, entry.url)
                    it.type = "text/plain"
                }
            )
            true
        } ?: false
    }

    // ------ //

    /**
     * 表示中の画面のエントリ自体に遷移する
     */
    fun downFloor(activity: BookmarksActivity) {
        val entry = entry.value ?: return
        activity.closeDrawer()
        changeFloor(
            activity,
            HatenaClient.getEntryUrlFromCommentPageUrl(entry.url)
        )
    }

    /**
     * 表示中の画面のブコメページに遷移する
     */
    fun upFloor(activity: BookmarksActivity) {
        val entry = entry.value ?: return
        activity.closeDrawer()
        changeFloor(
            activity,
            HatenaClient.getCommentPageUrlFromEntryUrl(entry.url)
        )
    }

    private fun changeFloor(context: Context, url: String) {
        val entry = entry.value ?: return
        Intent(context, BookmarksActivity::class.java)
            .apply {
                putExtra(
                    BookmarksActivity.EXTRA_ENTRY_URL,
                    url
                )
                Regex("""(\S+)\s*のブックマーク\s*/\s*はてなブックマーク$""")
                    .find(entry.title)
                    ?.groupValues
                    ?.get(1)
                    ?.let {
                        putExtra(BookmarksActivity.EXTRA_TARGET_USER, it)
                    }
            }
            .let {
                context.startActivity(it)
            }
    }
}
