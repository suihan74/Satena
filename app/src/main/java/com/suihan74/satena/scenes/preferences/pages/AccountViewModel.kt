package com.suihan74.satena.scenes.preferences.pages

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.TextView
import androidx.databinding.BindingAdapter
import androidx.databinding.ViewDataBinding
import androidx.fragment.app.Fragment
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.suihan74.hatenaLib.Account
import com.suihan74.satena.R
import com.suihan74.satena.databinding.ListviewItemPrefsSignInHatenaBinding
import com.suihan74.satena.databinding.ListviewItemPrefsSignInMastodonBinding
import com.suihan74.satena.scenes.authentication.HatenaAuthenticationActivity
import com.suihan74.satena.scenes.authentication.MastodonAuthenticationActivity
import com.suihan74.satena.scenes.preferences.PreferencesAdapter
import com.suihan74.satena.scenes.preferences.addButton
import com.suihan74.satena.scenes.preferences.addSection
import com.suihan74.utilities.AccountLoader
import com.suihan74.utilities.extensions.alsoAs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AccountViewModel(
    context: Context,
    private val accountLoader: AccountLoader
) : ListPreferencesViewModel(context) {

    val accountHatena = MutableLiveData<Account?>()

    val accountMastodon = MutableLiveData<com.sys1yagi.mastodon4j.api.entity.Account?>()

    // ------ //

    override fun onCreateView(fragment: ListPreferencesFragment) {
        super.onCreateView(fragment)

        viewModelScope.launch(Dispatchers.Main) {
            accountHatena.value = accountLoader.signInHatenaAsync(reSignIn = false).await()
            accountMastodon.value = accountLoader.signInMastodonAsync(reSignIn = false).await()
            load(fragment)
        }
    }

    @OptIn(ExperimentalStdlibApi::class)
    override fun createList(fragment: ListPreferencesFragment): List<PreferencesAdapter.Item> = buildList {
        val context = fragment.requireContext()

        addSection(R.string.pref_accounts_service_name_hatena)
        if (accountHatena.value == null) {
            addButton(fragment, R.string.sign_in) {
                openHatenaAuthenticationActivity(context)
            }
        }
        else {
            add(PrefItemHatenaAccount(fragment, this@AccountViewModel))
        }

        // --- //

        addSection(R.string.pref_accounts_service_name_mastodon)
        if (accountMastodon.value == null) {
            addButton(fragment, R.string.sign_in) {
                openMastodonAuthenticationActivity(context)
            }
        }
        else {
            add(PrefItemMastodonAccount(fragment, this@AccountViewModel))
        }
    }

    // ------ //

    private fun openHatenaAuthenticationActivity(context: Context) {
        val intent = Intent(context, HatenaAuthenticationActivity::class.java)
        context.startActivity(intent)
    }

    private fun openMastodonAuthenticationActivity(context: Context) {
        val intent = Intent(context, MastodonAuthenticationActivity::class.java)
        context.startActivity(intent)
    }

    // ------ //

    /**
     * サインイン済みのはてなアカウントボタン
     */
    class PrefItemHatenaAccount(
        private val fragment: Fragment,
        private val viewModel: AccountViewModel
    ) : PreferencesAdapter.Item {
        override val layoutId: Int = R.layout.listview_item_prefs_sign_in_hatena

        override fun bind(binding: ViewDataBinding) {
            binding.alsoAs<ListviewItemPrefsSignInHatenaBinding> {
                it.vm = viewModel
                it.root.setOnClickListener { v ->
                    viewModel.openHatenaAuthenticationActivity(v.context)
                }
            }
        }

        override fun areItemsTheSame(old: PreferencesAdapter.Item, new: PreferencesAdapter.Item) =
            old is PrefItemHatenaAccount && new is PrefItemHatenaAccount &&
                    old.viewModel == new.viewModel

        override fun areContentsTheSame(old: PreferencesAdapter.Item, new: PreferencesAdapter.Item) =
            old is PrefItemHatenaAccount && new is PrefItemHatenaAccount &&
                    old.fragment == new.fragment &&
                    old.viewModel.accountHatena.value == new.viewModel.accountHatena.value
    }

    /**
     * サインイン済みのMastodonアカウントボタン
     */
    class PrefItemMastodonAccount(
        private val fragment: Fragment,
        private val viewModel: AccountViewModel
    ) : PreferencesAdapter.Item {
        override val layoutId: Int = R.layout.listview_item_prefs_sign_in_mastodon

        override fun bind(binding: ViewDataBinding) {
            binding.alsoAs<ListviewItemPrefsSignInMastodonBinding> {
                it.vm = viewModel
                it.root.setOnClickListener { v ->
                    viewModel.openMastodonAuthenticationActivity(v.context)
                }
            }
        }

        override fun areItemsTheSame(old: PreferencesAdapter.Item, new: PreferencesAdapter.Item) =
            old is PrefItemMastodonAccount && new is PrefItemMastodonAccount &&
                    old.viewModel == new.viewModel

        override fun areContentsTheSame(old: PreferencesAdapter.Item, new: PreferencesAdapter.Item) =
            old is PrefItemMastodonAccount && new is PrefItemMastodonAccount &&
                    old.fragment == new.fragment &&
                    old.viewModel.accountMastodon.value == new.viewModel.accountMastodon.value
    }

    object MastodonAccountBindingAdapters {
        /**
         * "ユーザー名@インスタンス"を表示する
         */
        @JvmStatic
        @BindingAdapter("userName")
        fun bindUserNameAndInstance(textView: TextView, account: com.sys1yagi.mastodon4j.api.entity.Account?) {
            if (account == null) {
                textView.text = ""
                return
            }

            val uri = Uri.parse(account.url)
            val instance = uri.host ?: ""

            textView.text = String.format("%s@%s", account.userName, instance)
        }
    }
}
