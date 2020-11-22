package com.suihan74.satena.scenes.preferences.ignored

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.suihan74.hatenaLib.*
import com.suihan74.satena.scenes.bookmarks.dialog.ReportDialog
import com.suihan74.utilities.AccountLoader
import com.suihan74.utilities.exceptions.TaskFailureException
import kotlinx.coroutines.Dispatchers
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
    val ignoredUsersCache : List<String>

    /** 非表示ユーザーリストのLiveData */
    val ignoredUsers : LiveData<List<String>>

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
    suspend fun getFollowers() : List<String>

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

    private val _ignoredUsersCache by lazy { ArrayList<String>() }

    private val _ignoredUsers by lazy {
        MutableLiveData<List<String>>()
    }

    override val ignoredUsersCache: List<String>
        get() = _ignoredUsersCache

    override val ignoredUsers: LiveData<List<String>>
        get() = _ignoredUsers

    override suspend fun loadIgnoredUsers() = withContext(Dispatchers.Default) {
        val result = runCatching {
            val client = signIn()
            client.getIgnoredUsersAsync().await()
        }

        if (result.isSuccess) {
            _ignoredUsersCache.clear()
            _ignoredUsersCache.addAll(result.getOrDefault(emptyList()).reversed())
            _ignoredUsers.postValue(_ignoredUsersCache)
        }
    }

    /**
     * ユーザーを非表示にする
     *
     * @throws FetchIgnoredUsersFailureException
     */
    override suspend fun ignoreUser(user: String) = withContext(Dispatchers.Default) {
        val result = runCatching {
            val client = signIn()
            client.ignoreUserAsync(user).await()
        }

        if (result.isSuccess) {
            _ignoredUsersCache.add(user)
            _ignoredUsers.postValue(_ignoredUsersCache)
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
    override suspend fun unIgnoreUser(user: String) = withContext(Dispatchers.Default) {
        val result = runCatching {
            val client = signIn()
            client.unignoreUserAsync(user).await()
        }

        if (result.isSuccess) {
            _ignoredUsersCache.remove(user)
            _ignoredUsers.postValue(_ignoredUsersCache)
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
    override suspend fun getFollowers() : List<String> {
        val result = runCatching {
            val client = signIn()
            if (client.signedIn()) {
                client.getFollowersAsync().await()
            }
            else {
                emptyList()
            }
        }

        return result.getOrElse {
            throw TaskFailureException(cause = result.exceptionOrNull())
        }
    }

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
        val result = runCatching {
            val client = signIn()
            client.reportAsync(entry, bookmark, category, model.comment).await()
        }

        if (result.isFailure) {
            throw TaskFailureException(cause = result.exceptionOrNull())
        }

        if (model.ignoreAfterReporting) {
            ignoreUser(model.user)
        }
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
