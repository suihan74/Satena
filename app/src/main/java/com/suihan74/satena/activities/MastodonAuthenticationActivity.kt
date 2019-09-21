package com.suihan74.satena.activities

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import com.google.gson.Gson
import com.suihan74.satena.R
import com.suihan74.utilities.AccountLoader
import com.suihan74.utilities.MastodonClientHolder
import com.suihan74.utilities.showToast
import com.sys1yagi.mastodon4j.MastodonClient
import com.sys1yagi.mastodon4j.api.Scope
import com.sys1yagi.mastodon4j.api.entity.auth.AppRegistration
import com.sys1yagi.mastodon4j.api.method.Apps
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient

class MastodonAuthenticationActivity : ActivityBase() {
    override val progressBackgroundId: Int? = R.id.click_guard
    override val progressBarId: Int? = R.id.progress_bar

    companion object {
        private var mAppRegistration : AppRegistration? = null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_mastodon_authentication)

        val authButton = findViewById<Button>(R.id.auth_button)
        val instanceName = findViewById<EditText>(R.id.instance_name)

        authButton.setOnClickListener {
            showProgressBar()
            launch(Dispatchers.Main) {
                try {
                    startAuthorizeMastodon(instanceName.text.toString())
                }
                catch (e: Exception) {
                    Log.e("FailedToSignIn", e.message)
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
                        continueAuthorizeMastodon(instance, authCode, this@MastodonAuthenticationActivity)
                        showToast("$instance にログイン完了")

                        // 前の画面に戻る
                        onBackPressed()
                    }
                    catch (e: Exception) {
                        Log.e("FailedToSignIn", e.message)
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
        MastodonClientHolder.signOut()

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
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            startActivity(intent)
        }
    }

    private suspend fun continueAuthorizeMastodon(instanceName: String, code: String, context: Context) = withContext(Dispatchers.IO) {
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
        MastodonClientHolder.signInAsync(
            MastodonClient
            .Builder(instanceName, OkHttpClient.Builder(), Gson())
            .accessToken(accessToken.accessToken)
            .build()
        ).await()

        // persist AccessToken
        AccountLoader.saveMastodonAccount(context, instanceName, accessToken.accessToken)
    }
}
