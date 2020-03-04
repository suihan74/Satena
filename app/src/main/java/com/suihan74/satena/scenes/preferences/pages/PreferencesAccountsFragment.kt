package com.suihan74.satena.scenes.preferences.pages

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.suihan74.hatenaLib.HatenaClient
import com.suihan74.satena.R
import com.suihan74.satena.scenes.authentication.HatenaAuthenticationActivity
import com.suihan74.satena.scenes.authentication.MastodonAuthenticationActivity
import com.suihan74.satena.scenes.preferences.PreferencesFragmentBase
import com.suihan74.utilities.MastodonClientHolder
import kotlinx.android.synthetic.main.fragment_preferences_accounts.view.*

class PreferencesAccountsFragment : PreferencesFragmentBase() {
    companion object {
        fun createInstance() =
            PreferencesAccountsFragment()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_preferences_accounts, container, false)

        view.preferences_accounts_hatena_sign_in_button
            .setOnClickListener {
                val intent = Intent(context, HatenaAuthenticationActivity::class.java)
                startActivity(intent)
            }

        view.preferences_accounts_mastodon_sign_in_button
            .setOnClickListener {
                val intent = Intent(context, MastodonAuthenticationActivity::class.java)
                startActivity(intent)
            }

        return view
    }

    override fun onResume() {
        super.onResume()

        view?.preferences_accounts_hatena_name?.apply {
            if (HatenaClient.signedIn()) {
                text = HatenaClient.account!!.name
                visibility = View.VISIBLE
            }
            else {
                visibility = View.INVISIBLE
            }
        }

        view?.preferences_accounts_mastodon_name?.apply {
            if (MastodonClientHolder.signedIn()) {
                text = String.format(
                    "%s@%s",
                    MastodonClientHolder.account!!.userName,
                    MastodonClientHolder.client!!.getInstanceName()
                )
                visibility = View.VISIBLE
            }
            else {
                visibility = View.INVISIBLE
            }
        }
    }
}
