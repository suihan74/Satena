package com.suihan74.satena.scenes.splash

import android.content.Context
import android.content.Intent
import com.suihan74.satena.SatenaApplication
import com.suihan74.satena.scenes.authentication.HatenaAuthenticationActivity2
import com.suihan74.satena.scenes.entries2.EntriesActivity
import com.suihan74.utilities.AccountLoader
import com.suihan74.utilities.OnError

class Repository(
    private val accountLoader : AccountLoader
) {
    /** サインイン */
    private suspend fun signIn() {
        accountLoader.signInAccounts(reSignIn = false)
    }

    /** サインイン処理を行うか */
    val needToLoad : Boolean
        = !SatenaApplication.instance.isFirstLaunch && !accountLoader.client.signedIn()

    /** 起動時の状態によって適切な処理の後画面遷移する */
    suspend fun createIntent(context: Context, onError: OnError? = null) : Intent {
        val app = SatenaApplication.instance
        return if (app.isFirstLaunch) {
            // 初回起動時
            app.isFirstLaunch = false
            Intent(context, HatenaAuthenticationActivity2::class.java)
        }
        else {
            // サインインしてエントリ画面を開く
            val result = runCatching {
                signIn()
            }

            result.exceptionOrNull()?.let { e ->
                onError?.invoke(e)
            }

            Intent(context, EntriesActivity::class.java)
        }
    }
}
