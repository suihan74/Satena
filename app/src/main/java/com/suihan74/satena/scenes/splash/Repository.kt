package com.suihan74.satena.scenes.splash

import android.content.Context
import android.content.Intent
import androidx.core.app.ActivityOptionsCompat
import com.suihan74.hatenaLib.HatenaClient
import com.suihan74.satena.SatenaApplication
import com.suihan74.satena.scenes.authentication.HatenaAuthenticationActivity
import com.suihan74.satena.scenes.entries2.EntriesActivity
import com.suihan74.utilities.AccountLoader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

typealias OnError = (Throwable)->Unit

class Repository(
    private val context : Context,
    private val client : HatenaClient,
    private val accountLoader : AccountLoader
) {
    /** サインイン状態 */
    val signedIn : Boolean
        get() = client.signedIn()

    /** アプリバージョン */
    val appVersion : String by lazy {
        val pm = context.packageManager
        val packageInfo = pm.getPackageInfo(context.packageName, 0)
        packageInfo.versionName
    }

    /** サインイン */
    private suspend fun signIn(onError: OnError?) {
        try {
            accountLoader.signInAccounts(reSignIn = false)
        }
        catch (e : Throwable) {
            onError?.invoke(e)
        }
    }

    /** 起動時の状態によって適切な処理の後画面遷移する */
    suspend fun start(onError: OnError? = null) {
        if (SatenaApplication.instance.isFirstLaunch) {
            // 初回起動時
            SatenaApplication.instance.isFirstLaunch = false
            withContext(Dispatchers.Main) {
                startAuthenticationActivity()
            }
        }
        else {
            // サインインしてエントリ画面を開く
            signIn(onError)
            startEntriesActivity()
        }
    }

    /** エントリ画面に遷移 */
    private fun startEntriesActivity() {
        val intent = Intent(context, EntriesActivity::class.java)
        context.startActivity(
            intent,
            ActivityOptionsCompat.makeCustomAnimation(
                context,
                android.R.anim.fade_in, android.R.anim.fade_out
            ).toBundle())
    }

    /** 認証画面に遷移 */
    private fun startAuthenticationActivity() {
        val intent = Intent(context, HatenaAuthenticationActivity::class.java)
        context.startActivity(intent)
    }
}
