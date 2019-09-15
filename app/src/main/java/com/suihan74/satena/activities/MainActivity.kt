package com.suihan74.satena.activities

import android.content.Intent
import android.os.Bundle
import android.util.Log
import com.suihan74.HatenaLib.HatenaClient
import com.suihan74.satena.R
import com.suihan74.satena.SatenaApplication
import com.suihan74.satena.fragments.EntriesFragment
import com.suihan74.satena.fragments.HatenaAuthenticationFragment
import com.suihan74.satena.fragments.MastodonAuthenticationFragment
import com.suihan74.satena.models.PreferenceKey
import com.suihan74.utilities.AccountLoader
import com.suihan74.utilities.MastodonClientHolder
import com.suihan74.utilities.SafeSharedPreferences
import com.suihan74.utilities.showToast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : ActivityBase() {
    private var entriesShowed = true

    override val containerId = R.id.main_layout
    override val progressBarId = R.id.main_progress_bar
    override val progressBackgroundId = R.id.click_guard

    override fun onSaveInstanceState(outState: Bundle?) {
        super.onSaveInstanceState(outState)
        outState?.apply {
            putBoolean("entries_showed", entriesShowed)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 設定ロード
        val prefs = SafeSharedPreferences.create<PreferenceKey>(applicationContext)

        // テーマの設定
        val isThemeDark = prefs.getBoolean(PreferenceKey.DARK_THEME)
        if (isThemeDark) {
            setTheme(R.style.AppTheme_Dark)
        }
        else {
            setTheme(R.style.AppTheme_Light)
        }
        setContentView(R.layout.activity_main)

        if (savedInstanceState != null) {
            entriesShowed = savedInstanceState.getBoolean("entries_showed")
        }

        if ((SatenaApplication.instance.isFirstLaunch && !HatenaClient.signedIn()) || !entriesShowed) {
            // 初回起動時にはログインフラグメントを選択
            entriesShowed = false
            showFragment(HatenaAuthenticationFragment.createInstance())
            SatenaApplication.instance.isFirstLaunch = false
        }
        else if (!isFragmentShowed()) {
            entriesShowed = true

            showProgressBar()
            launch(Dispatchers.Main) {
                try {
                    AccountLoader.signInAccounts(applicationContext)
                }
                catch (e: Exception) {
                    showToast("アカウント認証失敗")
                    Log.e("FailedToAuth", Log.getStackTraceString(e))
                }
                finally {
                    showFragment(EntriesFragment.createInstance())
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()

        // Mastodon認証
        when (intent.action) {
            Intent.ACTION_VIEW -> {
                val uri = intent.data
                if (uri?.scheme == "satena-mastodon" && !MastodonClientHolder.signedIn()) {
                    launch(Dispatchers.Main) {
                        try {
                            val instance = uri.host

                            val authFragment =
                                MastodonAuthenticationFragment.createInstance()
                            val authCode = uri.getQueryParameter("code") ?: throw Exception("invalid code")
                            authFragment.continueAuthorizeMastodonAsync(authCode, applicationContext).await()
                            showToast("$instance にログイン完了")
                        }
                        catch (e: Exception) {
                            Log.d("FailedToSignIn", e.message)
                            showToast("ログイン失敗")
                        }
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        setIntent(intent)
    }
}
