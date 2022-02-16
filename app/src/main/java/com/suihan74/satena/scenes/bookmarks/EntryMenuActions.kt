package com.suihan74.satena.scenes.bookmarks

import android.content.Context
import android.content.Intent
import androidx.fragment.app.FragmentActivity
import com.suihan74.hatenaLib.Entry
import com.suihan74.satena.R
import com.suihan74.satena.models.EntryReadActionType
import com.suihan74.satena.scenes.bookmarks.repository.BookmarksRepository
import com.suihan74.satena.scenes.entries2.EntriesActivity
import com.suihan74.satena.scenes.entries2.EntryMenuActionsImplBasic
import com.suihan74.satena.scenes.post.BookmarkPostActivity
import com.suihan74.satena.scenes.preferences.favoriteSites.FavoriteSitesRepository
import com.suihan74.utilities.extensions.ContextExtensions.showToast
import com.suihan74.utilities.extensions.putObjectExtra
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/** ブコメに含まれるリンク文字列に対する処理 */
class EntryMenuActionsImplForBookmarks(
    private val bookmarksRepo : BookmarksRepository,
    private val favoriteSitesRepo : FavoriteSitesRepository
) : EntryMenuActionsImplBasic() {

    override fun showEntries(activity: FragmentActivity, entry: Entry) {
        val intent = Intent(activity, EntriesActivity::class.java).also {
            it.putExtra(EntriesActivity.EXTRA_SITE_URL, entry.rootUrl)
        }
        activity.startActivity(intent)
    }

    override fun favoriteEntry(context: Context, entry: Entry, coroutineScope: CoroutineScope) {
        coroutineScope.launch(Dispatchers.Main) {
            runCatching {
                favoriteSitesRepo.favoriteEntrySite(entry)
            }.onSuccess {
                context.showToast(R.string.msg_favorite_site_registration_succeeded)
            }
        }
    }

    override fun unfavoriteEntry(context: Context, entry: Entry, coroutineScope: CoroutineScope) {
        coroutineScope.launch(Dispatchers.Main) {
            runCatching {
                favoriteSitesRepo.unfavoriteEntrySite(entry)
            }.onSuccess {
                context.showToast(R.string.msg_favorite_site_deletion_succeeded)
            }
        }
    }

    override fun readLaterEntry(activity: FragmentActivity, entry: Entry, coroutineScope: CoroutineScope) {
        coroutineScope.launch(Dispatchers.Main) {
            runCatching {
                bookmarksRepo.readLater(entry)
            }.onSuccess {
                activity.showToast(R.string.msg_post_bookmark_succeeded)
            }.onFailure {
                activity.showToast(R.string.msg_post_bookmark_failed)
            }
        }
    }

    override fun readEntry(
        activity: FragmentActivity,
        entry: Entry,
        coroutineScope: CoroutineScope
    ) {
        coroutineScope.launch(Dispatchers.Main) {
            runCatching {
                bookmarksRepo.read(entry)
            }.onSuccess { (action, _) ->
                when (action) {
                    EntryReadActionType.DIALOG -> {
                        // ブクマ編集ダイアログに遷移する
                        // あとで戻ってきたときに画面を更新する --> Activity#onActivityResult
                        val intent = Intent(activity, BookmarkPostActivity::class.java).also {
                            it.putObjectExtra(BookmarkPostActivity.EXTRA_ENTRY, entry)
                        }
                        activity.startActivity(intent)
                    }
                    EntryReadActionType.REMOVE -> activity.showToast(R.string.msg_remove_bookmark_succeeded)
                    else -> activity.showToast(R.string.msg_post_bookmark_succeeded)
                }
            }.onFailure {
                activity.showToast(R.string.msg_post_bookmark_failed)
            }
        }
    }

    override fun deleteEntryBookmark(
        activity: FragmentActivity,
        entry: Entry,
        coroutineScope: CoroutineScope
    ) {
        coroutineScope.launch(Dispatchers.Main) {
            runCatching {
                bookmarksRepo.deleteBookmark(entry)
            }.onSuccess {
                activity.showToast(R.string.msg_remove_bookmark_succeeded)
            }.onFailure {
                activity.showToast(R.string.msg_remove_bookmark_failed)
            }
        }
    }

    override fun deleteReadMark(
        activity: FragmentActivity,
        entry: Entry,
        coroutineScope: CoroutineScope
    ) {
        // DO NOTHING
        // ブクマ画面では表示されないアクション
    }
}
