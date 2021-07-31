package com.suihan74.satena.scenes.bookmarks.repository

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.suihan74.hatenaLib.*
import com.suihan74.satena.models.PreferenceKey
import com.suihan74.utilities.AccountLoader
import com.suihan74.utilities.SafeSharedPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
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

    val starPosting : LiveData<Boolean>

    /** ユーザーが所持しているカラースター数を取得する */
    suspend fun loadUserColorStarsCount()

    /** カラースターを使用できるかチェックする */
    suspend fun checkColorStarAvailability(color: StarColor) : Boolean

    // ------ //

    /**
     * ブクマにスターを付ける
     *
     * @throws ConnectionFailureException
     * @throws StarExhaustedException
     */
    suspend fun postStar(
        entry: Entry,
        bookmark: Bookmark,
        color: StarColor,
        quote: String = "",
        updateCacheImmediately: Boolean = true
    )

    /**
     * エントリにスターを付ける
     *
     * @throws ConnectionFailureException
     * @throws StarExhaustedException
     */
    suspend fun postStar(
        entry: Entry,
        color: StarColor,
        quote: String = "",
        updateCacheImmediately: Boolean = true
    )

    /**
     * スターを付ける
     *
     * @throws ConnectionFailureException
     * @throws StarExhaustedException
     */
    suspend fun postStar(
        url: String,
        color: StarColor,
        quote: String = "",
        updateCacheImmediately: Boolean = true
    )

    /**
     *  スターを解除する
     *
     * @throws ConnectionFailureException
     */
    suspend fun deleteStar(
        entry: Entry,
        bookmark: Bookmark,
        star: Star,
        updateCacheImmediately: Boolean = true
    )

    /**
     * 指定URLに付けられたスターのリストを取得する
     *
     * @throws ConnectionFailureException
     */
    suspend fun getStarsEntry(url: String, forceUpdate: Boolean = false) : LiveData<StarsEntry>

    /**
     * 渡された全URLに対するスター情報を取得し内部にキャッシュする
     *
     * @throws ConnectionFailureException
     */
    suspend fun loadStarsEntries(urls: List<String>, forceUpdate: Boolean = false)

    /**
     * キャッシュされたスターの中から指定ユーザーがつけたものを探す
     */
    suspend fun getUserStars(user: String) : List<LiveData<StarsEntry>>
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

    private val _starPosting = MutableLiveData<Boolean>()

    override val starPosting: LiveData<Boolean> = _starPosting

    /**
     * 取得したスター情報のキャッシュ
     *
     * key: URL, value: スター情報
     */
    private val starsEntriesCache = HashMap<String, MutableLiveData<StarsEntry>>()

    private val starsEntriesLock = Mutex()

    private suspend fun startPosting() = withContext(Dispatchers.Main) {
        _starPosting.value = true
    }

    private suspend fun stopPosting() = withContext(Dispatchers.Main) {
        _starPosting.value = false
    }

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
        if (color == StarColor.Yellow) return client.signedIn()
        if (_userColorStarsCount.value == null) {
            loadUserColorStarsCount()
        }
        return _userColorStarsCount.value?.has(color) ?: false
    }

    /**
     * ブクマにスターを付ける
     *
     * @throws ConnectionFailureException
     * @throws StarExhaustedException
     */
    override suspend fun postStar(
        entry: Entry,
        bookmark: Bookmark,
        color: StarColor,
        quote: String,
        updateCacheImmediately: Boolean
    ) {
        postStar(
            bookmark.getBookmarkUrl(entry),
            color,
            quote,
            updateCacheImmediately
        )
    }

    /**
     * エントリにスターを付ける
     *
     * @throws ConnectionFailureException
     * @throws StarExhaustedException
     */
    override suspend fun postStar(
        entry: Entry,
        color: StarColor,
        quote: String,
        updateCacheImmediately: Boolean
    ) {
        postStar(
            entry.url,
            color,
            quote,
            updateCacheImmediately
        )
    }

    /**
     * スターを付ける
     *
     * @throws ConnectionFailureException
     * @throws StarExhaustedException
     */
    override suspend fun postStar(
        url: String,
        color: StarColor,
        quote: String,
        updateCacheImmediately: Boolean
    ) = withContext(Dispatchers.Default) {
        startPosting()

        if (!checkColorStarAvailability(color)) {
            stopPosting()
            throw StarExhaustedException(color)
        }

        val result = runCatching {
            client.postStarAsync(
                url = url,
                color = color,
                quote = quote
            ).await()
        }

        if (result.isFailure) {
            stopPosting()
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

        if (updateCacheImmediately) {
            runCatching {
                getStarsEntry(url, forceUpdate = true)
            }
        }

        stopPosting()
    }


    /**
     * スターを解除する
     *
     * @throws ConnectionFailureException
     */
    override suspend fun deleteStar(
        entry: Entry,
        bookmark: Bookmark,
        star: Star,
        updateCacheImmediately: Boolean
    ) {
        startPosting()

        val url = bookmark.getBookmarkUrl(entry)
        val result = runCatching {
            signIn()
            repeat(star.count) {
                client.deleteStarAsync(
                    url = url,
                    star = star
                ).await()
            }
        }

        if (result.isFailure) {
            stopPosting()
            throw ConnectionFailureException(cause = result.exceptionOrNull())
        }

        if (updateCacheImmediately) {
            runCatching {
                getStarsEntry(url, forceUpdate = true)
            }
        }

        stopPosting()
    }

    private suspend fun signIn() {
        runCatching {
            accountLoader.signInHatenaAsync(reSignIn = false).await()
        }
    }

    /**
     * 指定URLに付けられたスターのリストを取得する
     *
     * @throws ConnectionFailureException
     */
    override suspend fun getStarsEntry(
        url: String,
        forceUpdate: Boolean
    ) : LiveData<StarsEntry> {
        starsEntriesLock.withLock {
            // 読み込み完了前に再度呼ばれた場合に同じ対象に対して通信が始まらないように
            // ひとまずのところ(getStarsEntryAsync()部分のために)処理全体をあえてロックで包んでいる

            if (!forceUpdate) {
                starsEntriesCache[url]?.let {
                    return it
                }
            }

            val result = runCatching {
                client.getStarsEntryAsync(url).await()
            }

            val entry = result.getOrElse {
                throw ConnectionFailureException()
            }

            val liveData = withContext(Dispatchers.Main) {
                starsEntriesCache[entry.url]?.also {
                    it.value = entry
                } ?: MutableLiveData(entry)
            }

            starsEntriesCache[entry.url] = liveData
            return liveData
        }
    }

    /**
     * 渡された全URLに対するスター情報を取得し内部にキャッシュする
     *
     * @throws ConnectionFailureException
     */
    override suspend fun loadStarsEntries(urls: List<String>, forceUpdate: Boolean) {
        starsEntriesLock.withLock {
            val targetUrls =
                if (forceUpdate) urls
                else urls.filterNot { starsEntriesCache.containsKey(it) }

            if (targetUrls.isEmpty()) return@withLock

            val result = runCatching {
                client.getStarsEntryAsync(targetUrls).await()
            }.onFailure {
                throw ConnectionFailureException(cause = it)
            }

            val entries = result.getOrNull()!!
            entries.forEach { entry ->
                val liveData = withContext(Dispatchers.Main.immediate) {
                    starsEntriesCache[entry.url]?.also {
                        it.value = entry
                    } ?: MutableLiveData(entry)
                }

                starsEntriesCache[entry.url] = liveData
            }
            // スターが付いていない場合，空のデータで埋める
            targetUrls.filter { url -> entries.none { it.url == url } }
                .forEach { url ->
                    starsEntriesCache[url] = MutableLiveData()
                }
        }
    }

    /**
     * キャッシュされたスターの中から指定ユーザーがつけたものを探す
     */
    override suspend fun getUserStars(user: String) : List<LiveData<StarsEntry>> {
        return starsEntriesLock.withLock {
            starsEntriesCache.values.filter { starsEntry ->
                starsEntry.value?.allStars?.any { it.user == user } == true
            }
        }
    }
}
