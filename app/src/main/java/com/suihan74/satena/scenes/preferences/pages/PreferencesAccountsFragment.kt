package com.suihan74.satena.scenes.preferences.pages

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.suihan74.hatenaLib.HatenaClient
import com.suihan74.satena.databinding.FragmentPreferencesAccountsBinding
import com.suihan74.satena.scenes.authentication.HatenaAuthenticationActivity
import com.suihan74.satena.scenes.authentication.MastodonAuthenticationActivity
import com.suihan74.satena.scenes.preferences.PreferencesFragmentBase
import com.suihan74.utilities.MastodonClientHolder

class PreferencesAccountsFragment : PreferencesFragmentBase() {
    companion object {
        fun createInstance() =
            PreferencesAccountsFragment()
    }

    // ------ //

    private var _binding : FragmentPreferencesAccountsBinding? = null
    private val binding
        get() = _binding!!

    // ------ //

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentPreferencesAccountsBinding.inflate(inflater, container, false)

        binding.preferencesAccountsHatenaSignInButton
            .setOnClickListener {
                val intent = Intent(context, HatenaAuthenticationActivity::class.java)
                startActivity(intent)
            }

        binding.preferencesAccountsMastodonSignInButton
            .setOnClickListener {
                val intent = Intent(context, MastodonAuthenticationActivity::class.java)
                startActivity(intent)
            }

        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onResume() {
        super.onResume()

        // TODO: サインイン状態でもアカウントが取得されない場合がある

        binding.preferencesAccountsHatenaName.apply {
            if (HatenaClient.signedIn()) {
                text = HatenaClient.account?.name ?: ""
                visibility = View.VISIBLE
            }
            else {
                visibility = View.INVISIBLE
            }
        }

        binding.preferencesAccountsMastodonName.apply {
            if (MastodonClientHolder.signedIn()) {
                val userName = MastodonClientHolder.account?.userName
                val instanceName = MastodonClientHolder.client?.getInstanceName()
                if (userName != null && instanceName != null) {
                    text = String.format("%s@%s", userName, instanceName)
                }
                visibility = View.VISIBLE
            }
            else {
                visibility = View.INVISIBLE
            }
        }
    }
}
