package com.suihan74.satena.scenes.preferences.ignored

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.suihan74.hatenaLib.FetchIgnoredUsersFailureException
import com.suihan74.hatenaLib.HatenaClient
import com.suihan74.utilities.AccountLoader
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

    override suspend fun loadIgnoredUsers() = withContext(Dispatchers.IO) {
        val result = runCatching {
            val client = signIn()
            client.getIgnoredUsersAsync().await()
        }

        if (result.isSuccess) {
            _ignoredUsersCache.clear()
            _ignoredUsersCache.addAll(result.getOrDefault(emptyList()))
            _ignoredUsers.postValue(_ignoredUsersCache)
        }
    }

    @Throws(FetchIgnoredUsersFailureException::class)
    override suspend fun ignoreUser(user: String) = withContext(Dispatchers.IO) {
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
    override suspend fun unIgnoreUser(user: String) = withContext(Dispatchers.IO) {
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

    /** 必要ならサインインし直す */
    @Throws(AccountLoader.HatenaSignInException::class)
    private suspend fun signIn() : HatenaClient {
        accountLoader.signInHatenaAsync(reSignIn = false).await()
        return accountLoader.client
    }
}
