package com.suihan74.satena.scenes.authentication

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.browser.customtabs.CustomTabColorSchemeParams
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.suihan74.misskey.Misskey
import com.suihan74.misskey.api.auth.AppCredential
import com.suihan74.misskey.api.auth.GenerateSessionResponse
import com.suihan74.satena.R
import com.suihan74.satena.SatenaApplication
import com.suihan74.satena.databinding.ActivityMisskeyAuthenticationBinding
import com.suihan74.utilities.extensions.ContextExtensions.showToast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.chromium.customtabsclient.shared.CustomTabsHelper

class MisskeyAuthenticationActivity : AppCompatActivity() {

    private lateinit var binding : ActivityMisskeyAuthenticationBinding

    // ------ //

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMisskeyAuthenticationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.authButton.setOnClickListener {
            showProgressBar()
            lifecycleScope.launch(Dispatchers.Main) {
                runCatching {
                    startAuthorizeMisskey(binding.instanceName.text.toString())
                }.onFailure {
                    Log.e("FailedToSignIn", it.stackTraceToString())
                    showToast("インスタンスが見つかりません")
                    hideProgressBar()
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()

        val uri = intent.data
        if (Intent.ACTION_VIEW != intent.action || uri?.scheme != "satena-misskey") {
            hideProgressBar()
            return
        }

        lifecycleScope.launch {
            runCatching {
                val instance = uri.host!!
                continueAuthorizeMisskey(instance)
            }.onSuccess {
                Log.i("misskey", "authorization has been completed")
                appCredential = null
                session = null
                showToast(R.string.pref_account_misskey_msg_auth_succeeded)
            }.onFailure {
                Log.e("misskey", "authorization failure")
                Log.e("misskey", it.stackTraceToString())
                showToast(R.string.pref_account_misskey_msg_auth_failure)
            }
            hideProgressBar()
        }
    }

    // ------ //

    private var appCredential: AppCredential? = null
    private var session: GenerateSessionResponse? = null

    private suspend fun startAuthorizeMisskey(instance: String) = withContext(Dispatchers.IO) {
        val appCredential = Misskey.auth.createApp(
            instance = instance,
            name = "Satena",
            description = "はてなブックマークの非公式アプリ",
            permissions = listOf("write:notes"),
            callbackUrl = "satena-misskey://$instance/callback"
        )
        val session = Misskey.auth.generateSession(appCredential)
        this@MisskeyAuthenticationActivity.appCredential = appCredential
        this@MisskeyAuthenticationActivity.session = session

        withContext(Dispatchers.Main) {
            val context = this@MisskeyAuthenticationActivity

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

            intent.launchUrl(context, Uri.parse(session.url))
        }
    }

    private suspend fun continueAuthorizeMisskey(instanceName: String) = withContext(Dispatchers.IO) {
        val appCredential = this@MisskeyAuthenticationActivity.appCredential
        val session = this@MisskeyAuthenticationActivity.session
        if (appCredential == null || session == null) {
            showToast(R.string.pref_account_misskey_msg_auth_failure)
            return@withContext
        }
        val accessToken = Misskey.auth.getAccessToken(appCredential, session)

        // make a MisskeyClient
        // persist AccessToken
        SatenaApplication.instance.accountLoader
            .signInMisskey(instanceName, accessToken)
    }

    override fun onNewIntent(intent: Intent?) {
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
