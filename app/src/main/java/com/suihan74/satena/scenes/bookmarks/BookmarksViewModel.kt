package com.suihan74.satena.scenes.bookmarks

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.suihan74.HatenaLib.*
import com.suihan74.satena.models.ignoredEntry.IgnoreTarget
import com.suihan74.satena.models.ignoredEntry.IgnoredEntryType
import com.suihan74.satena.models.userTag.TagAndUsers
import com.suihan74.satena.models.userTag.UserAndTags
import com.suihan74.satena.scenes.preferences.ignored.IgnoredEntryRepository
import com.suihan74.satena.scenes.preferences.userTag.UserTagRepository
import java.util.HashMap

class BookmarksViewModel(
    private val ignoredEntryRepository: IgnoredEntryRepository,
    private val userTagRepository: UserTagRepository
) : ViewModel() {

    val ignoredWords by lazy { MutableLiveData<List<String>>() }

    val taggedUsers by lazy { MutableLiveData<List<UserAndTags>>()}
    val userTags by lazy { MutableLiveData<List<TagAndUsers>>() }

    lateinit var entry: Entry
    var bookmarksEntry: BookmarksEntry? = null
    var bookmarksDigest: BookmarksDigest? = null
    var bookmarksRecent : List<Bookmark> = emptyList()
    var starsMap: HashMap<String, StarsEntry> = HashMap()
    var tabPosition: Int = -1
    var searchModeEnabled: Boolean = false

    suspend fun init() {
        ignoredWords.postValue(
            ignoredEntryRepository.load()
                .filter {
                    it.type == IgnoredEntryType.TEXT && it.target.contains(IgnoreTarget.BOOKMARK)
                }
                .map { it.query }
        )
        loadTags()
    }

    suspend fun loadTags() {
        taggedUsers.postValue(
            userTagRepository.loadUsers()
        )

        userTags.postValue(
            userTagRepository.loadTags()
        )
    }

    class Factory(
        private val ignoredEntryRepository: IgnoredEntryRepository,
        private val userTagRepository: UserTagRepository
    ) : ViewModelProvider.NewInstanceFactory() {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>) =
            BookmarksViewModel(ignoredEntryRepository, userTagRepository) as T
    }
}
