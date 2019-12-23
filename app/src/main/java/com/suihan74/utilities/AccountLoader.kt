package com.suihan74.utilities

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.suihan74.HatenaLib.HatenaClient
import com.suihan74.satena.models.PreferenceKey
import com.sys1yagi.mastodon4j.MastodonClient
import kotlinx.coroutines.*
import okhttp3.OkHttpClient

class AccountLoader(
    private val context: Context,
    private val client: HatenaClient,
    private val mastodonClientHolder: MastodonClientHolder
) {
    class HatenaSignInException(message: String? = null) : Exception(message)
    class MastodonSignInException(message: String? = null) : Exception(message)

    suspend fun signInAccounts(reSignIn: Boolean = false) {
        val jobs = listOf(
            signInHatenaAsync(reSignIn),
            signInMastodonAsync(reSignIn)
        )
        jobs.awaitAll()
    }

    fun signInHatenaAsync(reSignIn: Boolean = true) = GlobalScope.async(Dispatchers.Default + SupervisorJob()) {
        if (client.signedIn() && !reSignIn) return@async client.account

        val prefs = SafeSharedPreferences.create<PreferenceKey>(context)
        val userNameEncryptedStr = prefs.getString(PreferenceKey.HATENA_USER_NAME)
        val userPasswordEncryptedStr = prefs.getString(PreferenceKey.HATENA_PASSWORD)

        // Hatenaログイン
        if (userNameEncryptedStr?.isNotEmpty() == true && userPasswordEncryptedStr?.isNotEmpty() == true) {
            val serializer = ObjectSerializer<CryptUtility.EncryptedData>()
            try {
                val key = createKey(context)

                val userNameEncryptedData = serializer.deserialize(userNameEncryptedStr)
                val name = CryptUtility.decrypt(userNameEncryptedData, key)

                val passwordEncryptedData = serializer.deserialize(userPasswordEncryptedStr)
                val password = CryptUtility.decrypt(passwordEncryptedData, key)

                client.signInAsync(name, password).await()
            }
            catch (e: Exception) {
                Log.d("HatenaLogin", Log.getStackTraceString(e))
                throw HatenaSignInException(e.message)
            }
        } else {
            return@async null
        }
    }

    fun signInMastodonAsync(reSignIn: Boolean = true) = GlobalScope.async(Dispatchers.Default + SupervisorJob()) {
        if (mastodonClientHolder.signedIn() && !reSignIn) return@async mastodonClientHolder.account

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
            catch (e: Exception) {
                Log.d("MastodonLogin", Log.getStackTraceString(e))
                throw MastodonSignInException(e.message)
            }
        } else {
            return@async null
        }
    }

    fun saveHatenaAccount(name: String, password: String) {
        val serializer = ObjectSerializer<CryptUtility.EncryptedData>()
        val key = createKey(context)
        val encryptedName = CryptUtility.encrypt(name, key)
        val encryptedPassword = CryptUtility.encrypt(password, key)

        val prefs = SafeSharedPreferences.create<PreferenceKey>(context)
        prefs.edit {
            putString(PreferenceKey.HATENA_USER_NAME, serializer.serialize(encryptedName))
            putString(PreferenceKey.HATENA_PASSWORD, serializer.serialize(encryptedPassword))
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
