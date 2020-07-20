package com.suihan74.satena.scenes.post2

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.suihan74.hatenaLib.BookmarkResult
import com.suihan74.hatenaLib.Entry
import com.suihan74.hatenaLib.HatenaClient
import com.suihan74.hatenaLib.Tag
import com.suihan74.satena.modifySpecificUrls
import com.suihan74.utilities.AccountLoader
import com.suihan74.utilities.MastodonClientHolder
import com.suihan74.utilities.exceptions.InvalidUrlException
import com.sys1yagi.mastodon4j.api.entity.Status
import com.sys1yagi.mastodon4j.api.method.Statuses
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.ceil

typealias OnSuccess = (result: BookmarkResult)->Unit
typealias OnError = (e: Throwable)->Unit
typealias OnFinally = (e: Throwable?)->Unit

class ViewModel(
    private val client: HatenaClient,
    private val accountLoader: AccountLoader
) : ViewModel() {

    /** タグリスト読み込み失敗 */
    class LoadingTagsFailureException(cause: Throwable? = null) : Throwable("loading tags is failed.", cause)

    /** コメント長すぎ例外 */
    class CommentTooLongException : Throwable("the comment is too long to post.")

    /** 使用タグ数が制限を超える例外 */
    class TooManyTagsException : Throwable("too many tags (more than 10)")

    companion object {
        /** ブコメ最大文字数 */
        const val MAX_COMMENT_LENGTH = 100

        /** 最大使用タグ数 */
        const val MAX_TAGS_COUNT = 10
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

    /** 初期化 */
    fun init(url: String, editingComment: String?, onError: OnError? = null) = viewModelScope.launch(
        Dispatchers.Main + CoroutineExceptionHandler { _, e ->
            onError?.invoke(e)
        }
    ) {
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
    }

    /** 初期化 */
    fun init(entry: Entry, editingComment: String?, onError: OnError? = null) = viewModelScope.launch(
        Dispatchers.Main + CoroutineExceptionHandler { _, e ->
            onError?.invoke(e)
        }
    ) {
        this@ViewModel.entry.value = entry
        comment.value = editingComment
                    ?: entry.bookmarkedData?.comment
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
    }

    /** サインインが必要なら行う */
    private suspend fun signIn() {
        // Twitter
        accountLoader.signInHatenaAsync(reSignIn = false).await()?.let {
            signedInTwitter.value = it.isOAuthTwitter
            signedInFacebook.value = it.isOAuthFaceBook
        }

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
    private val tagRegex by lazy { Regex("""\[[^%/:\[\]]+]""") }

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

    /** ブクマを投稿 */
    fun postBookmark(
        onSuccess: OnSuccess? = null,
        onError: OnError? = null,
        onFinally: OnFinally? = null
    ) = viewModelScope.launch(Dispatchers.Default) {
        var error: Throwable? = null

        val entry =
            withContext(Dispatchers.Main) {
                try { entry.value!! }
                catch (e: Throwable) {
                    onError?.invoke(e)
                    null
                }
            } ?: return@launch


        // private投稿の場合他サービスに共有しない
        val privateEnabled = isPrivate.value == true
        val postTwitterEnabled = !privateEnabled && postTwitter.value == true
        val postFacebookEnabled = !privateEnabled && postFacebook.value == true
        val postMastodonEnabled = !privateEnabled && postMastodon.value == true

        val result =
            try {
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
            catch (e: Throwable) {
                error = e
                withContext(Dispatchers.Main) {
                    onError?.invoke(e)
                }
                null
            }

        // Mastodonに投稿
        if (error == null && postMastodonEnabled) {
            result!!

            try {
                val status =
                    if (result.comment.isBlank()) {
                        "\"${entry.title}\" ${entry.url}"
                    }
                    else {
                        "${result.comment} / \"${entry.title}\" ${entry.url}"
                    }

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
                onSuccess?.invoke(result!!)
            }
            onFinally?.invoke(error)
        }
    }

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
        if (commentText.contains(tagText)) {
            comment.value = commentText.replace(tagText, "")
        }
        else {
            val matches = tagRegex.findAll(commentText)

            // タグは10個まで
            if (matches.count() == MAX_TAGS_COUNT) {
                throw TooManyTagsException()
            }

            val lastExisted = matches.lastOrNull()
            val pos = lastExisted?.range?.endInclusive?.plus(1) ?: 0

            comment.value = buildString {
                append(
                    commentText.substring(0, pos),
                    tagText,
                    commentText.substring(pos)
                )
            }
        }
    }

    class Factory(
        private val client: HatenaClient,
        private val accountLoader: AccountLoader
    ) : ViewModelProvider.NewInstanceFactory() {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel?> create(modelClass: Class<T>) =
            ViewModel(client, accountLoader) as T
    }
}
