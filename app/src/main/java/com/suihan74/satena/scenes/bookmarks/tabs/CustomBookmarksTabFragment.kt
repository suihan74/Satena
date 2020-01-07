package com.suihan74.satena.scenes.bookmarks.tabs

import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import com.suihan74.HatenaLib.Bookmark
import com.suihan74.satena.R
import com.suihan74.satena.dialogs.AlertDialogFragment
import com.suihan74.satena.models.BookmarksTabType
import com.suihan74.satena.models.PreferenceKey
import com.suihan74.satena.scenes.bookmarks.BookmarksActivity
import com.suihan74.satena.scenes.bookmarks.BookmarksAdapter
import com.suihan74.satena.scenes.bookmarks.BookmarksFragment
import com.suihan74.satena.scenes.bookmarks.BookmarksTabFragment
import com.suihan74.utilities.SafeSharedPreferences

class CustomBookmarksTabFragment : BookmarksTabFragment(), AlertDialogFragment.Listener {
    /** ブコメを表示するタグのID */
    private var mActiveTagIds : List<Int> = emptyList()

    /** どのタグにも所属していないユーザーのブコメを表示するか否か */
    private var mIsUnaffiliatedUsersActive : Boolean = true

    /** 無言ブクマを表示するか否か */
    private var mIsNoCommentUsersActive : Boolean = false

    /** 非表示ユーザーを表示するか否か */
    private var mIsMutedUsersActive : Boolean = false


    companion object {
        fun createInstance() = CustomBookmarksTabFragment().apply {
            arguments = Bundle().apply {
                putInt(ARGS_KEY_TAB_TYPE, BookmarksTabType.CUSTOM.ordinal)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setHasOptionsMenu(true)

        SafeSharedPreferences.create<PreferenceKey>(context).let {
            mActiveTagIds = it.get(PreferenceKey.CUSTOM_BOOKMARKS_ACTIVE_TAG_IDS)
            mIsUnaffiliatedUsersActive = it.getBoolean(PreferenceKey.CUSTOM_BOOKMARKS_IS_UNAFFILIATED_USERS_ACTIVE)
            mIsNoCommentUsersActive = it.getBoolean(PreferenceKey.CUSTOM_BOOKMARKS_IS_NO_COMMENT_USERS_ACTIVE)
            mIsMutedUsersActive = it.getBoolean(PreferenceKey.CUSTOM_BOOKMARKS_IS_MUTED_USERS_ACTIVE)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.custom_bookmarks_tab, menu)
        menu.findItem(R.id.settings)?.setOnMenuItemClickListener {
            val fragment = bookmarksFragment ?: return@setOnMenuItemClickListener false
            openSettingsDialog(fragment)
            return@setOnMenuItemClickListener true
        }
    }

    private fun getActiveTaggedUsers(fragment: BookmarksFragment) : List<String> {
        val taggedUsers = fragment.taggedUsers
        return mActiveTagIds
            .map { id ->
                taggedUsers.filter { it.tags.any { tag -> tag.id == id } }
            }
            .flatMap { it -> it.map { it.user.name } }
            .distinct()
    }

    override fun getBookmarks(fragment: BookmarksFragment) : List<Bookmark> {
        val ignoredUsers = fragment.ignoredUsers
        val activeUsers = getActiveTaggedUsers(fragment)

        return fragment.recentBookmarks.filter { bookmark ->
            if (!mIsNoCommentUsersActive && bookmark.comment.isBlank()) return@filter false
            if (!mIsMutedUsersActive && ignoredUsers.contains(bookmark.user)) return@filter false

            val user = bookmark.user
            val isUserTagged = fragment.taggedUsers?.any { it.user.name == user && it.tags.isNotEmpty() } ?: false

            return@filter activeUsers.contains(bookmark.user) ||
                    mIsUnaffiliatedUsersActive && !isUserTagged
        }
    }

    override fun isBookmarkShown(bookmark: Bookmark, fragment: BookmarksFragment) : Boolean {
        val ignoredUsers = fragment.ignoredUsers
        val activeUsers = getActiveTaggedUsers(fragment)

        if (!mIsNoCommentUsersActive && bookmark.comment.isBlank()) return false
        if (!mIsMutedUsersActive && ignoredUsers.contains(bookmark.user)) return false

        val user = bookmark.user
        val isUserTagged = fragment.taggedUsers?.any { it.user.name == user && it.tags.isNotEmpty() } ?: false

        return activeUsers.contains(bookmark.user) ||
                mIsUnaffiliatedUsersActive && !isUserTagged
    }

    override fun hideIgnoredBookmark(adapter: BookmarksAdapter, bookmark: Bookmark) {
        if (!mIsMutedUsersActive) {
            adapter.removeItem(bookmark)
        }
    }

    fun openSettingsDialog(fragment: BookmarksFragment) {
        val notTagItems = listOf(
            getString(R.string.custom_bookmarks_no_comment_active) to mIsNoCommentUsersActive,
            getString(R.string.custom_bookmarks_ignored_user_active) to mIsMutedUsersActive,
            getString(R.string.custom_bookmarks_no_user_tags_active) to mIsUnaffiliatedUsersActive)

        val activeTagIds = ArrayList(mActiveTagIds)
        val tags = fragment.tags?.map { tag -> "タグ:${tag.userTag.name}" to activeTagIds.contains(tag.userTag.id) } ?: emptyList()

        val items = notTagItems.plus(tags)

        AlertDialogFragment.Builder(R.style.AlertDialogStyle)
            .setTitle(R.string.custom_bookmarks_tab_pref_dialog_title)
            .setPositiveButton(R.string.dialog_register)
            .setNegativeButton(R.string.dialog_cancel)
            .setMultiChoiceItems(
                items.map { it.first },
                items.map { it.second }.toBooleanArray())
            .show(childFragmentManager, "custom_tab_pref_dialog")
    }

    override fun onClickPositiveButton(dialog: AlertDialogFragment) {
        val items = dialog.items ?: return
        val states = dialog.multiChoiceItemsCurrentStates ?: return
//        val tagsContainer = (activity as? BookmarksActivity)?.bookmarksFragment?.userTagsContainer ?: return
        val bookmarksFragment = (activity as? BookmarksActivity)?.bookmarksFragment ?: return

        val activeTagIds = ArrayList<Int>()
        states
            .toList()
            .forEachIndexed { index, state -> when(val text = items[index]) {
                getString(R.string.custom_bookmarks_no_comment_active) -> mIsNoCommentUsersActive = state
                getString(R.string.custom_bookmarks_ignored_user_active) -> mIsMutedUsersActive = state
                getString(R.string.custom_bookmarks_no_user_tags_active) -> mIsUnaffiliatedUsersActive = state

                else -> {
                    if (state) {
                        val item =
                            bookmarksFragment.tags?.firstOrNull { tag -> text == "タグ:${tag.userTag.name}" }
                                ?: return@forEachIndexed
                        activeTagIds.add(item.userTag.id)
                    }
                }
            }}
        mActiveTagIds = activeTagIds

        SafeSharedPreferences.create<PreferenceKey>(context).edit {
            put(PreferenceKey.CUSTOM_BOOKMARKS_ACTIVE_TAG_IDS, mActiveTagIds)
            put(PreferenceKey.CUSTOM_BOOKMARKS_IS_UNAFFILIATED_USERS_ACTIVE, mIsUnaffiliatedUsersActive)
            put(PreferenceKey.CUSTOM_BOOKMARKS_IS_NO_COMMENT_USERS_ACTIVE, mIsNoCommentUsersActive)
            put(PreferenceKey.CUSTOM_BOOKMARKS_IS_MUTED_USERS_ACTIVE, mIsMutedUsersActive)
        }

        refreshBookmarksAdapter()
    }
}
