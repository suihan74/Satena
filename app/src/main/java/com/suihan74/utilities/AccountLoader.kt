package com.suihan74.utilities

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.suihan74.HatenaLib.HatenaClient
import com.suihan74.satena.models.PreferenceKey
import com.sys1yagi.mastodon4j.MastodonClient
import kotlinx.coroutines.*
import okhttp3.OkHttpClient

object AccountLoader {
    suspend fun signInAccounts(context: Context, reSignIn: Boolean = false) = withContext(Dispatchers.IO + SupervisorJob()) {
        lock(this) {
            runBlocking {
                try {
                    val jobs = listOf(
                        signInHatenaAsync(context, reSignIn),
                        signInMastodonAsync(context, reSignIn)
                    )
                    jobs.awaitAll()
                } catch (e: Exception) {
                    throw RuntimeException("failed to sign in")
                }
            }
        }
    }

    fun signInHatenaAsync(context: Context, reSignIn: Boolean = true) = GlobalScope.async(Dispatchers.Default + SupervisorJob()) {
        if (HatenaClient.signedIn() && !reSignIn) return@async HatenaClient.account

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

                HatenaClient.signInAsync(name, password).await()
            } catch (e: Exception) {
                Log.d("HatenaLogin", Log.getStackTraceString(e))
                throw e
            }
        } else {
            return@async null
        }
    }

    fun signInMastodonAsync(context: Context, reSignIn: Boolean = true) = GlobalScope.async(Dispatchers.Default + SupervisorJob()) {
        if (MastodonClientHolder.signedIn() && !reSignIn) return@async MastodonClientHolder.account

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

                MastodonClientHolder.signInAsync(client).await()
            } catch (e: Exception) {
                Log.d("MastodonLogin", Log.getStackTraceString(e))
                throw e
            }
        } else {
            return@async null
        }
    }

    fun saveHatenaAccount(context: Context, name: String, password: String) {
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

    fun saveMastodonAccount(context: Context, instanceName: String, accessToken: String) {
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

    private fun createKey(context: Context) : String {
        val prefs = SafeSharedPreferences.create<PreferenceKey>(context)
        val uuid = prefs.get<String>(PreferenceKey.ID)
        val path = context.filesDir.absolutePath
        return "$uuid@$path"
    }
}
