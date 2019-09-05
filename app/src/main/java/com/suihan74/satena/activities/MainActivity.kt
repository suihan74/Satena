package com.suihan74.satena.activities

import android.os.Bundle
import android.content.Intent
import android.util.Log
import android.view.View
import android.widget.ProgressBar
import com.jakewharton.threetenabp.AndroidThreeTen
import com.suihan74.HatenaLib.Entry
import com.suihan74.utilities.*
import com.suihan74.satena.*
import com.suihan74.satena.fragments.EntriesFragment
import com.suihan74.satena.fragments.HatenaAuthenticationFragment
import com.suihan74.satena.fragments.MastodonAuthenticationFragment
import com.suihan74.satena.models.PreferenceKey
import kotlinx.coroutines.*
import java.util.*

class MainActivity : ActivityBase() {
    private var entriesShowed = true

    override val containerId = R.id.main_layout
    override fun getProgressBarId(): Int? = R.id.main_progress_bar
    override fun getProgressBackgroundId(): Int? = R.id.click_guard

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

        if (prefs.version == SafeSharedPreferences.versionDefault || !entriesShowed) {
            // 初回起動時にはログインフラグメントを選択

            entriesShowed = false

            // とにかくedit呼べばバージョン情報は書き込まれる
            // TODO: なんかそれっぽい初回書き込み用のものを用意した方がいいかも
            prefs.edit {}

            showFragment(HatenaAuthenticationFragment.createInstance())
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

    suspend fun refreshEntries(tabPosition: Int, offset: Int? = null) : List<Entry> {
        val f = currentFragment
        return if (f is EntriesFragment) {
            f.refreshEntriesAsync(tabPosition, offset).await()
        }
        else {
            emptyList()
        }
    }
}
