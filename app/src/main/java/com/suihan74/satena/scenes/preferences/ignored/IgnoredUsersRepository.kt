package com.suihan74.satena.scenes.preferences.ignored

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.suihan74.hatenaLib.*
import com.suihan74.satena.scenes.bookmarks2.dialog.ReportDialog
import com.suihan74.utilities.AccountLoader
import com.suihan74.utilities.exceptions.TaskFailureException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 他のリポジトリに組み込む際はIgnoredUsersRepositoryに移譲する
 */
interface IgnoredUsersRepositoryInterface {
    /** はてなで設定した非表示ユーザー */
    val ignoredUsersCache : List<String>

    /** 非表示ユーザーリストのLiveData */
    val ignoredUsers : LiveData<List<String>>

    /** リストを初期化する */
    suspend fun loadIgnoredUsers()

    /** ユーザーを非表示にする */
    @Throws(FetchIgnoredUsersFailureException::class)
    suspend fun ignoreUser(user: String)

    /** ユーザーの非表示を解除する */
    @Throws(FetchIgnoredUsersFailureException::class)
    suspend fun unIgnoreUser(user: String)

    /** ブコメを通報する */
    @Throws(TaskFailureException::class, FetchIgnoredUsersFailureException::class)
    suspend fun reportBookmark(
        entry: Entry,
        bookmark: Bookmark,
        category: ReportCategory,
        model: ReportDialog.Model
    )
}

/**
 * 非表示ユーザー情報を扱うリポジトリ
 */
class IgnoredUsersRepository(
    private val accountLoader: AccountLoader
) : IgnoredUsersRepositoryInterface {

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

    @Throws(FetchIgnoredUsersFailureException::class)
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

    @Throws(FetchIgnoredUsersFailureException::class)
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

    /** ブコメを通報する */
    @Throws(TaskFailureException::class, FetchIgnoredUsersFailureException::class)
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

    /** 必要ならサインインし直す */
    @Throws(AccountLoader.HatenaSignInException::class)
    private suspend fun signIn() : HatenaClient {
        accountLoader.signInHatenaAsync(reSignIn = false).await()
        return accountLoader.client
    }
}
