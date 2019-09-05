package com.suihan74.satena.fragments

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.support.v4.app.Fragment
import android.transition.AutoTransition
import android.transition.Slide
import android.transition.TransitionSet
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import com.google.gson.Gson
import com.suihan74.utilities.*
import com.suihan74.satena.R
import com.sys1yagi.mastodon4j.MastodonClient
import com.sys1yagi.mastodon4j.api.Scope
import com.sys1yagi.mastodon4j.api.entity.auth.AppRegistration
import com.sys1yagi.mastodon4j.api.method.Apps
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import okhttp3.OkHttpClient
import java.lang.RuntimeException

class MastodonAuthenticationFragment : CoroutineScopeFragment() {
    private lateinit var mView : View

    companion object {
        fun createInstance() : MastodonAuthenticationFragment {
            val f = MastodonAuthenticationFragment()

            f.enterTransition = TransitionSet().addTransition(Slide(Gravity.END))
            f.returnTransition = TransitionSet().addTransition(AutoTransition())

            return f
        }

        private var mInstanceName: String? = null
        private var mAppRegistration: AppRegistration? = null
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        mView = inflater.inflate(R.layout.fragment_mastodon_authentication, container, false)

        val authButton = mView.findViewById<Button>(R.id.auth_button)
        val cancelButton = mView.findViewById<Button>(R.id.cancel_button)
        val instanceName = mView.findViewById<EditText>(R.id.instance_name)

        authButton.setOnClickListener {
            mInstanceName = instanceName.text.toString()
            startAuthorizeMastodonAsync(mInstanceName!!).start()
        }

        cancelButton.setOnClickListener {
            (activity as FragmentContainerActivity).popFragment()
        }

        return mView
    }

    fun startAuthorizeMastodonAsync(instance: String) = async {
        try {
            MastodonClientHolder.signOut()

            val client = MastodonClient.Builder(
                instance,
                OkHttpClient.Builder(),
                Gson()
            ).build()
            val apps = Apps(client)

            mAppRegistration = apps.createApp(
                "Satena for Android",
                "satena-mastodon://$instance/callback",
                Scope(Scope.Name.ALL),
                "http://suihan74.orz.hm/blog/"
            ).execute()

            val url = apps.getOAuthUrl(
                clientId = mAppRegistration!!.clientId,
                scope = Scope(Scope.Name.ALL),
                redirectUri = "satena-mastodon://$instance/callback"
            )

            async(Dispatchers.Main) {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                startActivity(intent)
            }
        }
        catch (e: Exception) {
            Log.d("FailedToSignIn", e.message)
            activity!!.showToast("ログイン失敗")
        }
    }

    fun continueAuthorizeMastodonAsync(code: String, applicationContext: Context) = async {
        val client = MastodonClient.Builder(
            mInstanceName!!,
            OkHttpClient.Builder(),
            Gson()
        ).build()
        val apps = Apps(client)

        if (mAppRegistration == null)
        {
            throw RuntimeException("failed to get AppRegistration")
        }

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
            .Builder(mInstanceName!!, OkHttpClient.Builder(), Gson())
            .accessToken(accessToken.accessToken)
            .build()
        ).await()

        // persist AccessToken
        AccountLoader.saveMastodonAccount(applicationContext, mInstanceName!!, accessToken.accessToken)
    }
}
