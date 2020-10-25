package com.suihan74.satena.scenes.post2

import androidx.fragment.app.FragmentManager
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.suihan74.hatenaLib.BookmarkResult
import com.suihan74.hatenaLib.Entry
import com.suihan74.hatenaLib.HatenaClient
import com.suihan74.hatenaLib.Tag
import com.suihan74.satena.R
import com.suihan74.satena.models.PreferenceKey
import com.suihan74.satena.modifySpecificUrls
import com.suihan74.satena.scenes.post2.dialog.ConfirmPostBookmarkDialog
import com.suihan74.utilities.*
import com.suihan74.utilities.exceptions.InvalidUrlException
import com.sys1yagi.mastodon4j.api.entity.Status
import com.sys1yagi.mastodon4j.api.method.Statuses
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.ceil

class BookmarkPostViewModel(
    private val client : HatenaClient,
    private val accountLoader : AccountLoader,
    private val prefs : SafeSharedPreferences<PreferenceKey>
) : ViewModel() {

    class NotSignedInException : Throwable("not signed in")

    /** タグリスト読み込み失敗 */
    class LoadingTagsFailureException(cause: Throwable? = null) : Throwable("loading tags is failed.", cause)

    /** コメント長すぎ例外 */
    class CommentTooLongException : Throwable("the comment is too long to post.")

    /** 使用タグ数が制限を超える例外 */
    class TooManyTagsException : Throwable("too many tags (more than 10)")

    /** 多重投稿例外 */
    class MultiplePostException : Throwable("multiple post")

    /** Mastodonへの投稿に失敗 */
    class PostingMastodonFailureException(cause: Throwable? = null) : Throwable(cause = cause)

    // ------ //

    companion object {
        /** ブコメ最大文字数 */
        const val MAX_COMMENT_LENGTH = 100

        /** 最大使用タグ数 */
        const val MAX_TAGS_COUNT = 10
    }

    // ------ //

    /** テーマ */
    val themeId : Int by lazy {
        if (prefs.getBoolean(PreferenceKey.DARK_THEME)) R.style.AppDialogTheme_Dark
        else R.style.AppDialogTheme_Light
    }

    /** 対象のエントリ */
    val entry by lazy {
        MutableLiveData<Entry>()
    }

    /** タグ利用履歴 */
    val tags by lazy {
        MutableLiveData<List<Tag>>()
    }

    /** ブクマコメント */
    val comment by lazy {
        MutableLiveData<String>("").apply {
            observeForever {
                commentLength.value = getCommentLength(value ?: "")
            }
        }
    }

    val user by lazy {
        client.account?.name ?: ""
    }

    /** コメント長 */
    val commentLength by lazy {
        MutableLiveData(0)
    }

    /** プライベート投稿か否か */
    val isPrivate by lazy { MutableLiveData<Boolean>() }

    /** Twitterアカウントが紐づいているか否か */
    val signedInTwitter by lazy { MutableLiveData<Boolean>() }

    /** Mastodonアカウントが紐づいているか否か */
    val signedInMastodon by lazy { MutableLiveData<Boolean>() }

    /** Facebookアカウントが紐づいているか否か */
    val signedInFacebook by lazy { MutableLiveData<Boolean>() }

    /** Twitterに投稿するか否か */
    val postTwitter by lazy { MutableLiveData<Boolean>() }

    /** Mastodonに投稿するか否か */
    val postMastodon by lazy { MutableLiveData<Boolean>() }

    /** Facebookに投稿するか否か */
    val postFacebook by lazy { MutableLiveData<Boolean>() }

    /** エントリタイトル部分を表示する */
    val displayEntryTitle by lazy { MutableLiveData(false) }

    /** 初期化済みかのフラグ */
    var initialized : Boolean = false

    /** 投稿処理中かを示すフラグ */
    val nowPosting by lazy { MutableLiveData(false) }

    // ------ //

    private val DIALOG_CONFIRM_POST_BOOKMARK by lazy { "DIALOG_CONFIRM_POST_BOOKMARK" }

    // ------ //

    /** 初期化 */
    fun init(url: String, editingComment: String?, onError: OnError? = null) = viewModelScope.launch(
        Dispatchers.Main + CoroutineExceptionHandler { _, e ->
            onError?.invoke(e)
        }
    ) {
        if (initialized) return@launch

        comment.value = editingComment ?: ""

        try {
            signIn()
        }
        catch (e: AccountLoader.MastodonSignInException) {
            onError?.invoke(e)
        }

        try {
            loadTags()
        }
        catch (e: LoadingTagsFailureException) {
            onError?.invoke(e)
        }

        if (!(url.startsWith("http://") || url.startsWith("https://"))) {
            onError?.invoke(InvalidUrlException(url))
        }
        else {
            val modifiedUrl = modifySpecificUrls(url) ?: url
            entry.value = client.getEntryAsync(modifiedUrl).await()
        }

        initialized = true
    }

    /** 初期化 */
    fun init(entry: Entry, editingComment: String?, onError: OnError? = null) = viewModelScope.launch(
        Dispatchers.Main + CoroutineExceptionHandler { _, e ->
            onError?.invoke(e)
        }
    ) {
        if (initialized) return@launch

        this@BookmarkPostViewModel.entry.value = entry
        comment.value = editingComment
                    ?: entry.bookmarkedData?.commentRaw
                    ?: ""
        isPrivate.value = entry.bookmarkedData?.private ?: false

        try {
            signIn()
        }
        catch (e: AccountLoader.MastodonSignInException) {
            onError?.invoke(e)
        }

        try {
            loadTags()
        }
        catch (e: LoadingTagsFailureException) {
            onError?.invoke(e)
        }

        initialized = true
    }

    /** サインインが必要なら行う */
    private suspend fun signIn() {
        // Hatena
        accountLoader.signInHatenaAsync(reSignIn = false).await()?.let {
            signedInTwitter.value = it.isOAuthTwitter
            signedInFacebook.value = it.isOAuthFaceBook
        } ?: throw NotSignedInException()

        // Mastodon
        accountLoader.signInMastodonAsync(reSignIn = false).await()?.let {
            signedInMastodon.value = true
        }
    }

    /** タグをロード */
    suspend fun loadTags() {
        try {
            tags.value = client.getUserTagsAsync().await()
        }
        catch (e: Throwable) {
            throw LoadingTagsFailureException(e)
        }
    }

    /** コメント中でタグと判断される箇所のregex */
    val tagRegex by lazy { Regex("""\[[^%/:\[\]]+]""") }
    val tagsRegex by lazy { Regex("""^(\[[^%/:\[\]]*])+""") }

    /** コメント長を計算する */
    private fun getCommentLength(comment: String) =
        ceil(comment.replace(tagRegex, "").sumBy { c ->
            val code = c.toInt()
            when (code / 255) {
                0 -> 1
                1 -> if (code <= 0xc3bf) 1 else 3
                else -> 3
            }
        } / 3f).toInt()

    /** ブックマークを投稿する（必要ならダイアログで確認する） */
    fun postBookmark(
        fragmentManager: FragmentManager,
        onSuccess: OnSuccess<BookmarkResult>? = null,
        onError: OnError? = null,
        onFinally: OnFinally? = null
    ) = viewModelScope.launch(Dispatchers.Main) {
        if (!checkPostable(onError)) return@launch

        val onFinallyAction: OnFinally = {
            nowPosting.value = false
            onFinally?.invoke()
        }

        val showDialog = prefs.getBoolean(PreferenceKey.USING_POST_BOOKMARK_DIALOG)
        if (showDialog) {
            ConfirmPostBookmarkDialog.createInstance(this@BookmarkPostViewModel).run {
                showAllowingStateLoss(fragmentManager, DIALOG_CONFIRM_POST_BOOKMARK)

                setOnApprovedToPost {
                    viewModelScope.launch {
                        postBookmarkImpl(onSuccess, onError, onFinallyAction)
                    }
                }
            }
        }
        else {
            postBookmarkImpl(onSuccess, onError, onFinallyAction)
        }
    }

    /** ブクマを投稿 */
    private suspend fun postBookmarkImpl(
        onSuccess: OnSuccess<BookmarkResult>? = null,
        onError: OnError? = null,
        onFinally: OnFinally? = null
    ) = withContext(Dispatchers.Default) {

        withContext(Dispatchers.Main) {
            nowPosting.value = true
        }

        val entry =
            withContext(Dispatchers.Main) {
                try { entry.value!! }
                catch (e: Throwable) {
                    onError?.invoke(e)
                    null
                }
            } ?: return@withContext

        // private投稿の場合他サービスに共有しない
        val privateEnabled = isPrivate.value == true
        val postTwitterEnabled = !privateEnabled && postTwitter.value == true
        val postFacebookEnabled = !privateEnabled && postFacebook.value == true
        val postMastodonEnabled = !privateEnabled && postMastodon.value == true

        val result = kotlin.runCatching {
            val comment = comment.value ?: ""

            // コメント長チェック
            if (getCommentLength(comment) > MAX_COMMENT_LENGTH) {
                throw CommentTooLongException()
            }

            // タグ個数チェック
            val matches = tagRegex.findAll(comment)
            if (matches.count() > MAX_TAGS_COUNT) {
                throw TooManyTagsException()
            }

            client.postBookmarkAsync(
                url = entry.url,
                comment = comment,
                postTwitter = postTwitterEnabled,
                postFacebook = postFacebookEnabled,
                isPrivate = privateEnabled
            ).await()
        }

        var error : Throwable? =
            if (result.isFailure) {
                (result.exceptionOrNull() ?: RuntimeException()).also { e ->
                    withContext(Dispatchers.Main) {
                        onError?.invoke(e)
                    }
                }
            }
            else null

        val bookmarkResult = result.getOrNull()

        // Mastodonに投稿
        if (error == null && postMastodonEnabled) {
            bookmarkResult!!

            try {
                val status =
                    if (bookmarkResult.comment.isBlank()) "\"${entry.title}\" ${entry.url}"
                    else "${bookmarkResult.comment} / \"${entry.title}\" ${entry.url}"

                val client = MastodonClientHolder.client!!
                Statuses(client).postStatus(
                    status = status,
                    inReplyToId = null,
                    sensitive = false,
                    visibility = Status.Visibility.Public,
                    mediaIds = null,
                    spoilerText = null
                ).execute()
            }
            catch (e: Throwable) {
                withContext(Dispatchers.Main) {
                    onError?.invoke(e)
                }
                error = e
            }
        }

        withContext(Dispatchers.Main) {
            if (error == null) {
                onSuccess?.invoke(bookmarkResult!!)
            }
            onFinally?.invoke()
        }
    }

    /** 投稿可能な状態かを調べる */
    fun checkPostable(onError: OnError? = null) : Boolean =
        if (nowPosting.value == true) {
            onError?.invoke(MultiplePostException())
            false
        }
        else checkCommentLength(onError)

    /** コメントの長さをチェックする */
    fun checkCommentLength(onError: OnError? = null) =
        (getCommentLength(comment.value ?: "") <= MAX_COMMENT_LENGTH).also {
            if (!it) {
                onError?.invoke(CommentTooLongException())
            }
        }

    /** コメントにタグを挿入/削除 */
    fun toggleTag(tag: String) {
        val tagText = "[$tag]"
        val commentText = comment.value ?: ""

        val tagsArea = tagsRegex.find(commentText)
        val tagsText = tagsArea?.value ?: ""

        if (tagsText.contains(tagText)) {
            comment.value = buildString {
                append(
                    tagsText.replace(tagText, ""),
                    commentText.substring(tagsText.length)
                )
            }
        }
        else if (tagsText.contains("[]")) {
            comment.value = commentText.replaceFirst("[]", tagText)
        }
        else {
            val matches = tagRegex.findAll(tagsText)

            // タグは10個まで
            if (matches.count() == MAX_TAGS_COUNT) {
                throw TooManyTagsException()
            }

            comment.value = buildString {
                append(
                    tagsText,
                    tagText,
                    commentText.substring(tagsText.length)
                )
            }
        }
    }

    /** タグ部分の終了位置を取得する */
    fun getTagsEnd(s: CharSequence?) : Int {
        val results = tagsRegex.find(s ?: "")
        return results?.value?.length ?: 0
    }
}
