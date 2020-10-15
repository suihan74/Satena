package com.suihan74.satena.scenes.browser.bookmarks

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.suihan74.hatenaLib.*
import com.suihan74.satena.models.PreferenceKey
import com.suihan74.utilities.AccountLoader
import com.suihan74.utilities.SafeSharedPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** 使おうとしたカラースターの残数が0のときの例外 */
class StarExhaustedException(val color : StarColor) : RuntimeException(
    "${color.name} star has been exhausted."
)

/** スター関連の処理 */
interface StarRepositoryInterface {

    /** カラースター購入ページのURL */
    val purchaseColorStarsPageUrl : String
        get() = "https://www.hatena.ne.jp/shop/star"

    /** ユーザーが所持しているカラースター数 */
    val userColorStarsCount : LiveData<UserColorStarsCount>

    /** 確認ダイアログを表示するかどうか */
    val useConfirmPostingStarDialog : Boolean

    /** ユーザーが所持しているカラースター数を取得する */
    suspend fun loadUserColorStarsCount()

    /** カラースターを使用できるかチェックする */
    suspend fun checkColorStarAvailability(color: StarColor) : Boolean

    /** ブクマにスターを付ける */
    @Throws(
        ConnectionFailureException::class,
        StarExhaustedException::class
    )
    suspend fun postStar(
        entry: Entry,
        bookmark: Bookmark,
        color: StarColor,
        quote: String = ""
    )

    /** スターを解除する */
    @Throws(
        ConnectionFailureException::class
    )
    suspend fun deleteStar(
        entry: Entry,
        bookmark: Bookmark,
        star: Star
    )
}

// ------ //

class StarRepository(
    private val accountLoader: AccountLoader,
    private val prefs: SafeSharedPreferences<PreferenceKey>
) : StarRepositoryInterface {

    private val client = accountLoader.client

    private val _userColorStarsCount =
        MutableLiveData<UserColorStarsCount>()

    override val userColorStarsCount: LiveData<UserColorStarsCount>
        get() = _userColorStarsCount

    override val useConfirmPostingStarDialog: Boolean =
        prefs.getBoolean(PreferenceKey.USING_POST_STAR_DIALOG)

    /**
     * 所持カラースター数を取得する
     */
    override suspend fun loadUserColorStarsCount() = withContext(Dispatchers.Default) {
        signIn()

        if (!client.signedIn()) {
            return@withContext
        }

        val result = runCatching {
            client.getMyColorStarsAsync().await()
        }

        result.getOrNull()?.let {
            _userColorStarsCount.postValue(it)
        } ?: Unit
    }

    /**
     * カラースターを使用できるかチェックする
     */
    override suspend fun checkColorStarAvailability(color: StarColor) : Boolean {
        if (_userColorStarsCount.value == null) {
            loadUserColorStarsCount()
        }
        return _userColorStarsCount.value?.has(color) ?: false
    }

    /**
     * スターを付ける
     */
    @Throws(
        ConnectionFailureException::class,
        StarExhaustedException::class
    )
    override suspend fun postStar(
        entry: Entry,
        bookmark: Bookmark,
        color: StarColor,
        quote: String
    ) = withContext(Dispatchers.Default) {
        if (!checkColorStarAvailability(color)) {
            throw StarExhaustedException(color)
        }

        val result = runCatching {
            client.postStarAsync(
                url = bookmark.getBookmarkUrl(entry),
                color = color,
                quote = quote
            ).await()
        }

        if (result.isFailure) {
            throw ConnectionFailureException(cause = result.exceptionOrNull())
        }

        _userColorStarsCount.postValue(_userColorStarsCount.value?.let { prev ->
            when (color) {
                StarColor.Red -> prev.copy(red = prev.red - 1)
                StarColor.Green -> prev.copy(green = prev.green - 1)
                StarColor.Blue -> prev.copy(blue = prev.blue - 1)
                StarColor.Purple -> prev.copy(purple = prev.purple - 1)
                else -> prev
            }
        })
    }

    /**
     * スターを解除する
     */
    @Throws(
        ConnectionFailureException::class
    )
    override suspend fun deleteStar(
        entry: Entry,
        bookmark: Bookmark,
        star: Star
    ) = withContext(Dispatchers.Default) {
        val result = runCatching {
            signIn()
            client.deleteStarAsync(
                url = bookmark.getBookmarkUrl(entry),
                star = star
            ).await()
        }

        if (result.isFailure) {
            throw ConnectionFailureException(cause = result.exceptionOrNull())
        }
    }

    private suspend fun signIn() {
        runCatching {
            accountLoader.signInHatenaAsync(reSignIn = false).await()
        }
    }
}
