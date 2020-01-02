package com.suihan74.satena.scenes.post2

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.suihan74.HatenaLib.Entry
import com.suihan74.HatenaLib.HatenaClient
import com.suihan74.HatenaLib.SearchType
import com.suihan74.HatenaLib.Tag
import com.suihan74.utilities.AccountLoader
import com.suihan74.utilities.MastodonClientHolder
import com.sys1yagi.mastodon4j.api.entity.Status
import com.sys1yagi.mastodon4j.api.method.Statuses
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.ceil

typealias OnSuccess = ()->Unit
typealias OnError = (e: Throwable)->Unit

class ViewModel(
    private val client: HatenaClient,
    private val accountLoader: AccountLoader
) : ViewModel() {

    /** タグリスト読み込み失敗 */
    class LoadingTagsFailureException(cause: Throwable? = null) : Throwable("loading tags is failed.", cause)

    /** コメント長すぎ例外 */
    class CommentTooLongException : Throwable("the comment is too long to post.")

    companion object {
        /** ブコメ最大文字数 */
        const val MAX_COMMENT_LENGTH = 100
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

    /** 初期化 */
    fun init(url: String, onError: OnError? = null) = viewModelScope.launch(
        Dispatchers.Main + CoroutineExceptionHandler { _, e ->
            onError?.invoke(e)
        }
    ) {
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

        entry.value = client.searchEntriesAsync(url, SearchType.Text).await()
            .firstOrNull { it.url == url }
            ?: client.getEmptyEntryAsync(url).await()
    }

    /** 初期化 */
    fun init(entry: Entry, onError: OnError? = null) = viewModelScope.launch(
        Dispatchers.Main + CoroutineExceptionHandler { _, e ->
            onError?.invoke(e)
        }
    ) {
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

        this@ViewModel.entry.value = entry
        entry.bookmarkedData?.comment?.let {
            comment.value = it
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
    fun postBookmark(onSuccess: OnSuccess? = null, onError: OnError? = null) = viewModelScope.launch(
        Dispatchers.Main + CoroutineExceptionHandler { _, e ->
            onError?.invoke(e)
        }
    ) {
        val comment = comment.value ?: ""
        if (getCommentLength(comment) > MAX_COMMENT_LENGTH) {
            throw CommentTooLongException()
        }

        val entry = entry.value!!

        val result = client.postBookmarkAsync(
            url = entry.url,
            comment = comment,
            postTwitter = postTwitter.value == true,
            postFacebook = postFacebook.value == true,
            isPrivate = isPrivate.value == true
        ).await()

        // Mastodonに投稿
        if (postMastodon.value == true) {
            val status =
                if (result.comment.isBlank()) {
                    "\"${entry.title}\" ${entry.url}"
                }
                else {
                    "${result.comment} / \"${entry.title}\" ${entry.url}"
                }

            withContext(Dispatchers.IO) {
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
        }

        onSuccess?.invoke()
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
