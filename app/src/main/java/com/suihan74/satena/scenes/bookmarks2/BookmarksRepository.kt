package com.suihan74.satena.scenes.bookmarks2

import androidx.lifecycle.LiveData
import com.suihan74.HatenaLib.*
import kotlinx.coroutines.Deferred

/** ブクマに付けられたスター情報取得を監視するライブデータ */
class StarsEntryLiveData(
    private val client: HatenaClient,
    private val entry: Entry,
    private val bookmark: Bookmark,
    private val repository: BookmarksRepository,
    private val starsEntry: StarsEntry? = null
) : LiveData<StarsEntry>() {
    private var task: Deferred<StarsEntry>? = null

    override fun onActive() {
        if (starsEntry == null) {
            updateAsync().start()
        }
        else {
            postValue(starsEntry)
        }
    }

    override fun onInactive() {
        task?.cancel()
    }

    /** スター情報を強制再読み込み */
    fun updateAsync() : Deferred<StarsEntry> {
        this.task?.cancel()
        val task = client.getStarsEntryAsync(bookmark.getBookmarkUrl(entry)).apply {
            invokeOnCompletion { e ->
                if (e == null) {
                    val result = getCompleted()
                    repository.setStarsEntryTo(bookmark.user, result)
                    postValue(result)
                }
            }
        }
        this.task = task
        return task
    }
}

class BookmarksRepository(
    val entry: Entry,
    private val client: HatenaClient
) {
    var bookmarksEntry: BookmarksEntry? = null
        private set

    var bookmarksDigest: BookmarksDigest? = null
        private set

    var bookmarksPopular: List<Bookmark> = emptyList()
        private set

    var bookmarksRecent: List<Bookmark> = emptyList()
        private set

    var ignoredUsers: List<String> = emptyList()

    /** ユーザーに付けられたスターの情報 -> key = 対象ユーザー名, value = スター情報 */
    private val starsMap = HashMap<String, StarsEntry>()

    /** ユーザーに付けられたスター情報を取得する */
    fun getStarsEntryTo(user: String) = synchronized(starsMap) { starsMap[user] }
    /** ユーザーに付けられたスター情報を登録or更新する */
    fun setStarsEntryTo(user: String, entry: StarsEntry) = synchronized(starsMap) { starsMap[user] = entry }

    /** ユーザーが付けたスター情報を取得する */
    fun getStarsEntryFrom(user: String) = synchronized(starsMap) {
        starsMap.mapNotNull m@ {
            if (it.value.allStars.any { star -> star.user == user }) it.value
            else null
        }
    }

    /** ブックマークエントリを取得 */
    fun loadBookmarksEntryAsnyc() =
        client.getBookmarksEntryAsync(entry.url).apply {
            invokeOnCompletion { e ->
                if (e != null) return@invokeOnCompletion

                val result = getCompleted()
                bookmarksEntry = result
                if (bookmarksEntry == null) return@invokeOnCompletion

                // スター情報をロード開始
                val commentUrls = result.bookmarks
                    .filter { it.comment.isNotBlank() }
                    .map { Pair(it.user, it.getBookmarkUrl(entry)) }

                client.getStarsEntryAsync(commentUrls.map { it.second }).run {
                    invokeOnCompletion { e ->
                        if (e == null) {
                            getCompleted().forEach { response ->
                                val user =
                                    commentUrls.firstOrNull { it.second == response.url }?.first
                                        ?: return@forEach
                                setStarsEntryTo(user, response)
                            }
                        }
                    }
                }
            }
        }

    /** 人気ブクマなどの概要情報を取得 */
    fun loadBookmarksDigestAsync() =
        client.getDigestBookmarksAsync(entry.url).apply {
            invokeOnCompletion { e ->
                if (e == null) {
                    val result = getCompleted()
                    bookmarksDigest = result
                    bookmarksPopular = result.scoredBookmarks.map { Bookmark.createFrom(it) }
                }
            }
        }

    /** 新着ブクマを取得 */
    fun loadBookmarksRecentAsync(offset: Long? = null) =
        client.getRecentBookmarksAsync(entry.url, of = offset).apply {
            invokeOnCompletion { e ->
                if (e != null) return@invokeOnCompletion

                val page = getCompleted().map { Bookmark.createFrom(it) }
                bookmarksRecent =
                    page.plus(
                        bookmarksRecent.filterNot { page.any { updated -> it.user == updated.user } }
                    )
                        .sortedByDescending { it.timestamp }

                val commentUrls = page
                    .filter { it.comment.isNotBlank() && starsMap.containsKey(it.user) }
                    .map { Pair(it.user, it.getBookmarkUrl(entry)) }

                client.getStarsEntryAsync(commentUrls.map { it.second }).run {
                    invokeOnCompletion { e ->
                        if (e == null) {
                            getCompleted().forEach { response ->
                                val user =
                                    commentUrls.firstOrNull { it.second == response.url }?.first
                                        ?: return@forEach
                                setStarsEntryTo(user, response)
                            }
                        }
                    }
                }
            }
        }

    /** 新着ブクマリストの次のページをロードする */
    fun loadNextBookmarksRecentAsync() =
        loadBookmarksRecentAsync(bookmarksRecent.size.toLong())

    /** 非表示ユーザーのリストをロードする */
    fun loadIgnoredUsersAsync() =
        client.getIgnoredUsersAsync().apply {
            invokeOnCompletion { e ->
                if (e == null) {
                    ignoredUsers = getCompleted()
                }
            }
        }

    /** ブクマに付けられたスター取得を監視するライブデータを生成する */
    fun createStarsEntryLiveData(bookmark: Bookmark) =
        StarsEntryLiveData(
            client,
            entry,
            bookmark,
            this,
            getStarsEntryTo(bookmark.user))
}
