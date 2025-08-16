package com.suihan74.satena.scenes.authentication

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.WindowInsets
import androidx.appcompat.app.AppCompatActivity
import androidx.browser.customtabs.CustomTabColorSchemeParams
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.content.ContextCompat
import androidx.core.view.updatePadding
import androidx.lifecycle.lifecycleScope
import com.google.gson.Gson
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

class MastodonAuthenticationActivity : AppCompatActivity() {

    private lateinit var binding : ActivityMastodonAuthenticationBinding

    // ------ //

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
//        window.statusBarColor = getColor(R.color.mastodonBackground)
        binding = ActivityMastodonAuthenticationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // ステータスバーを避けるように配置する
        binding.mainLayout.setOnApplyWindowInsetsListener { view, insets ->
            binding.mainLayout.updatePadding(top =
                when {
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.R -> {
                        insets.getInsetsIgnoringVisibility(WindowInsets.Type.systemBars()).top
                    }
                    else -> {
                        insets.systemWindowInsetTop
                    }
                }
            )
            insets
        }

        binding.authButton.setOnClickListener {
            showProgressBar()
            lifecycleScope.launch(Dispatchers.Main) {
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
                lifecycleScope.launch(Dispatchers.Main) {
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

    // ------ //

    /** Mastodonインスタンスへのアプリの登録情報を端末に保存する */
    private fun writeAppRegistration(instance: String, appRegistration: AppRegistration) {
        val filename = "mstdn_app_reg_$instance"
        openFileOutput(filename, MODE_PRIVATE).use {
            val json = Gson().toJson(appRegistration)
            it.write(json.toByteArray())
        }
    }

    /** 既に端末に保存されているMastodonインスタンスへのアプリの登録情報を取得する */
    private fun readAppRegistration(instance: String) : AppRegistration? {
        val filename = "mstdn_app_reg_$instance"
        val result = runCatching {
            openFileInput(filename).bufferedReader().useLines { lines ->
                val json = lines.fold("") { some, text ->
                    "$some\n$text"
                }
                Gson().fromJson(json, AppRegistration::class.java)
            }
        }
        return result.getOrNull()
    }

    // ------ //

    private suspend fun startAuthorizeMastodon(instance: String) = withContext(Dispatchers.IO) {
        val client = MastodonClient.Builder(
            instance,
            OkHttpClient.Builder(),
            Gson()
        ).build()
        val apps = Apps(client)

        val appRegistration =
            readAppRegistration(instance)
                ?: apps.createApp(
                    "Satena for Android",
                    "satena-mastodon://$instance/callback",
                    Scope(Scope.Name.ALL),
                    getString(R.string.developer_website)
                ).execute()
                    .also {
                        writeAppRegistration(instance, it)
                    }

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

        val appRegistration = readAppRegistration(instanceName)!!

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

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
    }

    // ------ //

    private fun showProgressBar() {
        binding.clickGuard.visibility = View.VISIBLE
        binding.progressBar.visibility = View.VISIBLE
    }

    private fun hideProgressBar() {
        binding.clickGuard.visibility = View.GONE
        binding.progressBar.visibility = View.GONE
    }
}
