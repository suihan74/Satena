package com.suihan74.utilities

import android.util.Log
import com.google.gson.Gson
import com.sys1yagi.mastodon4j.MastodonClient
import com.sys1yagi.mastodon4j.api.entity.Account
import com.sys1yagi.mastodon4j.extension.fromJson
import kotlinx.coroutines.*
import java.lang.RuntimeException

object MastodonClientHolder {
    var client : MastodonClient? = null
        private set

    var account : Account? = null
        private set

    fun signInAsync(c: MastodonClient) : Deferred<Account> = GlobalScope.async {
        client = c
        try {
            account = c.get("accounts/verify_credentials").fromJson(Gson(), Account::class.java)
        }
        catch (e: Exception) {
            account = null
        }

        return@async account ?: throw RuntimeException("failed to sign-in mastodon")
    }

    fun signOut() {
        client = null
        account = null
    }

    fun signedIn() : Boolean = client != null && account != null
}
