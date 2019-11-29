package com.suihan74.satena.dialogs

import android.app.AlertDialog
import android.app.Dialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import com.suihan74.HatenaLib.Bookmark
import com.suihan74.HatenaLib.Entry
import com.suihan74.HatenaLib.HatenaClient
import com.suihan74.satena.R
import com.suihan74.satena.models.TaggedUser
import com.suihan74.satena.models.UserTagsKey
import com.suihan74.satena.scenes.bookmarks.BookmarksActivity
import com.suihan74.satena.scenes.entries.EntriesActivity
import com.suihan74.utilities.BookmarkCommentDecorator
import com.suihan74.utilities.SafeSharedPreferences
import com.suihan74.utilities.showToast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

open class BookmarkDialog : DialogFragment() {
    companion object {
        private const val ARG_BOOKMARK = "bookmark"
        private const val ARG_ENTRY = "entry"
    }

    class Builder(
        bookmark: Bookmark,
        entry: Entry
    ) {
        val arguments = Bundle().apply {
            putSerializable(ARG_BOOKMARK, bookmark)
            putSerializable(ARG_ENTRY, entry)
        }

        fun build() = BookmarkDialog().apply {
            arguments = this@Builder.arguments
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val inflater = LayoutInflater.from(context)
        setStyle(STYLE_NORMAL, R.style.AlertDialogStyle)

        val context = requireContext()
        val bookmark = arguments!!.getSerializable(ARG_BOOKMARK) as Bookmark
        val entry = arguments!!.getSerializable(ARG_ENTRY) as Entry

        val listener = parentFragment as? Listener ?: activity as? Listener

        val titleView = inflater.inflate(R.layout.dialog_title_bookmark, null).apply {
            setCustomTitle(bookmark)
        }

        val items = arrayListOf(context.getString(R.string.bookmark_show_user_entries) to { startUserEntriesActivity(bookmark) })
        if (HatenaClient.account?.name == bookmark.user) {
            items.add(context.getString(R.string.bookmark_remove) to { removeBookmark(listener, entry, bookmark) })
        }
        else if (HatenaClient.signedIn()) {
            if (HatenaClient.ignoredUsers.contains(bookmark.user)) {
                items.add(context.getString(R.string.bookmark_unignore) to { unignoreUser(listener, bookmark) })
            }
            else {
                items.add(context.getString(R.string.bookmark_ignore) to { ignoreUser(listener, bookmark) })
            }
            items.add(context.getString(R.string.bookmark_report) to { reportUser(entry, bookmark) })
        }

        items.add(context.getString(R.string.bookmark_user_tags) to { tagUser(listener, bookmark) })

        val analyzedBookmarkComment = BookmarkCommentDecorator.convert(bookmark.comment)
        for (url in analyzedBookmarkComment.urls) {
            items.add(url to { listener?.onSelectUrl(url) ?: Unit })
        }

        return AlertDialog.Builder(context, R.style.AlertDialogStyle)
            .setCustomTitle(titleView)
            .setNegativeButton("Cancel", null)
            .setItems(items.map { it.first }.toTypedArray()) { _, which ->
                items[which].second()
                listener?.onSelectMenuItem(bookmark, items[which].first)
            }
            .create()
    }

    private fun startUserEntriesActivity(bookmark: Bookmark) {
        val intent = Intent(context, EntriesActivity::class.java).apply {
            putExtra(EntriesActivity.EXTRA_DISPLAY_USER, bookmark.user)
        }
        context?.startActivity(intent)
    }

    private fun removeBookmark(listener: Listener?, entry: Entry, bookmark: Bookmark) {
        GlobalScope.launch(Dispatchers.Main) {
            try {
                HatenaClient.deleteBookmarkAsync(entry.url).await()
                context?.showToast(R.string.msg_remove_bookmark_succeeded)

                listener?.onRemoveBookmark(bookmark)
            }
            catch (e: Exception) {
                Log.d("FailedToIgnoreUser", Log.getStackTraceString(e))
                context?.showToast(R.string.msg_remove_bookmark_failed)
            }
        }
    }

    private fun ignoreUser(listener: Listener?, bookmark: Bookmark) {
        GlobalScope.launch(Dispatchers.Main) {
            try {
                HatenaClient.ignoreUserAsync(bookmark.user).await()
                context?.showToast(R.string.msg_ignore_user_succeeded, bookmark.user)
                listener?.onChangeUserIgnoreState(bookmark, true)
            }
            catch (e: Exception) {
                Log.d("FailedToIgnoreUser", Log.getStackTraceString(e))
                context?.showToast(R.string.msg_ignore_user_failed, bookmark.user)
            }
        }
    }

    private fun unignoreUser(listener: Listener?, bookmark: Bookmark) {
        GlobalScope.launch(Dispatchers.Main) {
            try {
                HatenaClient.unignoreUserAsync(bookmark.user).await()
                context?.showToast(R.string.msg_unignore_user_succeeded, bookmark.user)
                listener?.onChangeUserIgnoreState(bookmark, false)
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
        val bookmarksFragment = (activity as BookmarksActivity).bookmarksFragment!!
        val dialog = ReportDialogFragment.createInstance(entry, bookmark)
        dialog.show(bookmarksFragment.requireFragmentManager(), "report_dialog")
    }

    @Suppress("UseSparseArrays")
    private fun tagUser(listener: Listener?, bookmark: Bookmark) {
        val bookmarksFragment = (activity as BookmarksActivity).bookmarksFragment!!
        val userTagsContainer = bookmarksFragment.userTagsContainer
        val user = userTagsContainer.addUser(bookmark.user)
        val tags = userTagsContainer.tags
        val tagNames = tags.map { it.name }.toTypedArray()
        val states = tags.map { it.contains(user) }.toBooleanArray()

        AlertDialogFragment.Builder(R.style.AlertDialogStyle)
            .setTitle(R.string.user_tags_dialog_title)
            .setNeutralButton(R.string.user_tags_dialog_new_tag)
            .setNegativeButton(R.string.dialog_cancel)
            .setPositiveButton(R.string.dialog_ok)
            .setMultiChoiceItems(tagNames, states)
            .setAdditionalData("bookmark", bookmark)
            .show(listener!!.fragmentManagerForDialog, "user_tag_dialog")
    }


    interface Listener : AlertDialogListener {
        val fragmentManagerForDialog : FragmentManager

        fun onRemoveBookmark(bookmark: Bookmark) {}
        fun onChangeUserIgnoreState(bookmark: Bookmark, state: Boolean) {}
        fun onTagUser(bookmark: Bookmark) {}
        fun onSelectUrl(url: String) {}
        fun onSelectMenuItem(bookmark: Bookmark, text: String) {}

        companion object {
            fun onCompleteSelectTags(activity: BookmarksActivity, listener: Listener?, dialog: AlertDialogFragment) {
                val bookmark = dialog.getAdditionalData<Bookmark>("bookmark")!!

                val diffs =
                    dialog.multiChoiceItemsCurrentStates!!.toList().mapIndexedNotNull { index, b ->
                        if (dialog.multiChoiceItemsInitialStates!![index] != b) {
                            index to b
                        }
                        else null
                    }

                if (diffs.isNotEmpty()) {
                    val prefs = SafeSharedPreferences.create<UserTagsKey>(activity)

                    val bookmarksFragment = activity.bookmarksFragment!!
                    val userTagsContainer = bookmarksFragment.userTagsContainer
                    val user = userTagsContainer.addUser(bookmark.user)
                    val tags = userTagsContainer.tags
                    val tagNames = tags.map { it.name }

                    diffs.forEach {
                        val name = tagNames[it.first]
                        val tag = userTagsContainer.getTag(name)!!
                        if (it.second) {
                            userTagsContainer.tagUser(user, tag)
                        }
                        else {
                            userTagsContainer.unTagUser(user, tag)
                        }
                    }

                    prefs.edit {
                        putObject(UserTagsKey.CONTAINER, userTagsContainer)
                    }

                    activity.showToast(R.string.msg_user_tagged, user.name, user.tags.size)
                    listener?.onTagUser(bookmark)
                }
            }

            fun onCreateNewTag(listener: Listener?, dialog: AlertDialogFragment) {
                val bookmark = dialog.getAdditionalData<Bookmark>("bookmark")!!

                UserTagDialogFragment.Builder(R.style.AlertDialogStyle)
                    .setAdditionalData("bookmark", bookmark)
                    .show(listener!!.fragmentManagerForDialog, "create_tag_dialog")
            }

            fun onCompleteCreateTag(tagName: String, activity: BookmarksActivity, dialog: UserTagDialogFragment) : Boolean {
                val prefs = SafeSharedPreferences.create<UserTagsKey>(activity)

                val bookmarksFragment = activity.bookmarksFragment!!
                val userTagsContainer = bookmarksFragment.userTagsContainer

                val bookmark = dialog.getAdditionalData<Bookmark>("bookmark")!!
                val user = bookmarksFragment.userTagsContainer.addUser(bookmark.user)

                if (userTagsContainer.containsTag(tagName)) {
                    activity.showToast(R.string.msg_user_tag_existed)
                    return false
                }
                else {
                    try {
                        val tag = userTagsContainer.addTag(tagName)
                        userTagsContainer.tagUser(user, tag)

                        prefs.edit {
                            putObject(UserTagsKey.CONTAINER, userTagsContainer)
                        }

                        val parentTabAdapter = bookmarksFragment.bookmarksTabAdapter
                        parentTabAdapter?.notifyItemChanged(bookmark)
                        activity.showToast(
                            R.string.msg_user_tag_created_and_added_user,
                            tagName,
                            bookmark.user)
                    }
                    catch (e: Exception) {
                        Log.e("onCompleteEditTagName", "failed to save")
                        activity.showToast("ユーザータグの作成に失敗しました")
                    }
                    return true
                }
            }
        }
    }
}
