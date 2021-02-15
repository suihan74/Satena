package com.suihan74.utilities

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.suihan74.hatenaLib.Account
import com.suihan74.hatenaLib.HatenaClient
import com.suihan74.satena.models.PreferenceKey
import com.sys1yagi.mastodon4j.MastodonClient
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import okhttp3.OkHttpClient

class AccountLoader(
    private val context: Context,
    val client: HatenaClient,
    val mastodonClientHolder: MastodonClientHolder
) {
    class HatenaSignInException(message: String? = null) : Throwable(message)
    class MastodonSignInException(message: String? = null) : Throwable(message)

    private val hatenaMutex by lazy { Mutex() }
    private val mastodonMutex by lazy { Mutex() }

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
                    return@async result.getOrNull()
                }
            }

            // ID・パスワードを使用して再ログイン
            val userNameEncryptedStr = prefs.getString(PreferenceKey.HATENA_USER_NAME)
            val userPasswordEncryptedStr = prefs.getString(PreferenceKey.HATENA_PASSWORD)
            if (userNameEncryptedStr?.isNotEmpty() == true && userPasswordEncryptedStr?.isNotEmpty() == true) {
                try {
                    val userNameEncryptedData = serializer.deserialize(userNameEncryptedStr)
                    val name = CryptUtility.decrypt(userNameEncryptedData, key)

                    val passwordEncryptedData = serializer.deserialize(userPasswordEncryptedStr)
                    val password = CryptUtility.decrypt(passwordEncryptedData, key)

                    val account = client.signInAsync(name, password).await()

                    client.rkStr?.let { rk ->
                        saveHatenaCookie(rk)
                    }

                    return@async account
                }
                catch (e: Throwable) {
                    Log.d("HatenaLogin", Log.getStackTraceString(e))
                    throw HatenaSignInException(e.message)
                }
            }
            else {
                return@async null
            }
        }
    }

    fun signInMastodonAsync(reSignIn: Boolean = true) = GlobalScope.async(Dispatchers.Default + SupervisorJob()) {
        mastodonMutex.withLock {
            if (mastodonClientHolder.signedIn() && !reSignIn) {
                return@async mastodonClientHolder.account
            }

            val prefs = SafeSharedPreferences.create<PreferenceKey>(context)
            val mastodonAccessTokenEncryptedStr =
                prefs.get<String>(PreferenceKey.MASTODON_ACCESS_TOKEN)

            // Mastodonログイン
            if (mastodonAccessTokenEncryptedStr.isNotEmpty()) {
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

                    mastodonClientHolder.signInAsync(client).await()
                }
                catch (e: Throwable) {
                    Log.d("MastodonLogin", Log.getStackTraceString(e))
                    throw MastodonSignInException(e.message)
                }
            }
            else {
                return@async null
            }
        }
    }

    fun saveHatenaAccount(name: String, password: String, rk: String) {
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

    fun saveHatenaCookie(rk: String) {
        val serializer = ObjectSerializer<CryptUtility.EncryptedData>()
        val key = createKey(context)
        val encryptedRk = CryptUtility.encrypt(rk, key)
        val prefs = SafeSharedPreferences.create<PreferenceKey>(context)
        prefs.edit {
            putString(PreferenceKey.HATENA_RK, serializer.serialize(encryptedRk))
        }
    }

    fun saveMastodonAccount(instanceName: String, accessToken: String) {
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

    fun deleteHatenaAccount() {
        lock (client) {
            val prefs = SafeSharedPreferences.create<PreferenceKey>(context)
            prefs.edit {
                remove(PreferenceKey.HATENA_USER_NAME)
                remove(PreferenceKey.HATENA_PASSWORD)
                remove(PreferenceKey.HATENA_RK)
            }
            client.signOut()
        }
    }

    fun deleteMastodonAccount() {
        lock (MastodonClientHolder) {
            val prefs = SafeSharedPreferences.create<PreferenceKey>(context)
            prefs.edit {
                remove(PreferenceKey.MASTODON_ACCESS_TOKEN)
            }
            mastodonClientHolder.signOut()
        }
    }

    private fun createKey(context: Context) : String {
        val prefs = SafeSharedPreferences.create<PreferenceKey>(context)
        val uuid = prefs.get<String>(PreferenceKey.ID)
        val path = context.filesDir.absolutePath
        return "$uuid@$path"
    }
}
