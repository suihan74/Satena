package com.suihan74.satena.fragments

import android.app.AlertDialog
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import com.suihan74.HatenaLib.Bookmark
import com.suihan74.satena.R
import com.suihan74.satena.adapters.BookmarksAdapter
import com.suihan74.satena.models.BookmarksTabType
import com.suihan74.satena.models.PreferenceKey
import com.suihan74.utilities.SafeSharedPreferences

class CustomBookmarksTabFragment : BookmarksTabFragment() {
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
                putInt(ARGS_KEY_TAB_TYPE, BookmarksTabType.CUSTOM.int)
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
        val tagsContainer = fragment.userTagsContainer
        return mActiveTagIds
            .map { id ->
                tagsContainer.tags.firstOrNull { it.id == id }
            }
            .flatMap { tag ->
                tagsContainer.getUsersOfTag(tag)
            }
            .map { user ->
                user.name
            }
    }

    override fun getBookmarks(fragment: BookmarksFragment) : List<Bookmark> {
        val tagsContainer = fragment.userTagsContainer
        val ignoredUsers = fragment.ignoredUsers
        val activeUsers = getActiveTaggedUsers(fragment)

        return fragment.recentBookmarks.filter { bookmark ->
            if (!mIsNoCommentUsersActive && bookmark.comment.isBlank()) return@filter false
            if (!mIsMutedUsersActive && ignoredUsers.contains(bookmark.user)) return@filter false

            return@filter activeUsers.contains(bookmark.user) ||
                    mIsUnaffiliatedUsersActive && !tagsContainer.containsUser(bookmark.user)
        }
    }

    override fun isBookmarkShown(bookmark: Bookmark, fragment: BookmarksFragment) : Boolean {
        val tagsContainer = fragment.userTagsContainer
        val ignoredUsers = fragment.ignoredUsers
        val activeUsers = getActiveTaggedUsers(fragment)

        if (!mIsNoCommentUsersActive && bookmark.comment.isBlank()) return false
        if (!mIsMutedUsersActive && ignoredUsers.contains(bookmark.user)) return false
        return activeUsers.contains(bookmark.user) ||
                mIsUnaffiliatedUsersActive && !tagsContainer.containsUser(bookmark.user)
    }

    override fun hideIgnoredBookmark(adapter: BookmarksAdapter, bookmark: Bookmark) {
        if (!mIsMutedUsersActive) {
            adapter.removeItem(bookmark)
        }
    }

    fun openSettingsDialog(fragment: BookmarksFragment) {
        var isUnaffiliatedUsersActive = mIsUnaffiliatedUsersActive
        var isNoCommentUsersActive = mIsNoCommentUsersActive
        var isMutedUsersActive = mIsMutedUsersActive

        val notTagItems = listOf<Triple<String, Boolean, (Boolean)->Unit>>(
            Triple("無言ブクマを表示", isNoCommentUsersActive, { it -> isNoCommentUsersActive = it }),
            Triple("非表示ユーザーを表示", isMutedUsersActive, { it -> isMutedUsersActive = it }),
            Triple("タグ無しユーザーを表示", isUnaffiliatedUsersActive, { it -> isUnaffiliatedUsersActive = it })
        )

        val activeTagIds = ArrayList(mActiveTagIds)
        val tagsContainer = fragment.userTagsContainer
        val tags = tagsContainer.tags.map { tag ->
            Triple("タグ:${tag.name}", activeTagIds.contains(tag.id), { it: Boolean ->
                if (it) {
                    activeTagIds.add(tag.id)
                }
                else {
                    activeTagIds.remove(tag.id)
                }
            })
        }

        val items = notTagItems.plus(tags)

        AlertDialog.Builder(context, R.style.AlertDialogStyle)
            .setTitle("カスタムブクマリストの設定")
            .setMultiChoiceItems(
                items.map { it.first }.toTypedArray(),
                items.map { it.second }.toBooleanArray()
            ) { _, which, checked ->
                items[which].third(checked)
            }
            .setNegativeButton("Cancel", null)
            .setPositiveButton("登録") { _, _ ->
                mIsUnaffiliatedUsersActive = isUnaffiliatedUsersActive
                mIsNoCommentUsersActive = isNoCommentUsersActive
                mIsMutedUsersActive = isMutedUsersActive
                mActiveTagIds = activeTagIds

                SafeSharedPreferences.create<PreferenceKey>(context).edit {
                    put(PreferenceKey.CUSTOM_BOOKMARKS_ACTIVE_TAG_IDS, mActiveTagIds)
                    put(PreferenceKey.CUSTOM_BOOKMARKS_IS_UNAFFILIATED_USERS_ACTIVE, mIsUnaffiliatedUsersActive)
                    put(PreferenceKey.CUSTOM_BOOKMARKS_IS_NO_COMMENT_USERS_ACTIVE, mIsNoCommentUsersActive)
                    put(PreferenceKey.CUSTOM_BOOKMARKS_IS_MUTED_USERS_ACTIVE, mIsMutedUsersActive)
                }

                refreshBookmarksAdapter()
            }
            .show()
    }
}
