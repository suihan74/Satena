package com.suihan74.utilities

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.suihan74.hatenaLib.Account
import com.suihan74.hatenaLib.HatenaClient
import com.suihan74.satena.models.PreferenceKey
import com.suihan74.utilities.exceptions.TaskFailureException
import com.sys1yagi.mastodon4j.MastodonClient
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import okhttp3.OkHttpClient

typealias MastodonAccount = com.sys1yagi.mastodon4j.api.entity.Account

class AccountLoader(
    private val context: Context,
    val client: HatenaClient,
    val mastodonClientHolder: MastodonClientHolder
) {
    class HatenaSignInException(message: String? = null) : Throwable(message)
    class MastodonSignInException(message: String? = null) : Throwable(message)

    // ------ //

    private val hatenaMutex by lazy { Mutex() }
    private val mastodonMutex by lazy { Mutex() }

    // ------ //

    private val sharedHatenaFlow = MutableSharedFlow<Account?>()
    val hatenaFlow : SharedFlow<Account?> = sharedHatenaFlow.asSharedFlow()

    private val sharedMastodonFlow = MutableSharedFlow<MastodonAccount?>()
    val mastodonFlow : SharedFlow<MastodonAccount?> = sharedMastodonFlow.asSharedFlow()


    // ------ //

    suspend fun signInAccounts(reSignIn: Boolean = false) {
        val jobs = listOf(
            signInHatenaAsync(reSignIn),
            signInMastodonAsync(reSignIn)
        )
        jobs.awaitAll()
    }

    fun signInHatenaAsync(reSignIn: Boolean = true) : Deferred<Account?> = GlobalScope.async(Dispatchers.Default + SupervisorJob()) {
        hatenaMutex.withLock {
            if (client.signedIn() && !reSignIn) {
                sharedHatenaFlow.emit(client.account)
                return@async client.account
            }

            val prefs = SafeSharedPreferences.create<PreferenceKey>(context)
            val serializer = ObjectSerializer<CryptUtility.EncryptedData>()
            val key = createKey(context)

            // クッキーを使用してログイン状態復元を試行
            val userRkEncryptedStr = prefs.getString(PreferenceKey.HATENA_RK)
            if (userRkEncryptedStr.isNullOrEmpty().not()) {
                val result = runCatching {
                    val userRkEncryptedData = serializer.deserialize(userRkEncryptedStr!!)
                    val rk = CryptUtility.decrypt(userRkEncryptedData, key)
                    client.signIn(rk)
                }

                if (result.isFailure) {
                    Log.d(
                        "HatenaLoginWithCookie",
                        Log.getStackTraceString(result.exceptionOrNull())
                    )
                }
                else {
                    val account = result.getOrNull()
                    sharedHatenaFlow.emit(account)
                    return@async result.getOrNull()
                }
            }

            // ID・パスワードを使用して再ログイン
            val userNameEncryptedStr = prefs.getString(PreferenceKey.HATENA_USER_NAME)
            val userPasswordEncryptedStr = prefs.getString(PreferenceKey.HATENA_PASSWORD)
            if (userNameEncryptedStr?.isEmpty() != false || userPasswordEncryptedStr?.isEmpty() != false) {
                sharedHatenaFlow.emit(null)
                return@async null
            }

            try {
                val userNameEncryptedData = serializer.deserialize(userNameEncryptedStr)
                val name = CryptUtility.decrypt(userNameEncryptedData, key)

                val passwordEncryptedData = serializer.deserialize(userPasswordEncryptedStr)
                val password = CryptUtility.decrypt(passwordEncryptedData, key)

                val account = client.signInAsync(name, password).await()

                client.rkStr?.let { rk ->
                    saveHatenaCookie(rk)
                }

                sharedHatenaFlow.emit(account)

                return@async account
            }
            catch (e: Throwable) {
                sharedHatenaFlow.emit(null)
                Log.d("HatenaLogin", Log.getStackTraceString(e))
                throw HatenaSignInException(e.message)
            }
        }
    }

    fun signInMastodonAsync(reSignIn: Boolean = true) = GlobalScope.async(Dispatchers.Default + SupervisorJob()) {
        mastodonMutex.withLock {
            if (mastodonClientHolder.signedIn() && !reSignIn) {
                sharedMastodonFlow.emit(mastodonClientHolder.account)
                return@async mastodonClientHolder.account
            }

            val prefs = SafeSharedPreferences.create<PreferenceKey>(context)
            val mastodonAccessTokenEncryptedStr =
                prefs.get<String>(PreferenceKey.MASTODON_ACCESS_TOKEN)

            if (mastodonAccessTokenEncryptedStr.isEmpty()) {
                sharedMastodonFlow.emit(null)
                return@async null
            }
            // Mastodonログイン
            val serializer = ObjectSerializer<CryptUtility.EncryptedData>()
            val dataSerializer = ObjectSerializer<SerializableMastodonAccessToken>()
            try {
                val key = createKey(context)
                val mastodonAccessTokenEncryptedData =
                    serializer.deserialize(mastodonAccessTokenEncryptedStr)
                val decrypted = CryptUtility.decrypt(mastodonAccessTokenEncryptedData, key)
                val data = dataSerializer.deserialize(decrypted)

                val client = MastodonClient
                    .Builder(data.instanceName, OkHttpClient.Builder(), Gson())
                    .accessToken(data.accessToken)
                    .build()

                val account = mastodonClientHolder.signInAsync(client).await()

                sharedMastodonFlow.emit(account)

                return@async account
            }
            catch (e: Throwable) {
                sharedMastodonFlow.emit(null)
                Log.d("MastodonLogin", Log.getStackTraceString(e))
                throw MastodonSignInException(e.message)
            }
        }
    }

    // ------ //

    suspend fun signInHatena(name: String, password: String) : Account {
        hatenaMutex.withLock {
            try {
                val account = client.signInAsync(name, password).await()
                sharedHatenaFlow.emit(account)
                saveHatenaAccount(name, password, client.rkStr!!)

                return account
            }
            catch (e: Throwable) {
                throw TaskFailureException(cause = e)
            }
        }
    }

    suspend fun signInMastodon(instanceName: String, accessToken: String) : MastodonAccount {
        mastodonMutex.withLock {
            try {
                val account = MastodonClientHolder.signInAsync(
                    MastodonClient
                        .Builder(instanceName, OkHttpClient.Builder(), Gson())
                        .accessToken(accessToken)
                        .build()
                ).await()
                sharedMastodonFlow.emit(account)
                saveMastodonAccount(instanceName, accessToken)

                return account
            }
            catch (e: Throwable) {
                throw TaskFailureException(cause = e)
            }
        }
    }

    // ------ //

    private fun saveHatenaAccount(name: String, password: String, rk: String) {
        val serializer = ObjectSerializer<CryptUtility.EncryptedData>()
        val key = createKey(context)

        val encryptedName = CryptUtility.encrypt(name, key)
        val encryptedPassword = CryptUtility.encrypt(password, key)
        val encryptedRk = CryptUtility.encrypt(rk, key)

        val prefs = SafeSharedPreferences.create<PreferenceKey>(context)
        prefs.edit {
            putString(PreferenceKey.HATENA_USER_NAME, serializer.serialize(encryptedName))
            putString(PreferenceKey.HATENA_PASSWORD, serializer.serialize(encryptedPassword))
            putString(PreferenceKey.HATENA_RK, serializer.serialize(encryptedRk))
        }
    }

    private fun saveHatenaCookie(rk: String) {
        val serializer = ObjectSerializer<CryptUtility.EncryptedData>()
        val key = createKey(context)
        val encryptedRk = CryptUtility.encrypt(rk, key)
        val prefs = SafeSharedPreferences.create<PreferenceKey>(context)
        prefs.edit {
            putString(PreferenceKey.HATENA_RK, serializer.serialize(encryptedRk))
        }
    }

    private fun saveMastodonAccount(instanceName: String, accessToken: String) {
        val key = createKey(context)
        val data = SerializableMastodonAccessToken(instanceName, accessToken)

        val dataSerializer = ObjectSerializer<SerializableMastodonAccessToken>()
        val serializer = ObjectSerializer<CryptUtility.EncryptedData>()

        val encryptedData = CryptUtility.encrypt(dataSerializer.serialize(data), key)

        val prefs = SafeSharedPreferences.create<PreferenceKey>(context)
        prefs.edit {
            putString(PreferenceKey.MASTODON_ACCESS_TOKEN, serializer.serialize(encryptedData))
        }
    }

    // ------ //

    suspend fun deleteHatenaAccount() {
        hatenaMutex.withLock {
            sharedHatenaFlow.emit(null)
            val prefs = SafeSharedPreferences.create<PreferenceKey>(context)
            prefs.edit {
                remove(PreferenceKey.HATENA_USER_NAME)
                remove(PreferenceKey.HATENA_PASSWORD)
                remove(PreferenceKey.HATENA_RK)
            }
            client.signOut()
        }
    }

    suspend fun deleteMastodonAccount() {
        mastodonMutex.withLock {
            sharedMastodonFlow.emit(null)
            val prefs = SafeSharedPreferences.create<PreferenceKey>(context)
            prefs.edit {
                remove(PreferenceKey.MASTODON_ACCESS_TOKEN)
            }
            mastodonClientHolder.signOut()
        }
    }

    // ------ //

    private fun createKey(context: Context) : String {
        val prefs = SafeSharedPreferences.create<PreferenceKey>(context)
        val uuid = prefs.get<String>(PreferenceKey.ID)
        val path = context.filesDir.absolutePath
        return "$uuid@$path"
    }
}
