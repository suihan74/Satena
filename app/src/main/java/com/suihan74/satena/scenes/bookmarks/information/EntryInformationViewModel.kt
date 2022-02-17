package com.suihan74.satena.scenes.bookmarks.information

import android.content.Context
import android.content.Intent
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import com.suihan74.hatenaLib.Entry
import com.suihan74.hatenaLib.HatenaClient
import com.suihan74.satena.models.PreferenceKey
import com.suihan74.satena.models.TapEntryAction
import com.suihan74.satena.scenes.bookmarks.BookmarksActivity
import com.suihan74.satena.scenes.bookmarks.EntryMenuActionsImplForBookmarks
import com.suihan74.satena.scenes.bookmarks.repository.BookmarksRepository
import com.suihan74.satena.scenes.entries2.EntriesActivity
import com.suihan74.satena.scenes.entries2.EntryMenuActions
import com.suihan74.satena.scenes.entries2.dialog.ShareEntryDialog
import com.suihan74.satena.scenes.preferences.favoriteSites.FavoriteSitesRepository
import com.suihan74.satena.startInnerBrowser
import com.suihan74.utilities.extensions.alsoAs

class EntryInformationViewModel(
    private val bookmarksRepo: BookmarksRepository,
    favoriteSitesRepo: FavoriteSitesRepository
) : ViewModel() {
    private val entry: LiveData<Entry?> = bookmarksRepo.entry

    /**
     * ページURL部分をクリックしたときの動作
     *
     * ドロワを閉じてアプリ内ブラウザでURLを開く
     */
    fun onClickPageUrl(activity: BookmarksActivity) {
        val entry = entry.value ?: return
        activity.let {
            it.closeDrawer()
            it.startInnerBrowser(entry)
        }
    }

    /**
     * ページURL部分をクリックしたときの動作
     *
     * 共有する
     */
    fun onLongClickPageUrl(activity: BookmarksActivity) : Boolean {
        return entry.value?.let { entry ->
            ShareEntryDialog.createInstance(entry)
                .show(activity.supportFragmentManager, null)
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

    // ------ //

    /** タグリスト用のアダプタを生成する */
    fun tagsAdapter(context: Context) = TagsAdapter().also { adapter ->
        adapter.setOnItemClickedListener { tag ->
            context.alsoAs<BookmarksActivity> { it.closeDrawer() }
            val intent = Intent(context, EntriesActivity::class.java).apply {
                putExtra(EntriesActivity.EXTRA_SEARCH_TAG, tag)
            }
            context.startActivity(intent)
        }
    }

    /** 関連エントリ用のアダプタを生成する */
    fun relatedEntriesAdapter(
        fragment: Fragment,
        lifecycleOwner: LifecycleOwner
    ) = RelatedEntriesAdapter(lifecycleOwner).also { adapter ->
        fun invokeClickAction(fragment: Fragment, entry: Entry, actionKey: PreferenceKey) {
            val tapAction = TapEntryAction.fromId(
                bookmarksRepo.prefs.getInt(actionKey)
            )
            entryMenuActions.invokeEntryClickedAction(
                fragment.requireActivity(),
                entry,
                tapAction,
                fragment.childFragmentManager
            )
        }

        adapter.multipleClickDuration = bookmarksRepo.prefs.getLong(PreferenceKey.ENTRY_MULTIPLE_TAP_DURATION)

        adapter.setOnItemClickedListener { entry ->
            invokeClickAction(fragment, entry, PreferenceKey.ENTRY_SINGLE_TAP_ACTION)
        }
        adapter.setOnItemLongClickedListener { entry ->
            invokeClickAction(fragment, entry, PreferenceKey.ENTRY_LONG_TAP_ACTION)
            true
        }
        adapter.setOnItemMultipleClickedListener { entry, _ ->
            invokeClickAction(fragment, entry, PreferenceKey.ENTRY_MULTIPLE_TAP_ACTION)
        }
        adapter.setOnItemEdgeClickedListener { entry ->
            invokeClickAction(fragment, entry, PreferenceKey.ENTRY_EDGE_SINGLE_TAP_ACTION)
        }
        adapter.setOnItemEdgeLongClickedListener { entry ->
            invokeClickAction(fragment, entry, PreferenceKey.ENTRY_EDGE_LONG_TAP_ACTION)
            true
        }
        adapter.setOnItemEdgeMultipleClickedListener { entry, _ ->
            invokeClickAction(fragment, entry, PreferenceKey.ENTRY_EDGE_MULTIPLE_TAP_ACTION)
        }
    }

    // ------ //

    private val entryMenuActions : EntryMenuActions =
        EntryMenuActionsImplForBookmarks(bookmarksRepo, favoriteSitesRepo)

    private fun openEntryMenuDialog(entry: Entry, fragmentManager: FragmentManager) {
        entryMenuActions.openMenuDialog(entry, fragmentManager)
    }
}
