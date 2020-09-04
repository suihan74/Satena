package com.suihan74.satena.scenes.bookmarks2.tab

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.suihan74.hatenaLib.Bookmark
import com.suihan74.satena.models.PreferenceKey
import com.suihan74.satena.models.userTag.TagAndUsers
import com.suihan74.satena.models.userTag.UserAndTags
import kotlinx.coroutines.launch

class CustomTabViewModel : BookmarksTabViewModel() {
    /** カスタムタブの表示内容設定 */
    private val settingsLiveData by lazy {
        MutableLiveData<Settings>()
    }

    val settings
        get() = settingsLiveData.value

    override fun init() {
        super.init()
        reloadSettings()

        activityViewModel.bookmarksRecent.observeForever {
            reloadBookmarks()
        }

        settingsLiveData.observeForever {
            reloadBookmarks()
        }

        activityViewModel.userTags.observeForever {
            reloadSettings()
        }
    }

    /** 設定を再ロードする */
    private fun reloadSettings() {
        val tags = activityViewModel.userTags.value ?: emptyList()
        val activeTagIds = preferences.get<List<Int>>(PreferenceKey.CUSTOM_BOOKMARKS_ACTIVE_TAG_IDS)
        val activeTags = activeTagIds.mapNotNull { id ->
            tags.firstOrNull { t -> t.userTag.id == id }
        }

        // 存在しないタグIDの参照を削除する
        fixActiveTagsError(activeTagIds)

        settingsLiveData.postValue(
            Settings(
                activeTags,
                activeUnaffiliatedUsers = preferences.getBoolean(PreferenceKey.CUSTOM_BOOKMARKS_IS_UNAFFILIATED_USERS_ACTIVE),
                activeNoCommentBookmarks = preferences.getBoolean(PreferenceKey.CUSTOM_BOOKMARKS_IS_NO_COMMENT_USERS_ACTIVE),
                activeMutedBookmarks = preferences.getBoolean(PreferenceKey.CUSTOM_BOOKMARKS_IS_MUTED_USERS_ACTIVE)
            )
        )
    }

    private fun fixActiveTagsError(activeTagIds: List<Int>) = viewModelScope.launch {
        val tags = activityViewModel.userTags.value ?: return@launch
        val fixedIds = activeTagIds.filter { id -> tags.any { t -> id == t.userTag.id } }
        if (fixedIds.size != activeTagIds.size) {
            preferences.edit {
                put(PreferenceKey.CUSTOM_BOOKMARKS_ACTIVE_TAG_IDS, fixedIds)
            }
        }
    }

    /** 現在の設定を永続化 */
    fun saveSettings(set: Settings) {
        settingsLiveData.value = set

        val activeTagIds = set.activeTags.map { it.userTag.id }
        preferences.edit {
            put(
                PreferenceKey.CUSTOM_BOOKMARKS_ACTIVE_TAG_IDS,
                activeTagIds
            )
            put(
                PreferenceKey.CUSTOM_BOOKMARKS_IS_UNAFFILIATED_USERS_ACTIVE,
                set.activeUnaffiliatedUsers
            )
            put(
                PreferenceKey.CUSTOM_BOOKMARKS_IS_NO_COMMENT_USERS_ACTIVE,
                set.activeNoCommentBookmarks
            )
            put(
                PreferenceKey.CUSTOM_BOOKMARKS_IS_MUTED_USERS_ACTIVE,
                set.activeMutedBookmarks
            )
        }
    }

    /** リストを再生成する */
    private fun reloadBookmarks() {
        val set = settingsLiveData.value ?: return
        val list = activityViewModel.keywordFilter(
            activityViewModel.bookmarksRecent.value
            ?: return
        )

        val users = activityViewModel.taggedUsers.value ?: emptyList()

        val muteWords = activityViewModel.muteWords
        val muteUsers = activityViewModel.ignoredUsers.value

        val filtered = list.filter {
            set.shown(it, users, muteWords, muteUsers)
        }

        bookmarks.postValue(
            activityViewModel.keywordFilter(filtered)
        )
    }

    override fun updateBookmarks() = activityViewModel.updateRecent()

    override suspend fun loadNextBookmarks() =
        try {
            activityViewModel.loadNextRecent()
        }
        catch (e: Throwable) {
            emptyList()
        }

    override fun updateSignedUserBookmark(user: String) =
        activityViewModel.bookmarksEntry.value?.bookmarks?.firstOrNull { it.user == user }


    /** カスタムタブの表示設定 */
    data class Settings (
        /** 表示対象のユーザータグ */
        val activeTags: List<TagAndUsers>,
        /** ユーザータグが付いていないユーザーを表示するか否か */
        val activeUnaffiliatedUsers: Boolean,
        /** コメントがないブクマを表示するか */
        val activeNoCommentBookmarks: Boolean,
        /** 非表示設定されるブクマ(非表示ユーザー/NGワード)を表示するか */
        val activeMutedBookmarks: Boolean
    ) {
        fun shown(bookmark: Bookmark, taggedUsers: List<UserAndTags>, muteWords: List<String>, muteUsers: List<String>) : Boolean {
            val tags = taggedUsers.firstOrNull { it.user.name == bookmark.user }?.tags ?: emptyList()
            return when {
                !activeNoCommentBookmarks && bookmark.comment.isBlank() ->
                    false

                !activeMutedBookmarks
                && (muteWords.any { w -> bookmark.comment.contains(w) } || muteUsers.any { u -> bookmark.user == u }) ->
                    false

                activeUnaffiliatedUsers && tags.isEmpty() ->
                    true

                activeTags.any { tag ->
                    tag.users.any { u -> u.name == bookmark.user }
                } -> true

                else -> false
            }
        }
    }
}
