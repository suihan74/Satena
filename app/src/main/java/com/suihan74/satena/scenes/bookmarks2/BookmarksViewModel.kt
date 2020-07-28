package com.suihan74.satena.scenes.bookmarks2

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.suihan74.hatenaLib.Bookmark
import com.suihan74.hatenaLib.BookmarksEntry
import com.suihan74.hatenaLib.Entry
import com.suihan74.hatenaLib.Report
import com.suihan74.satena.models.ignoredEntry.IgnoreTarget
import com.suihan74.satena.models.ignoredEntry.IgnoredEntryType
import com.suihan74.satena.models.userTag.Tag
import com.suihan74.satena.models.userTag.TagAndUsers
import com.suihan74.satena.models.userTag.UserAndTags
import com.suihan74.satena.scenes.preferences.ignored.IgnoredEntryRepository
import com.suihan74.satena.scenes.preferences.userTag.UserTagRepository
import com.suihan74.utilities.lock
import kotlinx.coroutines.*

class BookmarksViewModel(
    val repository: BookmarksRepository,
    private val userTagRepository: UserTagRepository,
    private val ignoredEntryRepository: IgnoredEntryRepository
) : ViewModel() {

    val entry
        get() = repository.entry

    val bookmarksEntry by lazy {
        MutableLiveData<BookmarksEntry>()
    }

    val bookmarksPopular by lazy {
        MutableLiveData<List<Bookmark>>()
    }

    /** 新着順ブクマリスト */
    val bookmarksRecent by lazy {
        MutableLiveData<List<Bookmark>>()
    }

    val taggedUsers by lazy {
        MutableLiveData<List<UserAndTags>>()
    }

    val userTags by lazy {
        MutableLiveData<List<TagAndUsers>>()
    }

    /** キーワードでブクマを抽出 */
    val filteringWord by lazy {
        MutableLiveData<String?>()
    }

    /** IgnoredEntryで設定された非表示ワード */
    var muteWords = emptyList<String>()
        get() = lock(field) { field }
        private set(value) {
            lock(field) { field = value }
        }

    /** 非表示ユーザーリストの変更を監視 */
    val ignoredUsers = repository.ignoredUsersLiveData

    /** サインイン完了を監視する */
    val signedIn by lazy {
        MutableLiveData<Boolean>()
    }

    /** 編集途中の投稿コメント */
    val editingComment : String
        get() =
            mEditingComment
            ?: entry.bookmarkedData?.commentRaw
            ?: ""
    private var mEditingComment : String? = null

    fun setEditingComment(comment: String?) {
        mEditingComment = comment
    }

    /** 各リストを再構成する */
    private fun reloadLists() {
        if (repository.bookmarksEntry != null) {
            bookmarksEntry.postValue(repository.bookmarksEntry)
        }
        bookmarksPopular.postValue(repository.bookmarksPopular)
        bookmarksRecent.postValue(repository.bookmarksRecent)
    }

    private fun loadEntryImpl(
        loadAction: suspend ()->Unit,
        onSuccess: ((Entry) -> Unit)? = null,
        onError: CompletionHandler? = null
    ) = viewModelScope.launch {
        try {
            loadAction.invoke()
            init(true, onError)
        }
        catch (e: Throwable) {
            withContext(Dispatchers.Main) {
                onError?.invoke(e)
            }
            return@launch
        }

        withContext(Dispatchers.Main) {
            onSuccess?.invoke(repository.entry)
        }
    }

    /** URLを使ってEntryをロードする */
    fun loadEntry(
        url: String,
        onSuccess: ((entry: Entry) -> Unit)? = null,
        onError: CompletionHandler? = null
    ) = loadEntryImpl(
        { repository.loadEntry(url) },
        onSuccess,
        onError
    )

    /** EntryIdを使ってEntryをロードする */
    fun loadEntry(
        eid: Long,
        onSuccess: ((entry: Entry) -> Unit)? = null,
        onError: CompletionHandler? = null
    ) = loadEntryImpl(
        { repository.loadEntry(eid) },
        onSuccess,
        onError
    )

    /**
     * entryを更新する
     * ブクマ投稿後に変更を反映するために使用
     */
    fun resetEntry(newEntry: Entry) {
        repository.setEntry(newEntry)
    }

    /** 初期化 */
    fun init(loading: Boolean, onError: CompletionHandler? = null) = viewModelScope.launch(
        CoroutineExceptionHandler { _, e -> onError?.invoke(e) }
    ) {
        try {
            ignoredEntryRepository.load(forceUpdate = true)
            muteWords = ignoredEntryRepository.ignoredEntries
                .filter { it.type == IgnoredEntryType.TEXT && it.target.contains(IgnoreTarget.BOOKMARK) }
                .map { it.query }

            signIn()
        }
        catch (e: Throwable) {
            onError?.invoke(e)
        }

        if (loading) {
            loadUserTags()

            try {
                repository.loadIgnoredUsersAsync().await()
            }
            catch (e: Throwable) {
                onError?.invoke(e)
            }

            listOf(
                repository.loadBookmarksEntryAsync(),
                repository.loadBookmarksDigestAsync(),
                repository.loadBookmarksRecentAsync()
            ).run {
                awaitAll()
                reloadLists()
            }
        }

        // キーワードが更新されたら各リストを再生成する
        filteringWord.observeForever {
            reloadLists()
        }

        // 非表示ユーザーリストの更新を監視
        ignoredUsers.observeForever {
            reloadLists()
        }
    }

    /** サインイン */
    suspend fun signIn() {
        repository.init()
        signedIn.postValue(repository.signedIn)
    }

    /**
     * エントリ情報・非表示ユーザー情報・ログインを完了した状態にする
     * 通信OFF状態でinit()し，通信ONに変更した後でブクマロードをする場合のため
     */
    private suspend fun loadBasics(onError: CompletionHandler? = null) {
        if (!repository.signedIn) {
            try {
                repository.init()
                repository.loadIgnoredUsersAsync().await()
            }
            catch (e: Throwable) {
                onError?.invoke(e)
            }
        }

        if (bookmarksEntry.value == null) {
            try {
                repository.loadBookmarksEntryAsync().await()
            }
            catch (e: Throwable) {
                onError?.invoke(e)
            }
        }
    }

    /** ブクマにキーワードが含まれるか確認 */
    private fun Bookmark.containsKeyword(word: String) =
        user.contains(word) || comment.contains(word) || getTagsText(",").contains(word)

    /** 非表示ユーザー/ワード情報を適用したブクマリストを返す */
    fun filter(bookmarks: List<Bookmark>) =
        keywordFilter(bookmarks).filterNot { b ->
            repository.ignoredUsers.any { it == b.user }
                    || muteWords.any { b.containsKeyword(it) }
        }

    /** ブクマリストからキーワードで抽出 */
    fun keywordFilter(list: List<Bookmark>) : List<Bookmark> {
        val keyword = filteringWord.value
        return if (keyword.isNullOrBlank()) list
        else list.filter { it.containsKeyword(keyword) }
    }

    /** 新着ブクマリストの次のページを追加ロードする */
    fun loadNextRecent(onError: CompletionHandler? = null) = viewModelScope.launch(
        CoroutineExceptionHandler { _, e ->
            onError?.invoke(e)
        }
    ) {
        loadBasics(onError)
        repository.loadNextBookmarksRecentAsync().await()
        bookmarksRecent.postValue(repository.bookmarksRecent)
        bookmarksEntry.postValue(repository.bookmarksEntry)
    }

    /** 指定ユーザーのブクマが得られるまで新着順ブクマリストを追加ロードする */
    fun loadNextRecentToUser(user: String, onError: CompletionHandler? = null) = viewModelScope.launch(
        CoroutineExceptionHandler { _, e ->
            onError?.invoke(e)
        }
    ) {
        if (repository.bookmarksRecent.any { it.user == user }) {
            return@launch
        }

        while (true) {
            val list = repository.loadNextBookmarksRecentAsync().await()
            if (list.isEmpty() || list.any { it.user == user }) break
        }

        bookmarksRecent.postValue(repository.bookmarksRecent)
        bookmarksEntry.postValue(repository.bookmarksEntry)
    }

    /** 人気ブクマリストを再読み込み */
    fun updateDigest(onError: CompletionHandler? = null) = viewModelScope.launch(
        CoroutineExceptionHandler { _, e ->
            onError?.invoke(e)
        }
    ) {
        loadBasics()
        repository.loadBookmarksDigestAsync().await()
        bookmarksPopular.postValue(repository.bookmarksPopular)
    }

    /** 新着ブクマリストを再読み込み */
    fun updateRecent(onError: CompletionHandler? = null) = viewModelScope.launch(
        CoroutineExceptionHandler { _, e ->
            onError?.invoke(e)
        }
    ) {
        loadBasics()
        repository.loadBookmarksRecentAsync().await()
        bookmarksRecent.postValue(repository.bookmarksRecent)
        bookmarksEntry.postValue(repository.bookmarksEntry)
    }

    /** ユーザーの非表示状態を変更する */
    fun setUserIgnoreState(
        user: String,
        ignore: Boolean,
        onSuccess: (()->Unit)? = null,
        onError: ((Throwable)->Unit)? = null
    ) = viewModelScope.launch {
        try {
            if (ignore) {
                repository.ignoreUserAsync(user).await()
            }
            else {
                repository.unIgnoreUserAsync(user).await()
            }
        }
        catch (e: Throwable) {
            withContext(Dispatchers.Main) {
                onError?.invoke(e)
            }
            return@launch
        }

        withContext(Dispatchers.Main) {
            onSuccess?.invoke()
        }
    }

    /** 新しいタグを作成する */
    suspend fun createTag(tagName: String) {
        userTagRepository.addTag(tagName)
    }

    /** ユーザーにタグをつける */
    suspend fun tagUser(user: String, tag: Tag) {
        userTagRepository.addRelation(tag, user)
    }

    /** ユーザーのタグを外す */
    suspend fun unTagUser(user: String, tag: Tag) {
        userTagRepository.getUser(user)?.let {
            userTagRepository.deleteRelation(tag, it)
        }
    }

    /** ユーザータグをロードする */
    suspend fun loadUserTags() {
        taggedUsers.value = (userTagRepository.loadUsers())
        userTags.value = (userTagRepository.loadTags())
    }

    /** ブクマを通報 */
    fun reportBookmark(
        report: Report,
        onSuccess: (()->Unit)? = null,
        onError: ((Throwable)->Unit)? = null
    ) = viewModelScope.launch {
        try {
            repository.reportBookmark(report)
        }
        catch (e: Throwable) {
            withContext(Dispatchers.Main) {
                onError?.invoke(e)
            }
            return@launch
        }

        withContext(Dispatchers.Main) {
            onSuccess?.invoke()
        }
    }

    /** ViewModelProvidersを使用する際の依存性注入 */
    class Factory(
        private val repository: BookmarksRepository,
        private val userTagRepository: UserTagRepository,
        private val ignoredEntryRepository: IgnoredEntryRepository
    ) : ViewModelProvider.NewInstanceFactory() {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>) =
            BookmarksViewModel(repository, userTagRepository, ignoredEntryRepository) as T
    }
}
