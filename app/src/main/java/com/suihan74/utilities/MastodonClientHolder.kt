package com.suihan74.utilities

import com.google.gson.Gson
import com.sys1yagi.mastodon4j.MastodonClient
import com.sys1yagi.mastodon4j.api.entity.Account
import com.sys1yagi.mastodon4j.extension.fromJson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object MastodonClientHolder {
    var client : MastodonClient? = null
        private set

    var account : Account? = null
        private set

    suspend fun signIn(c: MastodonClient) : Account = withContext(Dispatchers.Default) {
        signOut()

        client = c
        account = try {
            c.get("accounts/verify_credentials").fromJson(Gson(), Account::class.java)
        }
        catch (e: Throwable) {
            client = null
            throw RuntimeException("failed to sign-in mastodon", e)
        }

        return@withContext account!!
    }

    fun signOut() {
        client = null
        account = null
    }

    fun signedIn() : Boolean = client != null && account != null
}
