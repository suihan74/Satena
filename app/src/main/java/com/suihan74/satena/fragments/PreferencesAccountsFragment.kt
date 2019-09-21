package com.suihan74.satena.fragments

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
import com.suihan74.satena.activities.HatenaAuthenticationActivity
import com.suihan74.satena.activities.MastodonAuthenticationActivity
import com.suihan74.utilities.MastodonClientHolder

class PreferencesAccountsFragment : Fragment() {

    companion object {
        fun createInstance() = PreferencesAccountsFragment()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_preferences_accounts, container, false)

        val hatenaSignInButton = view.findViewById<Button>(R.id.preferences_accounts_hatena_signin_button)
        hatenaSignInButton.setOnClickListener {
            val intent = Intent(context, HatenaAuthenticationActivity::class.java)
            startActivity(intent)
        }

        val hatenaUserName = view.findViewById<TextView>(R.id.preferences_accounts_hatena_name)
        if (HatenaClient.signedIn()) {
            hatenaUserName.text = HatenaClient.account!!.name
            hatenaUserName.visibility = View.VISIBLE
        }
        else {
            hatenaUserName.visibility = View.GONE
        }

        val mastodonSignInButton = view.findViewById<Button>(R.id.preferences_accounts_mastodon_signin_button)
        mastodonSignInButton.setOnClickListener {
            val intent = Intent(context, MastodonAuthenticationActivity::class.java)
            startActivity(intent)
        }

        val mastodonUserName = view.findViewById<TextView>(R.id.preferences_accounts_mastodon_name)
        if (MastodonClientHolder.signedIn()) {
            mastodonUserName.text = String.format("%s@%s",
                MastodonClientHolder.account!!.userName,
                MastodonClientHolder.client!!.getInstanceName())
            mastodonUserName.visibility = View.VISIBLE
        }
        else {
            mastodonUserName.visibility = View.GONE
        }

        return view
    }
}
