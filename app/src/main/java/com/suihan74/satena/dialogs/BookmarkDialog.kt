package com.suihan74.satena.dialogs

import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import androidx.fragment.app.DialogFragment
import com.suihan74.HatenaLib.Bookmark
import com.suihan74.HatenaLib.Entry
import com.suihan74.HatenaLib.HatenaClient
import com.suihan74.satena.R
import com.suihan74.satena.models.UserTagsKey
import com.suihan74.satena.scenes.bookmarks.BookmarksFragment
import com.suihan74.satena.scenes.entries.EntriesActivity
import com.suihan74.utilities.BookmarkCommentDecorator
import com.suihan74.utilities.SafeSharedPreferences
import com.suihan74.utilities.showToast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

open class BookmarkDialog : DialogFragment() {
    private lateinit var bookmarksFragment: BookmarksFragment

    companion object {
        private const val ARG_BOOKMARK = "bookmark"
        private const val ARG_ENTRY = "entry"
    }

    class Builder(
        val bookmarksFragment: BookmarksFragment,
        bookmark: Bookmark,
        entry: Entry
    ) {
        private var onRemoveBookmark: ((Bookmark)->Unit)? = null
        private var onChangeUserIgnoreState: ((Bookmark, Boolean)->Unit)? = null
        private var onTagUser: ((Bookmark)->Unit)? = null
        private var onSelectUrl: ((String)->Unit)? = null
        private var onSelectMenuItem: ((Bookmark, String)->Unit)? = null

        val arguments = Bundle().apply {
            putSerializable(ARG_BOOKMARK, bookmark)
            putSerializable(ARG_ENTRY, entry)
        }

        fun build() = BookmarkDialog().apply {
            bookmarksFragment = this@Builder.bookmarksFragment
            arguments = this@Builder.arguments
            onRemoveBookmark = this@Builder.onRemoveBookmark
            onChangeUserIgnoreState = this@Builder.onChangeUserIgnoreState
            onTagUser = this@Builder.onTagUser
            onSelectUrl = this@Builder.onSelectUrl
            onSelectMenuItem = this@Builder.onSelectMenuItem
        }

        fun setOnRemoveBookmark(action: (Bookmark)->Unit) : Builder {
            onRemoveBookmark = action
            return this@Builder
        }
        fun setOnChangeUserIgnoreState(action: (Bookmark, Boolean)->Unit) : Builder {
            onChangeUserIgnoreState = action
            return this@Builder
        }
        fun setOnTagUser(action: (Bookmark)->Unit) : Builder {
            onTagUser = action
            return this@Builder
        }
        fun setOnSelectUrl(action: (String)->Unit) : Builder {
            onSelectUrl = action
            return this@Builder
        }
        fun setOnSelectMenuItem(action: (Bookmark, String)->Unit) : Builder {
            onSelectMenuItem = action
            return this@Builder
        }
    }

    private var onRemoveBookmark: ((Bookmark)->Unit)? = null
    private var onChangeUserIgnoreState: ((Bookmark, Boolean)->Unit)? = null
    private var onTagUser: ((Bookmark)->Unit)? = null
    private var onSelectUrl: ((String)->Unit)? = null
    private var onSelectMenuItem: ((Bookmark, String)->Unit)? = null

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val inflater = LayoutInflater.from(context)
        setStyle(STYLE_NORMAL, R.style.AlertDialogStyle)

        val context = requireContext()
        val bookmark = arguments!!.getSerializable(ARG_BOOKMARK) as Bookmark
        val entry = arguments!!.getSerializable(ARG_ENTRY) as Entry

        val titleView = inflater.inflate(R.layout.dialog_title_bookmark, null).apply {
            setCustomTitle(bookmark)
        }

        val items = arrayListOf(context.getString(R.string.bookmark_show_user_entries) to { startUserEntriesActivity(bookmark) })
        if (HatenaClient.account?.name == bookmark.user) {
            items.add(context.getString(R.string.bookmark_remove) to { removeBookmark(entry, bookmark) })
        }
        else if (HatenaClient.signedIn()) {
            if (HatenaClient.ignoredUsers.contains(bookmark.user)) {
                items.add(context.getString(R.string.bookmark_unignore) to { unignoreUser(bookmark) })
            }
            else {
                items.add(context.getString(R.string.bookmark_ignore) to { ignoreUser(bookmark) })
            }
            items.add(context.getString(R.string.bookmark_report) to { reportUser(entry, bookmark) })
        }

        items.add(context.getString(R.string.bookmark_user_tags) to { tagUser(bookmark) })

        val analyzedBookmarkComment = BookmarkCommentDecorator.convert(bookmark.comment)
        for (url in analyzedBookmarkComment.urls) {
            items.add(url to { onSelectUrl?.invoke(url) ?: Unit })
        }

        return AlertDialog.Builder(context, R.style.AlertDialogStyle)
            .setCustomTitle(titleView)
            .setNegativeButton("Cancel", null)
            .setItems(items.map { it.first }.toTypedArray()) { _, which ->
                items[which].second()
                onSelectMenuItem?.invoke(bookmark, items[which].first)
            }
            .create()
    }

    private fun startUserEntriesActivity(bookmark: Bookmark) {
        val intent = Intent(context, EntriesActivity::class.java).apply {
            putExtra(EntriesActivity.EXTRA_DISPLAY_USER, bookmark.user)
        }
        context?.startActivity(intent)
    }

    private fun removeBookmark(entry: Entry, bookmark: Bookmark) {
        GlobalScope.launch(Dispatchers.Main) {
            try {
                HatenaClient.deleteBookmarkAsync(entry.url).await()
                context?.showToast(R.string.msg_remove_bookmark_succeeded)

                onRemoveBookmark?.invoke(bookmark)
            }
            catch (e: Exception) {
                Log.d("FailedToIgnoreUser", Log.getStackTraceString(e))
                context?.showToast(R.string.msg_remove_bookmark_failed)
            }
        }
    }

    private fun ignoreUser(bookmark: Bookmark) {
        GlobalScope.launch(Dispatchers.Main) {
            try {
                HatenaClient.ignoreUserAsync(bookmark.user).await()
                context?.showToast(R.string.msg_ignore_user_succeeded, bookmark.user)
                onChangeUserIgnoreState?.invoke(bookmark, true)
            }
            catch (e: Exception) {
                Log.d("FailedToIgnoreUser", Log.getStackTraceString(e))
                context?.showToast(R.string.msg_ignore_user_failed, bookmark.user)
            }
        }
    }

    private fun unignoreUser(bookmark: Bookmark) {
        GlobalScope.launch(Dispatchers.Main) {
            try {
                HatenaClient.unignoreUserAsync(bookmark.user).await()
                context?.showToast(R.string.msg_unignore_user_succeeded, bookmark.user)
                onChangeUserIgnoreState?.invoke(bookmark, false)
            }
            catch (e: Exception) {
                Log.d("FailedToIgnoreUser", Log.getStackTraceString(e))
                context?.showToast(R.string.msg_unignore_user_failed, bookmark.user)
            }

            try {
                HatenaClient.getIgnoredUsersAsync().await()
            }
            catch (e: Exception) {
                Log.d("FailedToUpdateIgnores", Log.getStackTraceString(e))
            }
        }
    }

    private fun reportUser(entry: Entry, bookmark: Bookmark) {
        val dialog = ReportDialogFragment.createInstance(entry, bookmark)
        dialog.show(bookmarksFragment.requireFragmentManager(), "report_dialog")
    }

    @Suppress("UseSparseArrays")
    private fun tagUser(bookmark: Bookmark) {
        val prefs = SafeSharedPreferences.create<UserTagsKey>(context)
        val userTagsContainer = bookmarksFragment.userTagsContainer
        val user = userTagsContainer.addUser(bookmark.user)
        val tags = userTagsContainer.tags
        val tagNames = tags.map { it.name }.toTypedArray()
        val states = tags.map { it.contains(user) }.toBooleanArray()
        val diffs = HashMap<Int, Boolean>()

        AlertDialog.Builder(context, R.style.AlertDialogStyle)
            .setTitle(getString(R.string.user_tags_dialog_title))
            .setMultiChoiceItems(tagNames, states) { _, which, isChecked ->
                diffs[which] = isChecked
            }
            .setNeutralButton(getString(R.string.user_tags_dialog_new_tag)) { _, _ ->
                val dialog = UserTagDialogFragment.createInstance { _, name, _ ->
                    if (userTagsContainer.containsTag(name)) {
                        context?.showToast(R.string.msg_user_tag_existed)
                        return@createInstance false
                    }
                    else {
                        val tag = userTagsContainer.addTag(name)
                        userTagsContainer.tagUser(user, tag)

                        prefs.edit {
                            putObject(UserTagsKey.CONTAINER, userTagsContainer)
                        }

                        val parentTabAdapter = bookmarksFragment.bookmarksTabAdapter
                        parentTabAdapter?.notifyItemChanged(bookmark)
                        context?.showToast(R.string.msg_user_tag_created_and_added_user, name, bookmark.user)
                        return@createInstance true
                    }
                }
                dialog.show(bookmarksFragment.fragmentManager!!, "dialog")
            }
            .setNegativeButton("Cancel", null)
            .setPositiveButton("OK") { _, _ ->
                if (diffs.isNotEmpty()) {
                    diffs.forEach {
                        val name = tagNames[it.key]
                        val tag = userTagsContainer.getTag(name)!!
                        if (it.value) {
                            userTagsContainer.tagUser(user, tag)
                        }
                        else {
                            userTagsContainer.unTagUser(user, tag)
                        }
                    }

                    prefs.edit {
                        putObject(UserTagsKey.CONTAINER, userTagsContainer)
                    }

                    context?.showToast(R.string.msg_user_tagged, user, user.tags.size)
                    onTagUser?.invoke(bookmark)
                }
            }
            .show()
    }
}
