package com.suihan74.satena.scenes.post

import android.webkit.URLUtil
import androidx.lifecycle.MutableLiveData
import com.suihan74.hatenaLib.*
import com.suihan74.satena.models.PreferenceKey
import com.suihan74.satena.models.Theme
import com.suihan74.satena.models.TootVisibility
import com.suihan74.satena.modifySpecificUrls
import com.suihan74.satena.scenes.post.exceptions.CommentTooLongException
import com.suihan74.satena.scenes.post.exceptions.PostingMastodonFailureException
import com.suihan74.satena.scenes.post.exceptions.TagAlreadyExistsException
import com.suihan74.satena.scenes.post.exceptions.TooManyTagsException
import com.suihan74.utilities.AccountLoader
import com.suihan74.utilities.SafeSharedPreferences
import com.suihan74.utilities.exceptions.InvalidUrlException
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

        /** (単数の)タグを表現する正規表現 */
        private val tagRegex = Regex("""\[[^%/:\[\]]+]""")

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

        /**
         * コメント長が制限内か確認する
         *
         * @return 投稿可能 (文字列長が制限以内)
         */
        fun checkCommentLength(commentRaw: String) : Boolean =
            calcCommentLength(commentRaw) <= MAX_COMMENT_LENGTH

        /**
         * 使用タグ数が制限内か確認する
         *
         * @return 投稿可能 (タグ数が制限以内)
         */
        fun checkTagsCount(commentRaw: String) : Boolean {
            val matches = tagRegex.findAll(commentRaw)
            return matches.count() <= MAX_TAGS_COUNT
        }
    }

    /** ダイアログテーマ */
    val themeId = Theme.dialogActivityThemeId(prefs)

    val entry = MutableLiveData<Entry>()

    /** はてなのユーザー名 */
    var userName : String = ""
        private set

    /** Twitterアカウントが紐づいているか否か */
    val signedInTwitter = MutableLiveData<Boolean>()

    /** Mastodonアカウントが紐づいているか否か */
    val signedInMastodon = MutableLiveData<Boolean>()

    /** Facebookアカウントが紐づいているか否か */
    val signedInFacebook = MutableLiveData<Boolean>()

    val private = MutableLiveData(false)

    val postTwitter = MutableLiveData(false)

    val postMastodon = MutableLiveData(false)

    val postFacebook = MutableLiveData(false)

    /** 使用したことがあるタグのリスト */
    val tags = MutableLiveData<List<Tag>>()

    // ------ //

    /** 外側タッチで閉じる */
    val closeOnTouchOutside =
        prefs.getBoolean(PreferenceKey.CLOSE_DIALOG_ON_TOUCH_OUTSIDE)

    /** 確認ダイアログを使用する */
    val useConfirmDialog =
        prefs.getBoolean(PreferenceKey.USING_POST_BOOKMARK_DIALOG)

    // ------ //

    /**
     * 初期化
     *
     * @throws AccountLoader.HatenaSignInException
     * @throws AccountLoader.MastodonSignInException
     */
    suspend fun initialize(entry: Entry) = withContext(Dispatchers.Main) {
        this@BookmarkPostRepository.entry.value = entry
        signIn()

        if (prefs.getBoolean(PreferenceKey.POST_BOOKMARK_SAVE_STATES)) {
            private.value = prefs.getBoolean(PreferenceKey.POST_BOOKMARK_PRIVATE_LAST_CHECKED)
        }
        else {
            private.value = prefs.getBoolean(PreferenceKey.POST_BOOKMARK_PRIVATE_DEFAULT_CHECKED)
        }
    }

    /**
     * 初期化
     *
     * URLからエントリ情報を作成する
     *
     * @throws AccountLoader.HatenaSignInException
     * @throws AccountLoader.MastodonSignInException
     * @throws ConnectionFailureException
     */
    suspend fun initialize(url: String) = withContext(Dispatchers.Main) {
        signIn()

        if (prefs.getBoolean(PreferenceKey.POST_BOOKMARK_SAVE_STATES)) {
            private.value = prefs.getBoolean(PreferenceKey.POST_BOOKMARK_PRIVATE_LAST_CHECKED)
        }
        else {
            private.value = prefs.getBoolean(PreferenceKey.POST_BOOKMARK_PRIVATE_DEFAULT_CHECKED)
        }

        val result = runCatching {
            val modifiedUrl = modifySpecificUrls(url) ?: throw ConnectionFailureException()
            entry.value = accountLoader.client.getEntryAsync(modifiedUrl).await()
        }

        if (result.isFailure) {
            throw ConnectionFailureException(cause = result.exceptionOrNull())
        }
    }

    /**
     * はてなにサインインし、成功したらMastodonにもサインインを行う
     *
     * @throws AccountLoader.HatenaSignInException
     * @throws AccountLoader.MastodonSignInException
     */
    private suspend fun signIn() = withContext(Dispatchers.Main) {
        val result = runCatching {
            accountLoader.signInHatenaAsync(reSignIn = false).await()!!
        }

        if (result.isSuccess) {
            val hatenaAccount = result.getOrElse {
                throw AccountLoader.HatenaSignInException()
            }
            userName = hatenaAccount.name

            val isTwitterActive = hatenaAccount.isOAuthTwitter
            val isFacebookActive = hatenaAccount.isOAuthFaceBook
            signedInTwitter.value = isTwitterActive
            signedInFacebook.value = isFacebookActive

            if (prefs.getBoolean(PreferenceKey.POST_BOOKMARK_SAVE_STATES)) {
                postTwitter.value =
                    isTwitterActive && prefs.getBoolean(PreferenceKey.POST_BOOKMARK_TWITTER_LAST_CHECKED)
                postFacebook.value =
                    isFacebookActive && prefs.getBoolean(PreferenceKey.POST_BOOKMARK_FACEBOOK_LAST_CHECKED)
            }
            else {
                postTwitter.value =
                    isTwitterActive && prefs.getBoolean(PreferenceKey.POST_BOOKMARK_TWITTER_DEFAULT_CHECKED)
                postFacebook.value =
                    isFacebookActive && prefs.getBoolean(PreferenceKey.POST_BOOKMARK_FACEBOOK_DEFAULT_CHECKED)
            }

            signInMastodon()
        }
        else {
            when (val e = result.exceptionOrNull()) {
                is AccountLoader.HatenaSignInException,
                is AccountLoader.MastodonSignInException -> throw e
            }
        }
    }

    /**
     * Mastodonにサインインする
     *
     * @throws AccountLoader.MastodonSignInException
     */
    private suspend fun signInMastodon() = withContext(Dispatchers.Main) {
        val result = runCatching {
            accountLoader.signInMastodonAsync(reSignIn = false).await()
        }

        if (result.isSuccess) {
            val mstdnAccount = accountLoader.mastodonClientHolder.account
            val isMastodonActive = mstdnAccount?.isLocked == false
            signedInMastodon.value = isMastodonActive

            if (prefs.getBoolean(PreferenceKey.POST_BOOKMARK_SAVE_STATES)) {
                postMastodon.value =
                    isMastodonActive && prefs.getBoolean(PreferenceKey.POST_BOOKMARK_MASTODON_LAST_CHECKED)
            }
            else {
                postMastodon.value =
                    isMastodonActive && prefs.getBoolean(PreferenceKey.POST_BOOKMARK_MASTODON_DEFAULT_CHECKED)
            }
        }
        else {
            throw result.exceptionOrNull() as? AccountLoader.MastodonSignInException
                ?: AccountLoader.MastodonSignInException()
        }
    }

    // ------ //

    /**
     * 使用したことがあるタグリストを取得する
     *
     * @throws ConnectionFailureException
     */
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
     *
     * @throws TooManyTagsException
     */
    fun toggleCommentTag(prevComment: String, tag: String) : String {
        val tagText = "[$tag]"
        val tagsArea = tagsRegex.find(prevComment)
        val tagsText = tagsArea?.value ?: ""
        return when {
            tagsText.contains(tagText) -> buildString {
                append(
                    tagsText.replace(tagText, ""),
                    prevComment.substring(tagsText.length)
                )
            }

            tagsText.contains("[]") -> prevComment.replaceFirst("[]", tagText)

            else -> insertTagToCommentImpl(prevComment, tagText, tagsText)
        }
    }

    /**
     * 現在のコメントにタグを挿入/削除したものを返す
     *
     * @throws TagAlreadyExistsException タグが既にコメント中に存在する
     * @throws TooManyTagsException タグが多すぎる
     */
    fun insertTagToComment(prevComment: String, tag: String) : String {
        val tagText = "[$tag]"
        val tagsArea = tagsRegex.find(prevComment)
        val tagsText = tagsArea?.value ?: ""
        return when {
            tagsText.contains(tagText) -> throw TagAlreadyExistsException(tag)

            tagsText.contains("[]") -> prevComment.replaceFirst("[]", tagText)

            else -> insertTagToCommentImpl(prevComment, tagText, tagsText)
        }
    }

    /**
     * コメントの先頭にタグを追加する
     *
     * @throws TooManyTagsException
     */
    private fun insertTagToCommentImpl(prevComment: String, newTag: String, existingTags: String) : String {
        if (!checkTagsCount(existingTags)) {
            throw TooManyTagsException(MAX_TAGS_COUNT)
        }

        return buildString {
            append(
                existingTags,
                newTag,
                prevComment.substring(existingTags.length)
            )
        }
    }

    // ------ //

    /** コメントのタグ部分全体を表現する正規表現 */
    private val tagsRegex by lazy { Regex("""^(\[[^%/:\[\]]*])+""") }

    /**
     * ブクマを投稿する
     *
     * @throws InvalidUrlException URLが不正
     * @throws CommentTooLongException コメント長すぎ
     * @throws TooManyTagsException タグが多すぎ
     * @throws ConnectionFailureException はてなへのブクマ投稿処理中での失敗
     * @throws PostingMastodonFailureException Mastodonへの投稿失敗(ブクマは自体は成功)
     */
    suspend fun postBookmark(editData: BookmarkEditData) : BookmarkResult = withContext(Dispatchers.Default) {
        val entry = editData.entry

        // URLスキームがhttpかhttpsであることを確認する
        if (entry == null || !URLUtil.isNetworkUrl(entry.url)) {
            throw InvalidUrlException(entry?.url.orEmpty())
        }

        // コメント長チェック
        if (!checkCommentLength(editData.comment)) {
            throw CommentTooLongException(MAX_COMMENT_LENGTH)
        }

        // タグ個数チェック
        if (!checkTagsCount(editData.comment)) {
            throw TooManyTagsException(MAX_TAGS_COUNT)
        }

        // 連携状態を保存
        saveStates()

        val result = runCatching {
            accountLoader.signInHatenaAsync(reSignIn = false).await()
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
                val visibility = TootVisibility.fromOrdinal(prefs.getInt(PreferenceKey.MASTODON_POST_VISIBILITY))

                val status =
                    if (bookmarkResult.comment.isBlank()) "\"${entry.title}\" ${entry.url}"
                    else "${bookmarkResult.comment} / \"${entry.title}\" ${entry.url}"

                accountLoader.signInMastodonAsync(reSignIn = false).await()
                val client = accountLoader.mastodonClientHolder.client!!
                Statuses(client).postStatus(
                    status = status,
                    inReplyToId = null,
                    sensitive = false,
                    visibility = visibility.value,
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

    /** タグ部分の終了位置を取得する */
    fun getTagsEnd(s: CharSequence?) : Int {
        val results = tagsRegex.find(s ?: "")
        return results?.value?.length ?: 0
    }

    // ------ //

    /**
     * 連携状態を保存する
     */
    fun saveStates() {
        prefs.edit {
            putBoolean(PreferenceKey.POST_BOOKMARK_PRIVATE_LAST_CHECKED, private.value ?: false)
            putBoolean(PreferenceKey.POST_BOOKMARK_TWITTER_LAST_CHECKED, postTwitter.value ?: false)
            putBoolean(PreferenceKey.POST_BOOKMARK_FACEBOOK_LAST_CHECKED, postFacebook.value ?: false)
            putBoolean(PreferenceKey.POST_BOOKMARK_MASTODON_LAST_CHECKED, postMastodon.value ?: false)
        }
    }
}
