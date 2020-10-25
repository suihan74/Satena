package com.suihan74.satena.scenes.post

import android.webkit.URLUtil
import androidx.lifecycle.MutableLiveData
import com.suihan74.hatenaLib.*
import com.suihan74.satena.models.PreferenceKey
import com.suihan74.satena.modifySpecificUrls
import com.suihan74.satena.scenes.post.exceptions.CommentTooLongException
import com.suihan74.satena.scenes.post.exceptions.PostingMastodonFailureException
import com.suihan74.satena.scenes.post.exceptions.TooManyTagsException
import com.suihan74.utilities.AccountLoader
import com.suihan74.utilities.SafeSharedPreferences
import com.suihan74.utilities.exceptions.InvalidUrlException
import com.sys1yagi.mastodon4j.api.entity.Status
import com.sys1yagi.mastodon4j.api.method.Statuses
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.ceil

class BookmarkPostRepository(
    val accountLoader : AccountLoader,
    val prefs : SafeSharedPreferences<PreferenceKey>
) {
    companion object {
        /** 最大コメント文字数 */
        const val MAX_COMMENT_LENGTH = 100

        /** 同時使用可能な最大タグ個数 */
        const val MAX_TAGS_COUNT = 10
    }

    /** ダークテーマを使用 */
    val isThemeDark = prefs.getBoolean(PreferenceKey.DARK_THEME)

    val entry = MutableLiveData<Entry>()

    val bookmarksEntry = MutableLiveData<BookmarksEntry>()

    /** はてなのユーザー名 */
    var userName : String = ""
        private set

    /** Twitterアカウントが紐づいているか否か */
    val signedInTwitter = MutableLiveData<Boolean>()

    /** Mastodonアカウントが紐づいているか否か */
    val signedInMastodon = MutableLiveData<Boolean>()

    /** Facebookアカウントが紐づいているか否か */
    val signedInFacebook = MutableLiveData<Boolean>()

    /** 使用したことがあるタグのリスト */
    val tags = MutableLiveData<List<Tag>>()

    // ------ //

    /** 確認ダイアログを使用する */
    val useConfirmDialog =
        prefs.getBoolean(PreferenceKey.USING_POST_BOOKMARK_DIALOG)

    // ------ //

    /** 初期化 */
    @Throws(
        AccountLoader.HatenaSignInException::class,
        AccountLoader.MastodonSignInException::class,
    )
    suspend fun initialize(entry: Entry) = withContext(Dispatchers.Main) {
        this@BookmarkPostRepository.entry.value = entry
        signIn()
    }

    /**
     * 初期化
     *
     * URLからエントリ情報を作成する
     */
    @Throws(
        AccountLoader.HatenaSignInException::class,
        AccountLoader.MastodonSignInException::class,
        ConnectionFailureException::class
    )
    suspend fun initialize(url: String) = withContext(Dispatchers.Main) {
        signIn()

        val result = runCatching {
            val modifiedUrl = modifySpecificUrls(url) ?: throw ConnectionFailureException()
            entry.value = accountLoader.client.getEntryAsync(modifiedUrl).await()
        }

        if (result.isFailure) {
            throw ConnectionFailureException(cause = result.exceptionOrNull())
        }
    }

    /** はてなにサインインし、成功したらMastodonにもサインインを行う */
    @Throws(
        AccountLoader.HatenaSignInException::class,
        AccountLoader.MastodonSignInException::class
    )
    suspend fun signIn() = withContext(Dispatchers.Default) {
        val result = runCatching {
            accountLoader.signInHatenaAsync(reSignIn = false).await()!!
        }

        if (result.isSuccess) {
            val hatenaAccount = result.getOrElse {
                throw AccountLoader.HatenaSignInException()
            }
            userName = hatenaAccount.name
            signedInTwitter.postValue(hatenaAccount.isOAuthTwitter)
            signedInFacebook.postValue(hatenaAccount.isOAuthFaceBook)

            signInMastodon()
        }
        else {
            when (val e = result.exceptionOrNull()) {
                is AccountLoader.HatenaSignInException,
                is AccountLoader.MastodonSignInException -> throw e
            }
        }
    }

    /** Mastodonにサインインする */
    @Throws(
        AccountLoader.MastodonSignInException::class
    )
    suspend fun signInMastodon() {
        val result = runCatching {
            accountLoader.signInMastodonAsync(reSignIn = false).await()
        }

        if (result.isSuccess) {
            val mstdnAccount = accountLoader.mastodonClientHolder.account
            signedInMastodon.postValue(mstdnAccount?.isLocked == false)
        }
        else {
            throw result.exceptionOrNull() as? AccountLoader.MastodonSignInException
                ?: AccountLoader.MastodonSignInException()
        }
    }

    // ------ //

    /** 使用したことがあるタグリストを取得する */
    @Throws(
        ConnectionFailureException::class
    )
    suspend fun loadTags() = withContext(Dispatchers.Default) {
        if (!tags.value.isNullOrEmpty()) {
            return@withContext
        }

        val result = runCatching {
            if (accountLoader.client.signedIn()) {
                accountLoader.client.getUserTagsAsync().await()
            }
            else {
                emptyList()
            }
        }

        if (result.isSuccess) {
            tags.postValue(result.getOrDefault(emptyList()))
        }
        else {
            throw ConnectionFailureException(cause = result.exceptionOrNull())
        }
    }

    /**
     * 現在のコメントにタグを挿入/削除したものを返す
     */
    @Throws(
        TooManyTagsException::class
    )
    fun toggleTag(prevComment: String, tag: String) : String {
        val tagText = "[$tag]"

        val tagsArea = tagsRegex.find(prevComment)
        val tagsText = tagsArea?.value ?: ""

        return when {
            tagsText.contains(tagText) -> {
                buildString {
                    append(
                        tagsText.replace(tagText, ""),
                        prevComment.substring(tagsText.length)
                    )
                }
            }

            tagsText.contains("[]") -> {
                prevComment.replaceFirst("[]", tagText)
            }

            else -> {
                val matches = tagRegex.findAll(tagsText)

                // タグは10個まで
                if (matches.count() == MAX_TAGS_COUNT) {
                    throw TooManyTagsException()
                }

                buildString {
                    append(
                        tagsText,
                        tagText,
                        prevComment.substring(tagsText.length)
                    )
                }
            }
        }
    }

    // ------ //

    /** (単数の)タグを表現する正規表現 */
    val tagRegex = Regex("""\[[^%/:\[\]]+]""")

    /** コメントのタグ部分全体を表現する正規表現 */
    val tagsRegex by lazy { Regex("""^(\[[^%/:\[\]]*])+""") }

    /** ブクマを投稿する */
    @Throws(
        // URLが不正
        InvalidUrlException::class,
        // コメント長すぎ
        CommentTooLongException::class,
        // タグが多すぎ
        TooManyTagsException::class,
        // はてなへのブクマ投稿処理中での失敗
        ConnectionFailureException::class,
        // Mastodonへの投稿失敗(ブクマは自体は成功)
        PostingMastodonFailureException::class
    )
    suspend fun postBookmark(editData: BookmarkEditData) : BookmarkResult = withContext(Dispatchers.Default) {
        val entry = editData.entry

        // URLスキームがhttpかhttpsであることを確認する
        if (entry == null || !URLUtil.isNetworkUrl(entry.url)) {
            throw InvalidUrlException(entry?.url.orEmpty())
        }

        // コメント長チェック
        if (calcCommentLength(editData.comment) > MAX_COMMENT_LENGTH) {
            throw CommentTooLongException()
        }

        // タグ個数チェック
        val matches = tagRegex.findAll(editData.comment)
        if (matches.count() > MAX_TAGS_COUNT) {
            throw TooManyTagsException()
        }

        val result = runCatching {
            val account = accountLoader.signInHatenaAsync(reSignIn = false).await()
                ?: throw AccountLoader.HatenaSignInException()

            accountLoader.client.postBookmarkAsync(
                url = entry.url,
                comment = editData.comment,
                postTwitter = editData.postTwitter,
                postFacebook = editData.postFacebook,
                isPrivate = editData.private
            ).await()
        }

        val bookmarkResult = result.getOrElse {
            throw ConnectionFailureException(cause = result.exceptionOrNull())
        }

        if (editData.postMastodon) {
            val mstdnResult = runCatching {
                val status =
                    if (bookmarkResult.comment.isBlank()) "\"${entry.title}\" ${entry.url}"
                    else "${bookmarkResult.comment} / \"${entry.title}\" ${entry.url}"

                accountLoader.signInMastodonAsync(reSignIn = false).await()
                val client = accountLoader.mastodonClientHolder.client!!
                Statuses(client).postStatus(
                    status = status,
                    inReplyToId = null,
                    sensitive = false,
                    visibility = Status.Visibility.Public,
                    mediaIds = null,
                    spoilerText = null
                ).execute()
            }

            if (mstdnResult.isFailure) {
                throw PostingMastodonFailureException(cause = result.exceptionOrNull())
            }
        }

        return@withContext bookmarkResult
    }

    /**
     * 生文字列からコメント長を計算する
     *
     * 外部から分かる限りサーバ側での判定に極力近づけているが完璧である保証はない
     * タグは判定外で、その他部分はバイト数から判定している模様
     */
    fun calcCommentLength(commentRaw: String) : Int =
        ceil(commentRaw.replace(tagRegex, "").sumBy { c ->
            val code = c.toInt()
            when (code / 0xff) {
                0 -> 1
                1 -> if (code <= 0xc3bf) 1 else 3
                else -> 3
            }
        } / 3f).toInt()

    /** タグ部分の終了位置を取得する */
    fun getTagsEnd(s: CharSequence?) : Int {
        val results = tagsRegex.find(s ?: "")
        return results?.value?.length ?: 0
    }
}
