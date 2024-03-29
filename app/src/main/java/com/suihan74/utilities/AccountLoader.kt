package com.suihan74.utilities

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.suihan74.hatenaLib.Account
import com.suihan74.hatenaLib.HatenaClient
import com.suihan74.misskey.Misskey
import com.suihan74.satena.models.PreferenceKey
import com.suihan74.utilities.exceptions.TaskFailureException
import com.sys1yagi.mastodon4j.MastodonClient
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import okhttp3.OkHttpClient

typealias MastodonAccount = com.sys1yagi.mastodon4j.api.entity.Account
typealias MisskeyAccount = com.suihan74.misskey.entity.Account

class AccountLoader(
    private val context: Context,
    val client: HatenaClient,
    val mastodonClientHolder: MastodonClientHolder,
    val misskeyClientHolder: MisskeyClientHolder
) {
    class HatenaSignInException(message: String? = null) : Throwable(message)
    class MastodonSignInException(message: String? = null) : Throwable(message)
    class MisskeySignInException(message: String? = null) : Throwable(message)

    // ------ //

    private val hatenaMutex by lazy { Mutex() }
    private val mastodonMutex by lazy { Mutex() }
    private val misskeyMutex by lazy { Mutex() }

    // ------ //

    private val sharedHatenaFlow = MutableStateFlow<Account?>(null)
    val hatenaFlow : StateFlow<Account?> = sharedHatenaFlow

    private val sharedMastodonFlow = MutableStateFlow<MastodonAccount?>(null)
    val mastodonFlow : StateFlow<MastodonAccount?> = sharedMastodonFlow

    private val sharedMisskeyFlow = MutableStateFlow<MisskeyAccount?>(null)
    val misskeyFlow : StateFlow<MisskeyAccount?> = sharedMisskeyFlow

    // ------ //

    suspend fun signInAccounts(reSignIn: Boolean = false) = coroutineScope {
        val jobs = listOf(
            async { signInHatena(reSignIn) },
            async { signInMastodon(reSignIn) },
            async { signInMisskey(reSignIn) }
        )
        jobs.awaitAll()
    }

    suspend fun signInHatena(reSignIn: Boolean = true) : Account? {
        hatenaMutex.withLock {
            if (client.signedIn() && !reSignIn) {
                sharedHatenaFlow.emit(client.account)
                return client.account
            }

            val prefs = SafeSharedPreferences.create<PreferenceKey>(context)
            val serializer = ObjectSerializer<CryptUtility.EncryptedData>()
            val key = createKey(context)

            // クッキーを使用してログイン状態復元を試行
            val userRkEncryptedStr = prefs.getString(PreferenceKey.HATENA_RK)
            if (userRkEncryptedStr.isNullOrEmpty()) {
                sharedHatenaFlow.emit(null)
                return null
            }
            else {
                val result = runCatching {
                    val userRkEncryptedData = serializer.deserialize(userRkEncryptedStr)
                    val rk = CryptUtility.decrypt(userRkEncryptedData, key)
                    client.signIn(rk)
                }.onFailure {
                    Log.d("HatenaLoginWithCookie", Log.getStackTraceString(it))
                    sharedHatenaFlow.emit(null)
                    throw HatenaSignInException(it.message)
                }

                val account = result.getOrNull()
                sharedHatenaFlow.emit(account)
                return result.getOrNull()
            }
        }
    }

    suspend fun signInMastodon(reSignIn: Boolean = true) : com.sys1yagi.mastodon4j.api.entity.Account? {
        mastodonMutex.withLock {
            if (mastodonClientHolder.signedIn() && !reSignIn) {
                sharedMastodonFlow.emit(mastodonClientHolder.account)
                return mastodonClientHolder.account
            }

            val prefs = SafeSharedPreferences.create<PreferenceKey>(context)
            val mastodonAccessTokenEncryptedStr =
                prefs.get<String>(PreferenceKey.MASTODON_ACCESS_TOKEN)

            if (mastodonAccessTokenEncryptedStr.isEmpty()) {
                sharedMastodonFlow.emit(null)
                return null
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

                val account = mastodonClientHolder.signIn(client)

                sharedMastodonFlow.emit(account)

                return account
            }
            catch (e: Throwable) {
                sharedMastodonFlow.emit(null)
                Log.d("MastodonLogin", Log.getStackTraceString(e))
                throw MastodonSignInException(e.message)
            }
        }
    }

    suspend fun signInMisskey(reSignIn: Boolean = true) : MisskeyAccount? {
        misskeyMutex.withLock {
            if (misskeyClientHolder.signedIn() && !reSignIn) {
                sharedMisskeyFlow.emit(misskeyClientHolder.account)
                return misskeyClientHolder.account
            }

            val prefs = SafeSharedPreferences.create<PreferenceKey>(context)
            val misskeyAccessTokenEncryptedStr =
                prefs.get<String>(PreferenceKey.MISSKEY_ACCESS_TOKEN)

            if (misskeyAccessTokenEncryptedStr.isEmpty()) {
                sharedMisskeyFlow.emit(null)
                return null
            }
            // Misskeyログイン
            val serializer = ObjectSerializer<CryptUtility.EncryptedData>()
            val dataSerializer = ObjectSerializer<SerializableMastodonAccessToken>()
            try {
                val key = createKey(context)
                val misskeyAccessTokenEncryptedData = serializer.deserialize(misskeyAccessTokenEncryptedStr)
                val decrypted = CryptUtility.decrypt(misskeyAccessTokenEncryptedData, key)
                val data = dataSerializer.deserialize(decrypted)

                val client = Misskey.Client(
                    instance = data.instanceName,
                    tokenDigest = data.accessToken
                )
                val account = misskeyClientHolder.signIn(client)
                sharedMisskeyFlow.emit(account)

                return account
            }
            catch (e: Throwable) {
                sharedMisskeyFlow.emit(null)
                Log.d("MisskeyLogin", Log.getStackTraceString(e))
                throw MisskeySignInException(e.message)
            }
        }
    }

    // ------ //

    suspend fun signInHatena(rk: String) : Account {
        hatenaMutex.withLock {
            try {
                val account = client.signIn(rk)
                sharedHatenaFlow.emit(account)
                saveHatenaCookie(rk)

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
                val account = MastodonClientHolder.signIn(
                    MastodonClient
                        .Builder(instanceName, OkHttpClient.Builder(), Gson())
                        .accessToken(accessToken)
                        .build()
                )
                sharedMastodonFlow.emit(account)
                saveMastodonAccount(instanceName, accessToken)

                return account
            }
            catch (e: Throwable) {
                throw TaskFailureException(cause = e)
            }
        }
    }

    suspend fun signInMisskey(instanceName: String, accessToken: String) : MisskeyAccount {
        misskeyMutex.withLock {
            try {
                val account = MisskeyClientHolder.signIn(
                    Misskey.Client(
                        instance = instanceName,
                        tokenDigest = accessToken
                    )
                )
                sharedMisskeyFlow.emit(account)
                saveMisskeyAccount(instanceName, accessToken)

                return account
            }
            catch (e: Throwable) {
                throw TaskFailureException(cause = e)
            }
        }
    }

    // ------ //

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

    private fun saveMisskeyAccount(instanceName: String, accessToken: String) {
        val key = createKey(context)
        val data = SerializableMastodonAccessToken(instanceName, accessToken)

        val dataSerializer = ObjectSerializer<SerializableMastodonAccessToken>()
        val serializer = ObjectSerializer<CryptUtility.EncryptedData>()

        val encryptedData = CryptUtility.encrypt(dataSerializer.serialize(data), key)

        val prefs = SafeSharedPreferences.create<PreferenceKey>(context)
        prefs.edit {
            putString(PreferenceKey.MISSKEY_ACCESS_TOKEN, serializer.serialize(encryptedData))
        }
    }

    // ------ //

    suspend fun deleteHatenaAccount() {
        hatenaMutex.withLock {
            sharedHatenaFlow.emit(null)
            val prefs = SafeSharedPreferences.create<PreferenceKey>(context)
            prefs.edit {
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

    suspend fun deleteMisskeyAccount() {
        misskeyMutex.withLock {
            sharedMisskeyFlow.emit(null)
            val prefs = SafeSharedPreferences.create<PreferenceKey>(context)
            prefs.edit {
                remove(PreferenceKey.MISSKEY_ACCESS_TOKEN)
            }
            misskeyClientHolder.signOut()
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
