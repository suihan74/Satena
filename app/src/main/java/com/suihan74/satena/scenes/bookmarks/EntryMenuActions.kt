package com.suihan74.satena.scenes.bookmarks

import android.app.Activity
import android.content.Context
import android.content.Intent
import com.suihan74.hatenaLib.Entry
import com.suihan74.satena.R
import com.suihan74.satena.models.EntryReadActionType
import com.suihan74.satena.scenes.bookmarks.repository.BookmarksRepository
import com.suihan74.satena.scenes.entries2.EntriesActivity
import com.suihan74.satena.scenes.entries2.EntryMenuActionsImplBasic
import com.suihan74.satena.scenes.post.BookmarkPostActivity
import com.suihan74.satena.scenes.preferences.favoriteSites.FavoriteSitesRepository
import com.suihan74.utilities.extensions.putObjectExtra
import com.suihan74.utilities.extensions.showToast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/** ブコメに含まれるリンク文字列に対する処理 */
class EntryMenuActionsImplForBookmarks(
    private val bookmarksRepo: BookmarksRepository,
    private val favoriteSitesRepo: FavoriteSitesRepository
) : EntryMenuActionsImplBasic() {

    override fun showEntries(activity: Activity, entry: Entry) {
        val intent = Intent(activity, EntriesActivity::class.java).also {
            it.putExtra(EntriesActivity.EXTRA_SITE_URL, entry.rootUrl)
        }
        activity.startActivity(intent)
    }

    override fun favoriteEntry(context: Context, entry: Entry, coroutineScope: CoroutineScope) {
        coroutineScope.launch(Dispatchers.Main) {
            val result = runCatching {
                favoriteSitesRepo.favoriteEntrySite(entry)
            }
            if (result.isSuccess) {
                context.showToast(R.string.msg_favorite_site_registration_succeeded)
            }
        }
    }

    override fun unfavoriteEntry(context: Context, entry: Entry, coroutineScope: CoroutineScope) {
        coroutineScope.launch(Dispatchers.Main) {
            val result = runCatching {
                favoriteSitesRepo.unfavoriteEntrySite(entry)
            }
            if (result.isSuccess) {
                context.showToast(R.string.msg_favorite_site_deletion_succeeded)
            }
        }
    }

    override fun readLaterEntry(activity: Activity, entry: Entry, coroutineScope: CoroutineScope) {
        coroutineScope.launch(Dispatchers.Main) {
            val result = runCatching {
                bookmarksRepo.readLater(entry)
            }

            if (result.isSuccess) {
                activity.showToast(R.string.msg_post_bookmark_succeeded)
            }
            else {
                activity.showToast(R.string.msg_post_bookmark_failed)
            }
        }
    }

    override fun readEntry(activity: Activity, entry: Entry, coroutineScope: CoroutineScope) {
        coroutineScope.launch(Dispatchers.Main) {
            val result = runCatching {
                bookmarksRepo.read(entry)
            }

            if (result.isSuccess) {
                val (action, _) = result.getOrNull()!!
                when (action) {
                    EntryReadActionType.REMOVE -> {
                        activity.showToast(R.string.msg_remove_bookmark_succeeded)
                    }

                    EntryReadActionType.DIALOG -> {
                        // ブクマ編集ダイアログに遷移する
                        // あとで戻ってきたときに画面を更新する --> Activity#onActivityResult
                        val intent = Intent(activity, BookmarkPostActivity::class.java).also {
                            it.putObjectExtra(BookmarkPostActivity.EXTRA_ENTRY, entry)
                        }
                        activity.startActivity(intent)
/*
                        activity.startActivityForResult(
                            intent,
                            BookmarkPostActivity.REQUEST_CODE
                        )
 */
                    }

                    else -> {
                        activity.showToast(R.string.msg_post_bookmark_succeeded)
                    }
                }
            }
            else {
                activity.showToast(R.string.msg_post_bookmark_failed)
            }
        }
    }

    override fun deleteEntryBookmark(
        activity: Activity,
        entry: Entry,
        coroutineScope: CoroutineScope
    ) {
        coroutineScope.launch(Dispatchers.Main) {
            val result = runCatching {
                bookmarksRepo.deleteBookmark(entry)
            }

            if (result.isSuccess) {
                activity.showToast(R.string.msg_remove_bookmark_succeeded)
            }
            else {
                activity.showToast(R.string.msg_remove_bookmark_failed)
            }
        }
    }
}
