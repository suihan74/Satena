package com.suihan74.satena.scenes.preferences.pages

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.suihan74.HatenaLib.HatenaClient
import com.suihan74.satena.R
import com.suihan74.satena.scenes.authentication.HatenaAuthenticationActivity
import com.suihan74.satena.scenes.authentication.MastodonAuthenticationActivity
import com.suihan74.utilities.MastodonClientHolder

class PreferencesAccountsFragment : Fragment() {
    private lateinit var mRoot: View

    companion object {
        fun createInstance() =
            PreferencesAccountsFragment()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_preferences_accounts, container, false)
        mRoot = view

        val hatenaSignInButton = view.findViewById<Button>(R.id.preferences_accounts_hatena_signin_button)
        hatenaSignInButton.setOnClickListener {
            val intent = Intent(context, HatenaAuthenticationActivity::class.java)
            startActivity(intent)
        }

        val mastodonSignInButton = view.findViewById<Button>(R.id.preferences_accounts_mastodon_signin_button)
        mastodonSignInButton.setOnClickListener {
            val intent = Intent(context, MastodonAuthenticationActivity::class.java)
            startActivity(intent)
        }

        return view
    }

    override fun onResume() {
        super.onResume()

        mRoot.findViewById<TextView>(R.id.preferences_accounts_hatena_name).apply {
            if (HatenaClient.signedIn()) {
                text = HatenaClient.account!!.name
                visibility = View.VISIBLE
            }
            else {
                visibility = View.GONE
            }
        }

        mRoot.findViewById<TextView>(R.id.preferences_accounts_mastodon_name).apply {
            if (MastodonClientHolder.signedIn()) {
                text = String.format(
                    "%s@%s",
                    MastodonClientHolder.account!!.userName,
                    MastodonClientHolder.client!!.getInstanceName()
                )
                visibility = View.VISIBLE
            }
            else {
                visibility = View.GONE
            }
        }
    }
}
