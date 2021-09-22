package com.suihan74.satena.scenes.preferences.ignored

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.suihan74.hatenaLib.*
import com.suihan74.satena.scenes.bookmarks.dialog.ReportDialog
import com.suihan74.utilities.AccountLoader
import com.suihan74.utilities.exceptions.TaskFailureException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * はてな内でのユーザー関係を扱うリポジトリ
 *
 * 非表示ユーザー，お気に入りユーザー，ユーザーの通報など
 *
 * 他のリポジトリに組み込む際はIgnoredUsersRepositoryに移譲する
 */
interface UserRelationRepositoryInterface {
    /** はてなで設定した非表示ユーザー */
//    val ignoredUsersCache : List<String>

    /** 非表示ユーザーリストのLiveData */
    val ignoredUsers : LiveData<List<String>>

    /** 通報処理中 */
    val reporting : LiveData<Boolean>

    /** リストを初期化する */
    suspend fun loadIgnoredUsers()

    /**
     * ユーザーを非表示にする
     *
     * @throws FetchIgnoredUsersFailureException
     */
    suspend fun ignoreUser(user: String)

    /**
     * ユーザーの非表示を解除する
     *
     * @throws FetchIgnoredUsersFailureException
     */
    suspend fun unIgnoreUser(user: String)

    /**
     * 指定ユーザーが非表示されているか確認する
     */
    suspend fun isIgnored(user: String) : Boolean

    // ------ //

    /**
     * ユーザーをお気に入りにする
     *
     * @throws TaskFailureException
     */
    suspend fun followUser(user: String)

    /**
     * ユーザーのお気に入りを解除する
     *
     * @throws TaskFailureException
     */
    suspend fun unFollowUser(user: String)

    /**
     * お気に入りユーザーリストを取得する
     *
     * @throws TaskFailureException
     */
    suspend fun getFollowings() : List<String>

    /**
     * お気に入られユーザーリストを取得する
     *
     * @throws TaskFailureException
     */
    suspend fun getFollowers() : List<Follower>

    /**
     *  ブコメを通報する
     *
     * @throws TaskFailureException
     * @throws FetchIgnoredUsersFailureException
     */
    suspend fun reportBookmark(
        entry: Entry,
        bookmark: Bookmark,
        category: ReportCategory,
        model: ReportDialog.Model
    )
}

/**
 * はてな内でのユーザー関係を扱うリポジトリ
 *
 * 非表示ユーザー，お気に入りユーザー，ユーザーの通報など
 */
class UserRelationRepository(
    private val accountLoader: AccountLoader
) : UserRelationRepositoryInterface {

    private val _ignoredUsersCache = ArrayList<String>()

    private val _ignoredUsers = MutableLiveData<List<String>>()
    override val ignoredUsers: LiveData<List<String>> = _ignoredUsers

    private val _reporting = MutableLiveData<Boolean>()
    override val reporting: LiveData<Boolean> = _reporting

    private val cacheMutex = Mutex()

    // ------ //

    /** 読み込み済みの内容をクリアする */
    suspend fun clearIgnoredUsers() = cacheMutex.withLock {
        _ignoredUsersCache.clear()
        withContext(Dispatchers.Main) {
            _ignoredUsers.value = emptyList()
        }
    }

    /** 非表示ユーザーリストを読み込む */
    override suspend fun loadIgnoredUsers() = cacheMutex.withLock {
        val result = runCatching {
            val client = signIn()
            client.getIgnoredUsersAsync().await()
        }

        if (result.isSuccess) {
            _ignoredUsersCache.clear()
            _ignoredUsersCache.addAll(result.getOrDefault(emptyList()).reversed())
            withContext(Dispatchers.Main) {
                _ignoredUsers.value = _ignoredUsersCache
            }
        }
    }

    /**
     * ユーザーを非表示にする
     *
     * @throws FetchIgnoredUsersFailureException
     */
    override suspend fun ignoreUser(user: String) {
        val result = runCatching {
            val client = signIn()
            client.ignoreUserAsync(user).await()
        }

        if (result.isSuccess) {
            cacheMutex.withLock {
                _ignoredUsersCache.add(user)
                withContext(Dispatchers.Main) {
                    _ignoredUsers.value = _ignoredUsersCache
                }
            }
        }
        else {
            val e = result.exceptionOrNull()
            throw FetchIgnoredUsersFailureException(
                message = e?.message,
                cause = e
            )
        }
    }

    /**
     * ユーザーの非表示を解除する
     *
     * @throws FetchIgnoredUsersFailureException
     */
    override suspend fun unIgnoreUser(user: String) {
        val result = runCatching {
            val client = signIn()
            client.unignoreUserAsync(user).await()
        }

        if (result.isSuccess) {
            cacheMutex.withLock {
                _ignoredUsersCache.remove(user)
                withContext(Dispatchers.Main) {
                    _ignoredUsers.value = _ignoredUsersCache
                }
            }
        }
        else {
            val e = result.exceptionOrNull()
            throw FetchIgnoredUsersFailureException(
                message = e?.message,
                cause = e
            )
        }
    }

    /**
     * 指定ユーザーが非表示されているか確認する
     */
    override suspend fun isIgnored(user: String) : Boolean = cacheMutex.withLock {
        return@withLock _ignoredUsersCache.contains(user)
    }

    // ------ //

    /**
     * ユーザーをお気に入りにする
     *
     * @throws TaskFailureException
     */
    override suspend fun followUser(user: String) {
        val result = runCatching {
            val client = signIn()
            client.follow(user)
        }

        if (result.isFailure) {
            throw TaskFailureException(cause = result.exceptionOrNull())
        }
    }

    /**
     * ユーザーのお気に入りを解除する
     *
     * @throws TaskFailureException
     */
    override suspend fun unFollowUser(user: String) {
        val result = runCatching {
            val client = signIn()
            client.unfollow(user)
        }

        if (result.isFailure) {
            throw TaskFailureException(cause = result.exceptionOrNull())
        }
    }

    /**
     * お気に入りユーザーリストを取得する
     *
     * @throws TaskFailureException
     */
    override suspend fun getFollowings() : List<String> {
        val result = runCatching {
            signIn().let { client ->
                if (client.signedIn()) client.getFollowingsAsync().await()
                else emptyList()
            }
        }

        return result.getOrElse {
            throw TaskFailureException(cause = result.exceptionOrNull())
        }
    }

    /**
     * お気に入られユーザーリストを取得する
     *
     * @throws TaskFailureException
     */
    override suspend fun getFollowers() : List<Follower> {
        val result = runCatching {
            signIn().let { client ->
                if (client.signedIn()) client.getFollowersAsync().await()
                else emptyList()
            }
        }

        return result.getOrElse {
            throw TaskFailureException(cause = result.exceptionOrNull())
        }
    }

    /** 通報処理開始 */
    private suspend fun startReporting() = withContext(Dispatchers.Main) { _reporting.value = true }
    /** 通報処理終了 */
    private suspend fun stopReporting() = withContext(Dispatchers.Main) { _reporting.value = false }

    /**
     *  ブコメを通報する
     *
     * @throws TaskFailureException
     * @throws FetchIgnoredUsersFailureException
     */
    override suspend fun reportBookmark(
        entry: Entry,
        bookmark: Bookmark,
        category: ReportCategory,
        model: ReportDialog.Model
    ) {
        startReporting()

        val result = runCatching {
            val client = signIn()
            client.reportAsync(entry, bookmark, category, model.comment).await()
        }

        if (result.isFailure) {
            stopReporting()
            throw TaskFailureException(cause = result.exceptionOrNull())
        }

        if (model.ignoreAfterReporting) {
            runCatching {
                ignoreUser(model.user)
            }.onFailure {
                stopReporting()
                throw it
            }
        }

        stopReporting()
    }

    /**
     * 必要ならサインインし直す
     *
     * @throws AccountLoader.HatenaSignInException
     */
    private suspend fun signIn() : HatenaClient {
        accountLoader.signInHatenaAsync(reSignIn = false).await()
        return accountLoader.client
    }
}
