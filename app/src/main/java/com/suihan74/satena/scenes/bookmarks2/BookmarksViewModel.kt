package com.suihan74.satena.scenes.bookmarks2

import android.content.Intent
import android.util.Log
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.suihan74.hatenaLib.*
import com.suihan74.satena.R
import com.suihan74.satena.models.ignoredEntry.IgnoreTarget
import com.suihan74.satena.models.ignoredEntry.IgnoredEntryType
import com.suihan74.satena.models.userTag.Tag
import com.suihan74.satena.models.userTag.TagAndUsers
import com.suihan74.satena.models.userTag.UserAndTags
import com.suihan74.satena.scenes.bookmarks2.dialog.BookmarkMenuDialog
import com.suihan74.satena.scenes.bookmarks2.dialog.PostStarDialog
import com.suihan74.satena.scenes.bookmarks2.dialog.ReportDialog
import com.suihan74.satena.scenes.bookmarks2.dialog.UserTagSelectionDialog
import com.suihan74.satena.scenes.entries2.EntriesActivity
import com.suihan74.satena.scenes.preferences.ignored.IgnoredEntryRepository
import com.suihan74.satena.scenes.preferences.userTag.UserTagRepository
import com.suihan74.utilities.*
import kotlinx.coroutines.*

class BookmarksViewModel(
    val repository: BookmarksRepository,
    private val userTagRepository: UserTagRepository,
    private val ignoredEntryRepository: IgnoredEntryRepository
) : ViewModel(),
        BookmarkMenuDialog.Listener,
        ReportDialog.Listener,
        UserTagSelectionDialog.Listener,
        PostStarDialog.Listener
{
    private val DIALOG_REPORT by lazy { "DIALOG_REPORT" }

    private val DIALOG_SELECT_USER_TAG by lazy { "DIALOG_SELECT_USER_TAG" }

    private val DIALOG_POST_STAR by lazy { "DIALOG_POST_STAR" }

    var fragmentManager : FragmentManager? = null
        private set

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
            ?: userBookmark?.commentRaw
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
        onSuccess: OnSuccess<Entry>? = null,
        onError: OnError? = null
    ) = viewModelScope.launch {
        try {
            loadAction.invoke()
            init(fragmentManager, true, onError)
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
        onSuccess: OnSuccess<Entry>? = null,
        onError: OnError? = null
    ) = loadEntryImpl(
        { repository.loadEntry(url) },
        onSuccess,
        onError
    )

    /** EntryIdを使ってEntryをロードする */
    fun loadEntry(
        eid: Long,
        onSuccess: OnSuccess<Entry>? = null,
        onError: OnError? = null
    ) = loadEntryImpl(
        { repository.loadEntry(eid) },
        onSuccess,
        onError
    )

    fun loadEntry(
        entry: Entry,
        onSuccess: OnSuccess<Entry>? = null,
        onError: OnError? = null
    ) = loadEntryImpl(
        { repository.loadEntry(entry) },
        onSuccess,
        onError
    )

    /**
     * ユーザーのブクマ情報を更新する
     */
    fun updateUserBookmark(bookmarkResult: BookmarkResult) = viewModelScope.launch(Dispatchers.Default) {
        repository.updateUserBookmark(bookmarkResult)
        bookmarksEntry.postValue(repository.bookmarksEntry)
        bookmarksPopular.postValue(repository.bookmarksPopular)
        bookmarksRecent.postValue(repository.bookmarksRecent)
    }

    /** 初期化 */
    fun init(
        fragmentManager: FragmentManager?,
        loading: Boolean,
        onError: OnError? = null,
        onFinally: OnFinally? = null
    ) = viewModelScope.launch {
        this@BookmarksViewModel.fragmentManager = fragmentManager

        try {
            ignoredEntryRepository.load(forceUpdate = true)
            muteWords = ignoredEntryRepository.ignoredEntries
                .filter { it.type == IgnoredEntryType.TEXT && it.target.contains(IgnoreTarget.BOOKMARK) }
                .map { it.query }

            signIn()
        }
        catch (e: Throwable) {
            withContext(Dispatchers.Main) {
                onError?.invoke(e)
            }
        }

        if (loading) {
            try {
                loadUserTags()
            }
            catch (e: Throwable) {
                withContext(Dispatchers.Main) {
                    onError?.invoke(e)
                }
            }

            try {
                repository.loadIgnoredUsersAsync().await()
            }
            catch (e: Throwable) {
                withContext(Dispatchers.Main) {
                    onError?.invoke(e)
                }
            }

            try {
                listOf(
                    repository.loadBookmarksEntryAsync(),
                    repository.loadBookmarksDigestAsync(),
                    repository.loadBookmarksRecentAsync()
                ).run {
                    awaitAll()
                    reloadLists()
                }
            }
            catch (e: Throwable) {
                withContext(Dispatchers.Main) {
                    onError?.invoke(e)
                }
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

        // 所持しているスター情報を取得する
        try {
            repository.userStarsLiveData.load()
        }
        catch (e: Throwable) {
            withContext(Dispatchers.Main) {
                onError?.invoke(e)
            }
        }

        withContext(Dispatchers.Main) {
            onFinally?.invoke()
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
        try {
            repository.init()
            repository.loadIgnoredUsersAsync().await()
        }
        catch (e: Throwable) {
            onError?.invoke(e)
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
    suspend fun loadNextRecent(onError: CompletionHandler? = null) : List<Bookmark> {
        loadBasics(onError)
        val recent = repository.loadNextBookmarksRecentAsync().await()
        bookmarksRecent.postValue(repository.bookmarksRecent)
        bookmarksEntry.postValue(repository.bookmarksEntry)

        return recent
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

    /** ブコメにスターをつける */
    fun postStar(
        bookmark: Bookmark,
        color: StarColor,
        quote: String = ""
    ) = viewModelScope.launch {
        repository.postStar(bookmark, color, quote)
        repository.allStarsLiveData.update(bookmark)
        repository.userStarsLiveData.load()
    }

    /** ブコメにスターをつける(必要なら送信前にダイアログを表示する) */
    fun postStarDialog(
        bookmark: Bookmark,
        color: StarColor,
        quote: String = ""
    ) {
        if (repository.usePostStarDialog) {
            val fragmentManager = fragmentManager ?: return
            val dialog = PostStarDialog.createInstance(bookmark, color, quote)
            dialog.show(fragmentManager, DIALOG_POST_STAR)
        }
        else {
            postStar(bookmark, color, quote)
        }
    }

    /** ブコメのスターを削除する */
    fun deleteStar(
        bookmark: Bookmark,
        star: Star
    ) = viewModelScope.launch {
        repository.deleteStar(bookmark, star)
        repository.allStarsLiveData.update(bookmark)
        repository.userStarsLiveData.load()
    }

    /** ブコメのスターを削除するダイアログを表示する */
    fun deleteStarDialog(
        bookmark: Bookmark,
        star: Star
    ) {
        val fragmentManager = fragmentManager ?: return
        val dialog = PostStarDialog.createInstanceDeleteMode(bookmark, star)
        dialog.show(fragmentManager, DIALOG_POST_STAR)
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

    /** ユーザーのブクマを取得 */
    val userBookmark : Bookmark? get() = repository.userBookmark

    // --- BookmarkMenuDialogの処理 --- //

    override fun isIgnored(dialog: BookmarkMenuDialog, user: String) =
        ignoredUsers.value.contains(user)

    override fun onShowEntries(dialog: BookmarkMenuDialog, user: String) {
        val activity = dialog.requireActivity()
        val intent = Intent(activity, EntriesActivity::class.java).apply {
            putExtra(EntriesActivity.EXTRA_USER, user)
        }
        activity.startActivity(intent)
    }

    override fun onIgnoreUser(dialog: BookmarkMenuDialog, user: String, ignore: Boolean) {
        val activity = dialog.requireActivity()
        setUserIgnoreState(
            user,
            ignore,
            onSuccess = {
                val msgId =
                    if (ignore) R.string.msg_ignore_user_succeeded
                    else R.string.msg_unignore_user_succeeded
                activity.showToast(msgId, user)
            },
            onError = { e ->
                val msgId =
                    if (ignore) R.string.msg_ignore_user_failed
                    else R.string.msg_unignore_user_failed
                activity.showToast(msgId, user)
                Log.e("ignoreUser", "failed: user = $user")
                e.printStackTrace()
            }
        )
    }

    override fun onReportBookmark(dialog: BookmarkMenuDialog, bookmark: Bookmark) {
        val activity = dialog.requireActivity()
        ReportDialog.createInstance(entry, bookmark)
            .showAllowingStateLoss(activity.supportFragmentManager, DIALOG_REPORT)
    }

    override fun onSetUserTag(dialog: BookmarkMenuDialog, user: String) {
        val activity = dialog.requireActivity()
        UserTagSelectionDialog.createInstance(user)
            .showAllowingStateLoss(activity.supportFragmentManager, DIALOG_SELECT_USER_TAG)
    }

    override fun onDeleteStar(dialog: BookmarkMenuDialog, bookmark: Bookmark, star: Star) {
        deleteStarDialog(bookmark, star)
    }

    // --- ReportDialogの処理 --- //

    override fun onReportBookmark(dialog: ReportDialog, model: ReportDialog.Model) {
        val activity = dialog.requireActivity()
        val user = model.report.bookmark.user
        reportBookmark(
            model.report,
            onSuccess = {
                if (model.ignoreAfterReporting && !ignoredUsers.value.contains(user)) {
                    setUserIgnoreState(
                        user,
                        true,
                        onSuccess = {
                            activity.showToast(R.string.msg_report_and_ignore_succeeded, user)
                        },
                        onError = { e ->
                            activity.showToast(R.string.msg_ignore_user_failed, user)
                            Log.e("ignoreUser", "failed: user = $user")
                            e.printStackTrace()
                        }
                    )
                }
                else {
                    activity.showToast(R.string.msg_report_succeeded, user)
                }
            },
            onError = { e ->
                activity.showToast(R.string.msg_report_failed)
                Log.e("onReportBookmark", "error user: $user")
                e.printStackTrace()
            }
        )
    }

    // --- UserTagSelectionDialogの処理 --- //

    override fun getUserTags() =
        userTags.value ?: emptyList()

    override suspend fun activateTags(user: String, activeTags: List<Tag>) {
        activeTags.forEach { tag ->
            tagUser(user, tag)
        }
    }

    override suspend fun inactivateTags(user: String, inactiveTags: List<Tag>) {
        inactiveTags.forEach { tag ->
            unTagUser(user, tag)
        }
    }

    override suspend fun reloadUserTags() {
        loadUserTags()
    }

    // --- PostStarDialogの処理 --- //

    override fun onPostStar(dialog: PostStarDialog, bookmark: Bookmark, starColor: StarColor, quote: String) {
        val activity = dialog.requireActivity()

        postStar(
            bookmark,
            starColor,
            quote
        ).invokeOnCompletion { e ->
            if (e == null) {
                activity.showToast(R.string.msg_post_star_succeeded, bookmark.user)
            }
            else {
                activity.showToast(R.string.msg_post_star_failed, bookmark.user)
            }
        }
    }

    override fun onDeleteStar(dialog: PostStarDialog, bookmark: Bookmark, star: Star) {
        val activity = dialog.requireActivity()

        deleteStar(
            bookmark,
            star,
        ).invokeOnCompletion { e ->
            if (e == null) {
                activity.showToast(R.string.msg_delete_star_succeeded)
            }
            else {
                activity.showToast(R.string.msg_delete_star_failed)
            }
        }
    }

    // ------- //

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
