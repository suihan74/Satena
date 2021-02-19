package com.suihan74.satena.scenes.authentication

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.browser.customtabs.CustomTabColorSchemeParams
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.content.ContextCompat
import com.google.gson.Gson
import com.suihan74.satena.ActivityBase
import com.suihan74.satena.R
import com.suihan74.satena.SatenaApplication
import com.suihan74.satena.databinding.ActivityMastodonAuthenticationBinding
import com.suihan74.utilities.extensions.ContextExtensions.showToast
import com.sys1yagi.mastodon4j.MastodonClient
import com.sys1yagi.mastodon4j.api.Scope
import com.sys1yagi.mastodon4j.api.entity.auth.AppRegistration
import com.sys1yagi.mastodon4j.api.method.Apps
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import org.chromium.customtabsclient.shared.CustomTabsHelper

class MastodonAuthenticationActivity : ActivityBase() {
    override val progressBackgroundId = R.id.click_guard
    override val progressBarId = R.id.progress_bar

    companion object {
        private var mAppRegistration : AppRegistration? = null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val binding = ActivityMastodonAuthenticationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.authButton.setOnClickListener {
            showProgressBar()
            launch(Dispatchers.Main) {
                try {
                    startAuthorizeMastodon(binding.instanceName.text.toString())
                }
                catch (e: Throwable) {
                    Log.w("FailedToSignIn", e)
                    showToast("インスタンスが見つかりません")
                }
                finally {
                    hideProgressBar()
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()

        if (Intent.ACTION_VIEW == intent.action) {
            val uri = intent.data
            if (uri?.scheme == "satena-mastodon") {
                launch(Dispatchers.Main) {
                    showProgressBar()
                    try {
                        val instance = uri.host!!
                        val authCode = uri.getQueryParameter("code") ?: throw Exception("invalid code")
                        continueAuthorizeMastodon(instance, authCode)
                        showToast("$instance にログイン完了")

                        // 前の画面に戻る
                        finish()
                    }
                    catch (e: Throwable) {
                        Log.e("FailedToSignIn", e.message ?: "")
                        showToast("ログイン失敗")
                    }
                    finally {
                        hideProgressBar()
                    }
                }
            }
        }
    }

    private suspend fun startAuthorizeMastodon(instance: String) = withContext(Dispatchers.IO) {
        val client = MastodonClient.Builder(
            instance,
            OkHttpClient.Builder(),
            Gson()
        ).build()
        val apps = Apps(client)

        val appRegistration = apps.createApp(
            "Satena for Android",
            "satena-mastodon://$instance/callback",
            Scope(Scope.Name.ALL),
            "http://suihan74.orz.hm/blog/"
        ).execute()

        mAppRegistration = appRegistration

        val url = apps.getOAuthUrl(
            clientId = appRegistration.clientId,
            scope = Scope(Scope.Name.ALL),
            redirectUri = "satena-mastodon://$instance/callback"
        )

        withContext(Dispatchers.Main) {
            val context = this@MastodonAuthenticationActivity

            val intent = CustomTabsIntent.Builder()
                .setShowTitle(true)
                .setUrlBarHidingEnabled(true)
                .setDefaultColorSchemeParams(CustomTabColorSchemeParams.Builder()
                    .setToolbarColor(ContextCompat.getColor(context, R.color.colorPrimary))
                    .build()
                )
                .build()
                .apply {
                    val packageName = CustomTabsHelper.getPackageNameToUse(context)
                    intent.setPackage(packageName)
                }

            intent.launchUrl(context, Uri.parse(url))
        }
    }

    private suspend fun continueAuthorizeMastodon(instanceName: String, code: String) = withContext(Dispatchers.IO) {
        val client = MastodonClient.Builder(
            instanceName,
            OkHttpClient.Builder(),
            Gson()
        ).build()
        val apps = Apps(client)

        val appRegistration = mAppRegistration!!

        val clientId = appRegistration.clientId
        val clientSecret = appRegistration.clientSecret
        val redirectUri = appRegistration.redirectUri

        val accessToken = apps.getAccessToken(
            clientId,
            clientSecret,
            redirectUri,
            code,
            "authorization_code"
        ).execute()

        // make a MastodonClient
        // persist AccessToken
        SatenaApplication.instance.accountLoader
            .signInMastodon(instanceName, accessToken.accessToken)
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        setIntent(intent)
    }
}
