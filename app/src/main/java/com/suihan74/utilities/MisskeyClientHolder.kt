package com.suihan74.utilities

import com.suihan74.misskey.AuthorizedMisskeyClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object MisskeyClientHolder {
    var client : AuthorizedMisskeyClient? = null
        private set

    var account : MisskeyAccount? = null
        private set

    suspend fun signIn(c: AuthorizedMisskeyClient) : MisskeyAccount = withContext(Dispatchers.Default) {
        signOut()

        client = c
        account = runCatching {
            c.account.i()
        }.onFailure {
            client = null
            throw RuntimeException("failed to sign-in misskey", it)
        }.getOrNull()

        return@withContext account!!
    }

    fun signOut() {
        client = null
        account = null
    }

    fun signedIn() : Boolean = client != null && account != null
}
