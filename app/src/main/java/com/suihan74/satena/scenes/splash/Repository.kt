package com.suihan74.satena.scenes.splash

import android.content.Context
import android.content.Intent
import com.suihan74.satena.SatenaApplication
import com.suihan74.satena.scenes.authentication.HatenaAuthenticationActivity
import com.suihan74.satena.scenes.entries2.EntriesActivity
import com.suihan74.utilities.AccountLoader
import com.suihan74.utilities.OnError
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class Repository(
    private val accountLoader : AccountLoader
) {
    /** サインイン */
    private suspend fun signIn(onError: OnError?) {
        try {
            accountLoader.signInAccounts(reSignIn = false)
        }
        catch (e : Throwable) {
            withContext(Dispatchers.Main) {
                onError?.invoke(e)
            }
        }
    }

    /** 起動時の状態によって適切な処理の後画面遷移する */
    suspend fun createIntent(context: Context, onError: OnError? = null) : Intent {
        val app = SatenaApplication.instance
        return if (app.isFirstLaunch) {
            // 初回起動時
            app.isFirstLaunch = false
            Intent(context, HatenaAuthenticationActivity::class.java).apply {
                putExtra(HatenaAuthenticationActivity.EXTRA_FIRST_LAUNCH, true)
            }
        }
        else {
            // サインインしてエントリ画面を開く
            signIn(onError)
            Intent(context, EntriesActivity::class.java)
        }
    }
}
