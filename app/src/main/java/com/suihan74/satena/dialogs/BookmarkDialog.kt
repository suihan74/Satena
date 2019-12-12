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
import com.suihan74.satena.SatenaApplication
import com.suihan74.satena.models.userTag.findRelation
import com.suihan74.satena.models.userTag.insertRelation
import com.suihan74.satena.models.userTag.insertTag
import com.suihan74.satena.models.userTag.makeUser
import com.suihan74.satena.scenes.bookmarks.BookmarksActivity
import com.suihan74.satena.scenes.entries.EntriesActivity
import com.suihan74.utilities.BookmarkCommentDecorator
import com.suihan74.utilities.showToast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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
        val tags = bookmarksFragment.tags!!
        val tagNames = tags.map { it.userTag.name }.toTypedArray()
        val states = tags.map { it.users.any { u -> u.name == bookmark.user } }.toBooleanArray()

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
                    val bookmarksFragment = activity.bookmarksFragment!!
                    bookmarksFragment.launch(Dispatchers.IO) {
                        val userName = bookmark.user
                        val dao = SatenaApplication.instance.getUserTagDao()
                        val user = dao.makeUser(userName)
                        val tags = bookmarksFragment.tags!!.map { it.userTag }

                        diffs.forEach {
                            val tag = tags[it.first]
                            if (it.second) {
                                dao.insertRelation(tag, user)
                            }
                            else {
                                dao.findRelation(tag, user)?.let { relation ->
                                    dao.deleteRelation(relation)
                                }
                            }
                        }

                        bookmarksFragment.taggedUsers = dao.getAllUsers().mapNotNull {
                            dao.getUserAndTags(it.name)
                        }
                        bookmarksFragment.tags = dao.getAllTags().mapNotNull {
                            dao.getTagAndUsers(it.name)
                        }

                        val userAndTags = dao.getUserAndTags(userName)

                        withContext(Dispatchers.Main) {
                            activity.showToast(
                                R.string.msg_user_tagged,
                                user.name,
                                userAndTags?.tags?.size ?: 0
                            )
                            listener?.onTagUser(bookmark)
                        }
                    }
                }
            }

            fun onCreateNewTag(listener: Listener?, dialog: AlertDialogFragment) {
                val bookmark = dialog.getAdditionalData<Bookmark>("bookmark")!!

                UserTagDialogFragment.Builder(R.style.AlertDialogStyle)
                    .setAdditionalData("bookmark", bookmark)
                    .show(listener!!.fragmentManagerForDialog, "create_tag_dialog")
            }

            suspend fun onCompleteCreateTag(
                tagName: String,
                activity: BookmarksActivity,
                dialog: UserTagDialogFragment
            ) : Boolean = withContext(Dispatchers.IO) {

                val dao = SatenaApplication.instance.getUserTagDao()

                if (dao.findTag(tagName) != null) {
                    withContext(Dispatchers.Main) {
                        activity.showToast(R.string.msg_user_tag_existed)
                    }
                    return@withContext false
                }
                else {
                    try {
                        val bookmark = dialog.getAdditionalData<Bookmark>("bookmark")!!
                        val userName = bookmark.user

                        dao.insertTag(tagName)
                        val tag = dao.findTag(tagName)!!
                        val user = dao.makeUser(userName)
                        dao.insertRelation(tag, user)

                        val bookmarksFragment = activity.bookmarksFragment!!
                        bookmarksFragment.taggedUsers = dao.getAllUsers().mapNotNull {
                            dao.getUserAndTags(it.name)
                        }
                        bookmarksFragment.tags = dao.getAllTags().mapNotNull {
                            dao.getTagAndUsers(it.name)
                        }

                        withContext(Dispatchers.Main) {
                            activity.showToast(
                                R.string.msg_user_tag_created_and_added_user,
                                tagName,
                                userName
                            )
                            val listener = dialog.parentFragment as? Listener ?: dialog.activity as? Listener
                            listener?.onTagUser(bookmark)
                        }
                    }
                    catch (e: Exception) {
                        Log.e("onCompleteEditTagName", "failed to save")
                        withContext(Dispatchers.Main) {
                            activity.showToast("ユーザータグの作成に失敗しました")
                        }
                    }
                    return@withContext true
                }
            }
        }
    }
}
