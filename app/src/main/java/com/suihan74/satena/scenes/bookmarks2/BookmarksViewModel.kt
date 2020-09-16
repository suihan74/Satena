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
import com.suihan74.satena.SatenaApplication
import com.suihan74.satena.models.ignoredEntry.IgnoreTarget
import com.suihan74.satena.models.ignoredEntry.IgnoredEntryType
import com.suihan74.satena.models.userTag.Tag
import com.suihan74.satena.models.userTag.TagAndUsers
import com.suihan74.satena.models.userTag.UserAndTags
import com.suihan74.satena.scenes.bookmarks2.dialog.*
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
        ReportDialog.Listener,
        UserTagSelectionDialog.Listener
{
    val DIALOG_BOOKMARK_MENU by lazy { "DIALOG_BOOKMARK_MENU" }

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
//            init(fragmentManager, true, onError)
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

            // キーワードが更新されたら各リストを再生成する
            var initializedFilteringWord = false
            filteringWord.observeForever {
                if (initializedFilteringWord) {
                    reloadLists()
                }
                initializedFilteringWord = true
            }

            // 非表示ユーザーリストの更新を監視
            var initializedIgnoredUsers = false
            ignoredUsers.observeForever {
                if (initializedIgnoredUsers) {
                    reloadLists()
                }
                initializedIgnoredUsers = true
            }
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
    private suspend fun loadBasics(onError: OnError? = null) {
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
    suspend fun loadNextRecent(onError: OnError? = null) : List<Bookmark> {
        loadBasics(onError)
        val recent = repository.loadNextBookmarksRecentAsync().await()
        bookmarksRecent.postValue(repository.bookmarksRecent)
        bookmarksEntry.postValue(repository.bookmarksEntry)

        return recent
    }

    /** 指定ユーザーのブクマが得られるまで新着順ブクマリストを追加ロードする */
    fun loadNextRecentToUser(user: String, onError: OnError? = null) = viewModelScope.launch(
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
    fun updateDigest(onError: OnError? = null) = viewModelScope.launch(
        CoroutineExceptionHandler { _, e ->
            onError?.invoke(e)
        }
    ) {
        loadBasics()
        repository.loadBookmarksDigestAsync().await()
        bookmarksPopular.postValue(repository.bookmarksPopular)
    }

    /** 新着ブクマリストを再読み込み */
    fun updateRecent(onError: OnError? = null) = viewModelScope.launch(
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
        onSuccess: OnSuccess<Unit>? = null,
        onError: OnError? = null
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
            onSuccess?.invoke(Unit)
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
        quote: String = "",
        onSuccess: OnSuccess<Star>? = null,
        onError: OnError? = null,
        onFinally: OnFinally? = null
    ) = viewModelScope.launch(Dispatchers.Main) {
        try {
            val star = withContext(Dispatchers.Default) {
                val star = repository.postStar(bookmark, color, quote)
                repository.allStarsLiveData.update(bookmark)
                repository.userStarsLiveData.load()
                star
            }
            onSuccess?.invoke(star)
        }
        catch (e: Throwable) {
            onError?.invoke(e)
        }
        finally {
            onFinally?.invoke()
        }
    }

    /** ブコメにスターをつける(必要なら送信前にダイアログを表示する) */
    fun postStarDialog(
        bookmark: Bookmark,
        color: StarColor,
        quote: String = ""
    ) = viewModelScope.launch(Dispatchers.Main) {
        if (repository.usePostStarDialog) {
            val fragmentManager = fragmentManager ?: return@launch

            val dialog = PostStarDialog.createInstance(bookmark, color, quote)
            dialog.show(fragmentManager, DIALOG_POST_STAR)

            dialog.setOnPostStar {
                postStar(
                    bookmark,
                    color,
                    quote,
                    onSuccess = {
                        SatenaApplication.instance.showToast(
                            R.string.msg_post_star_succeeded,
                            bookmark.user
                        )
                    },
                    onError = {
                        SatenaApplication.instance.showToast(
                            R.string.msg_post_star_failed,
                            bookmark.user
                        )
                    }
                )
            }
        }
        else {
            postStar(
                bookmark,
                color,
                quote,
                onSuccess = {
                    SatenaApplication.instance.showToast(
                        R.string.msg_post_star_succeeded,
                        bookmark.user
                    )
                },
                onError = {
                    SatenaApplication.instance.showToast(
                        R.string.msg_post_star_failed,
                        bookmark.user
                    )
                }
            )
        }
    }

    /** ブコメのスターを削除するダイアログを表示する */
    fun deleteStarDialog(
        bookmark: Bookmark,
        stars: List<Star>,
        onSuccess: OnSuccess<Unit>? = null,
        onError: OnError? = null,
    ) = viewModelScope.launch(Dispatchers.Main) {
        val fragmentManager = fragmentManager ?: return@launch

        val dialog = StarDeletionDialog.createInstance(stars)
        dialog.show(fragmentManager, DIALOG_POST_STAR)

        dialog.setOnDeleteStars { selectedStars ->
            viewModelScope.launch {
                selectedStars.forEach { star ->
                    try {
                        // 選択したカラーのスター個数分すべて削除する
                        repeat(star.count) {
                            repository.deleteStar(bookmark, star)
                        }
                    }
                    catch (e: Throwable) {
                        onError?.invoke(e)
                    }
                }

                try {
                    repository.allStarsLiveData.update(bookmark)
                    repository.userStarsLiveData.load()

                    withContext(Dispatchers.Main) {
                        onSuccess?.invoke(Unit)
                    }
                }
                catch (e: Throwable) {
                    withContext(Dispatchers.Main) {
                        onError?.invoke(e)
                    }
                }
            }
        }
    }

    /** ブクマを通報 */
    fun reportBookmark(
        report: Report,
        onSuccess: OnSuccess<Unit>? = null,
        onError: OnError? = null
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
            onSuccess?.invoke(Unit)
        }
    }

    /** ユーザーのブクマを取得 */
    val userBookmark : Bookmark? get() = repository.userBookmark

    // --- BookmarkMenuDialogの処理 --- //

    /**
     * ブクマに対する操作メニューを表示する
     *
     * @param activity startActivityを使用するので、アクティブなactivityを渡す必要がある
     * @param bookmark 操作対象のブクマ
     * @param starTarget bookmark以外のブクマに対してスター削除操作を行う場合に渡す
     * (ブクマ詳細画面のスターリスト項目に対するメニューでは、スター削除対象だけがリスト項目ではなく詳細画面を表示しているブクマである)
     */
    fun openBookmarkMenuDialog(
        activity: BookmarksActivity,
        bookmark: Bookmark,
        starTarget: Bookmark = bookmark
    ) = viewModelScope.launch(Dispatchers.Main) {
        this@BookmarksViewModel.fragmentManager = activity.supportFragmentManager
        val fragmentManager = activity.supportFragmentManager

        val starsEntry = repository.getStarsEntryTo(starTarget.user)
        val ignored = repository.ignoredUsers.contains(bookmark.user)
        val userSignedIn = repository.userSignedIn

        BookmarkMenuDialog.createInstance(bookmark, starsEntry, ignored, userSignedIn).run {
            showAllowingStateLoss(fragmentManager, DIALOG_BOOKMARK_MENU)

            setOnShowEntries { onShowEntries(activity, it) }
            setOnIgnoreUser { onIgnoreUser(it, true) }
            setOnUnignoreUser { onIgnoreUser(it, false) }
            setOnReportBookmark { onReportBookmark(it) }
            setOnSetUserTag { onSetUserTag(it) }
            setOnDeleteStar { onDeleteStar(starTarget) }
        }
    }

    private fun onShowEntries(activity: BookmarksActivity, user: String) {
        val intent = Intent(activity, EntriesActivity::class.java).apply {
            putExtra(EntriesActivity.EXTRA_USER, user)
        }
        activity.startActivity(intent)
    }

    private fun onIgnoreUser(user: String, ignore: Boolean) {
        setUserIgnoreState(
            user,
            ignore,
            onSuccess = {
                val context = SatenaApplication.instance
                val msgId =
                    if (ignore) R.string.msg_ignore_user_succeeded
                    else R.string.msg_unignore_user_succeeded
                context.showToast(msgId, user)
            },
            onError = { e ->
                val context = SatenaApplication.instance
                val msgId =
                    if (ignore) R.string.msg_ignore_user_failed
                    else R.string.msg_unignore_user_failed
                context.showToast(msgId, user)
                Log.e("ignoreUser", "failed: user = $user")
                e.printStackTrace()
            }
        )
    }

    private fun onReportBookmark(bookmark: Bookmark) {
        val fragmentManager = fragmentManager ?: return
        ReportDialog.createInstance(entry, bookmark)
            .showAllowingStateLoss(fragmentManager, DIALOG_REPORT)
    }

    private fun onSetUserTag(user: String) {
        val fragmentManager = fragmentManager ?: return
        UserTagSelectionDialog.createInstance(user)
            .showAllowingStateLoss(fragmentManager, DIALOG_SELECT_USER_TAG)
    }

    private fun onDeleteStar(bookmark: Bookmark) {
        val userSignedIn = repository.userSignedIn ?: return
        val stars = repository.getStarsEntryTo(bookmark.user)?.allStars?.filter { it.user == userSignedIn } ?: return
        deleteStarDialog(
            bookmark,
            stars,
            onSuccess = { SatenaApplication.instance.showToast(R.string.msg_delete_star_succeeded) },
            onError = { e ->
                SatenaApplication.instance.showToast(R.string.msg_delete_star_failed)
                Log.e("DeleteStar", Log.getStackTraceString(e))
            }
        )
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
