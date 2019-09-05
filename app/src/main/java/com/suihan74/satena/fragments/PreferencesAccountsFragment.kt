package com.suihan74.satena.fragments

import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import com.suihan74.HatenaLib.HatenaClient
import com.suihan74.utilities.MastodonClientHolder
import com.suihan74.satena.activities.PreferencesActivity
import com.suihan74.satena.R

class PreferencesAccountsFragment : Fragment() {

    companion object {
        fun createInstance() = PreferencesAccountsFragment()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_preferences_accounts, container, false)

        val activity = activity as PreferencesActivity

        val hatenaSignInButton = view.findViewById<Button>(R.id.preferences_accounts_hatena_signin_button)
        hatenaSignInButton.setOnClickListener {
            activity.showFragment(HatenaAuthenticationFragment.createInstance(), null)
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
            activity.showFragment(MastodonAuthenticationFragment.createInstance(), null)
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
