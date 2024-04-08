package com.suihan74.satena.scenes.post

import android.content.Context
import android.content.Intent
import android.webkit.URLUtil
import androidx.lifecycle.MutableLiveData
import com.suihan74.hatenaLib.BookmarkResult
import com.suihan74.hatenaLib.ConnectionFailureException
import com.suihan74.hatenaLib.Entry
import com.suihan74.hatenaLib.Tag
import com.suihan74.satena.R
import com.suihan74.satena.SatenaApplication
import com.suihan74.satena.models.PreferenceKey
import com.suihan74.satena.models.Theme
import com.suihan74.satena.models.TootVisibility
import com.suihan74.satena.models.misskey.NoteVisibility
import com.suihan74.satena.modifySpecificUrls
import com.suihan74.satena.scenes.post.exceptions.CommentTooLongException
import com.suihan74.satena.scenes.post.exceptions.PostingMastodonFailureException
import com.suihan74.satena.scenes.post.exceptions.PostingMisskeyFailureException
import com.suihan74.satena.scenes.post.exceptions.TagAlreadyExistsException
import com.suihan74.satena.scenes.post.exceptions.TooManyTagsException
import com.suihan74.satena.scenes.preferences.createLiveDataEnum
import com.suihan74.utilities.AccountLoader
import com.suihan74.utilities.SafeSharedPreferences
import com.suihan74.utilities.exceptions.InvalidUrlException
import com.sys1yagi.mastodon4j.api.method.Statuses
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import java.time.LocalDateTime
import java.time.ZoneOffset
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
            ceil(commentRaw.replace(tagRegex, "").sumOf { c ->
                val code = c.code
                when (code / 0xff) {
                    0 -> 1
                    1 -> if (code <= 0xc3bf) 1 else 3
                    else -> 3
                }.toLong()
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

    /** Misskeyアカウントが紐づいているか否か */
    val signedInMisskey = MutableLiveData<Boolean>()

    /** Facebookアカウントが紐づいているか否か */
    val signedInFacebook = MutableLiveData<Boolean>()

    val private = MutableLiveData(false)

    val postTwitter = MutableLiveData(false)

    val postMastodon = MutableLiveData(false)

    val postMisskey = MutableLiveData(false)

    val postFacebook = MutableLiveData(false)

    val share = MutableLiveData(false)

    /** 使用したことがあるタグのリスト */
    val tags = MutableLiveData<List<Tag>>()

    // ------ //

    /** 外側タッチで閉じる */
    val closeOnTouchOutside =
        prefs.getBoolean(PreferenceKey.CLOSE_DIALOG_ON_TOUCH_OUTSIDE)

    /** 確認ダイアログを使用する */
    val useConfirmDialog =
        prefs.getBoolean(PreferenceKey.USING_POST_BOOKMARK_DIALOG)

    /** タグ入力ダイアログを最初から最大展開する */
    val expandAddingTagsDialogByDefault =
        prefs.getBoolean(PreferenceKey.POST_BOOKMARK_EXPAND_ADDING_TAGS_DIALOG_BY_DEFAULT)

    /** タグリストの並び順 */
    val tagsListOrder = createLiveDataEnum(prefs, PreferenceKey.POST_BOOKMARK_TAGS_LIST_ORDER,
        { v -> v.ordinal },
        { i -> TagsListOrder.fromOrdinal(i) }
    )

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
     * @throws AccountLoader.MisskeySignInException
     * @throws ConnectionFailureException
     */
    suspend fun initialize(url: String) = withContext(Dispatchers.Main) {
        signIn()

        if (prefs.getBoolean(PreferenceKey.POST_BOOKMARK_SAVE_STATES)) {
            private.value = prefs.getBoolean(PreferenceKey.POST_BOOKMARK_PRIVATE_LAST_CHECKED)
            share.value = prefs.getBoolean(PreferenceKey.POST_BOOKMARK_SHARE_LAST_CHECKED)
        }
        else {
            private.value = prefs.getBoolean(PreferenceKey.POST_BOOKMARK_PRIVATE_DEFAULT_CHECKED)
            share.value = prefs.getBoolean(PreferenceKey.POST_BOOKMARK_SHARE_DEFAULT_CHECKED)
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
     * はてなにサインインし、成功したらMastodon/Misskeyにもサインインを行う
     *
     * @throws AccountLoader.HatenaSignInException
     * @throws AccountLoader.MastodonSignInException
     * @throws AccountLoader.MisskeySignInException
     */
    private suspend fun signIn() = withContext(Dispatchers.Main) {
        val result = runCatching {
            accountLoader.signInHatena(reSignIn = false)!!
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

            listOf(
                async { signInMastodon() },
                async { signInMisskey() }
            ).awaitAll()
        }
        else {
            when (val e = result.exceptionOrNull()) {
                is AccountLoader.HatenaSignInException,
                is AccountLoader.MastodonSignInException,
                is AccountLoader.MisskeySignInException -> throw e
                else -> {}
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
            accountLoader.signInMastodon(reSignIn = false)
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

    /**
     * Misskeyにサインインする
     *
     * @throws AccountLoader.MisskeySignInException
     */
    private suspend fun signInMisskey() = withContext(Dispatchers.Main) {
        val result = runCatching {
            accountLoader.signInMisskey(reSignIn = false)
        }

        if (result.isSuccess) {
            val account = accountLoader.misskeyClientHolder.account
            val isActive = account != null
            signedInMisskey.value = isActive

            if (prefs.getBoolean(PreferenceKey.POST_BOOKMARK_SAVE_STATES)) {
                postMisskey.value =
                    isActive && prefs.getBoolean(PreferenceKey.POST_BOOKMARK_MISSKEY_LAST_CHECKED)
            }
            else {
                postMisskey.value =
                    isActive && prefs.getBoolean(PreferenceKey.POST_BOOKMARK_MISSKEY_DEFAULT_CHECKED)
            }
        }
        else {
            throw result.exceptionOrNull() as? AccountLoader.MisskeySignInException
                ?: AccountLoader.MisskeySignInException()
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
            result.getOrDefault(emptyList())
                .let { list -> tags.postValue(sortTagsList(list)) }
        }
        else {
            throw ConnectionFailureException(cause = result.exceptionOrNull())
        }
    }

    /** タグリストを並べ替える */
    private fun sortTagsList(list: List<Tag>) : List<Tag> = when(tagsListOrder.value) {
        TagsListOrder.INDEX -> list.sortedBy { it.index }
        TagsListOrder.COUNT -> list.sortedByDescending { it.count }
        else -> list
    }

    /** タグリストの表示を更新する */
    suspend fun updateTagsList() = withContext(Dispatchers.Default) {
        tags.postValue(sortTagsList(tags.value.orEmpty()))
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

    private fun insertTagToUsedTagsList(tag: String) : List<Tag> {
        val existingTags = tags.value.orEmpty()
        return if (existingTags.none { it.text == tag }) {
            buildList {
                add(Tag(tag, 0, 0, LocalDateTime.now().toEpochSecond(ZoneOffset.UTC)))
                addAll(existingTags)
            }
        }
        else existingTags
    }

    /**
     * 現在のコメントにタグを挿入/削除したものを返す
     *
     * @throws TagAlreadyExistsException タグが既にコメント中に存在する
     * @throws TooManyTagsException タグが多すぎる
     */
    fun insertTagToComment(prevComment: String, tag: String) : String {
        tags.value = insertTagToUsedTagsList(tag)

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
    suspend fun postBookmark(
        context: Context,
        editData: BookmarkEditData
    ) : Triple<BookmarkResult, PostingMastodonFailureException?, PostingMisskeyFailureException?> = withContext(Dispatchers.Default) {
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

        val bookmarkResult =
            runCatching {
                accountLoader.signInHatena(reSignIn = false) ?: throw AccountLoader.HatenaSignInException()
                accountLoader.client.postBookmarkAsync(
                    url = entry.url,
                    comment = editData.comment,
                    postTwitter = editData.postTwitter,
                    postFacebook = editData.postFacebook,
                    isPrivate = editData.private
                ).await()
            }.onFailure {
                throw ConnectionFailureException(cause = it)
            }.getOrThrow()

        SatenaApplication.instance.actionsRepository.emitUpdatingEntry(
            entry.copy(bookmarkedData = bookmarkResult)
        )

        var mstdnException : PostingMastodonFailureException? = null
        var misskeyException : PostingMisskeyFailureException? = null
        listOf(
            async {
                runCatching {
                    postToMastodon(entry, bookmarkResult, editData)
                }.onFailure {
                    mstdnException = it as PostingMastodonFailureException
                }
            },
            async {
                runCatching {
                    postToMisskey(entry, bookmarkResult, editData)
                }.onFailure {
                    misskeyException = it as PostingMisskeyFailureException
                }
            }
        ).awaitAll()

        if (editData.share) {
            val status =
                if (bookmarkResult.comment.isBlank()) "\"${entry.title}\" ${entry.url}"
                else "${bookmarkResult.comment} / \"${entry.title}\" ${entry.url}"
            val intent = Intent(Intent.ACTION_SEND).also {
                it.putExtra(Intent.EXTRA_TEXT, status)
                it.type = "text/plain"
            }
            context.startActivity(Intent.createChooser(intent, context.getString(R.string.share_after_post_title)))
        }

        return@withContext Triple(bookmarkResult, mstdnException, misskeyException)
    }

    /**
     * Mastodonに投稿する
     *
     * @throws PostingMastodonFailureException
     */
    private suspend fun postToMastodon(entry: Entry, bookmarkResult: BookmarkResult, editData: BookmarkEditData) {
        if (!editData.postMastodon) return
        runCatching {
            val visibility = TootVisibility.fromOrdinal(prefs.getInt(PreferenceKey.MASTODON_POST_VISIBILITY))
            val status =
                if (bookmarkResult.comment.isBlank()) "\"${entry.title}\" ${entry.url}"
                else "${bookmarkResult.comment} / \"${entry.title}\" ${entry.url}"

            accountLoader.signInMastodon(reSignIn = false)
            val client = accountLoader.mastodonClientHolder.client!!
            Statuses(client).postStatus(
                status = status,
                inReplyToId = null,
                sensitive = false,
                visibility = visibility.value,
                mediaIds = null,
                spoilerText = null
            ).execute()
        }.onFailure {
            throw PostingMastodonFailureException(cause = it)
        }
    }

    /**
     * Misskeyに投稿する
     *
     * @throws PostingMastodonFailureException
     */
    private suspend fun postToMisskey(entry: Entry, bookmarkResult: BookmarkResult, editData: BookmarkEditData) {
        if (!editData.postMisskey) return
        runCatching {
            val visibility = NoteVisibility.values()[prefs.getInt(PreferenceKey.MISSKEY_POST_VISIBILITY)]
            val status =
                if (bookmarkResult.comment.isBlank()) "\"${entry.title}\" ${entry.url}"
                else "${bookmarkResult.comment} / \"${entry.title}\" ${entry.url}"

            accountLoader.signInMisskey(reSignIn = false)
            val client = accountLoader.misskeyClientHolder.client!!
            client.notes.create(
                visibility = visibility.value,
                text = status
            )
        }.onFailure {
            throw PostingMisskeyFailureException(cause = it)
        }
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
            putBoolean(PreferenceKey.POST_BOOKMARK_MISSKEY_LAST_CHECKED, postMisskey.value ?: false)
        }
    }
}
