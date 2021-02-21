package com.suihan74.satena.scenes.preferences.pages

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.TextView
import androidx.databinding.BindingAdapter
import androidx.databinding.ViewDataBinding
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.suihan74.hatenaLib.Account
import com.suihan74.satena.R
import com.suihan74.satena.SatenaApplication
import com.suihan74.satena.databinding.ListviewItemPrefsSignInHatenaBinding
import com.suihan74.satena.databinding.ListviewItemPrefsSignInMastodonBinding
import com.suihan74.satena.dialogs.AlertDialogFragment
import com.suihan74.satena.models.PreferenceKey
import com.suihan74.satena.scenes.authentication.HatenaAuthenticationActivity
import com.suihan74.satena.scenes.authentication.MastodonAuthenticationActivity
import com.suihan74.satena.scenes.preferences.*
import com.suihan74.utilities.AccountLoader
import com.suihan74.utilities.MastodonAccount
import com.suihan74.utilities.extensions.alsoAs
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

/**
 * 「アカウント」画面
 */
class AccountFragment : ListPreferencesFragment() {
    override val viewModel by lazy {
        AccountViewModel(requireContext(), SatenaApplication.instance.accountLoader)
    }
}

// ------ //

class AccountViewModel(
    context: Context,
    private val accountLoader: AccountLoader
) : ListPreferencesViewModel(context) {

    val accountHatena = MutableLiveData<Account?>()

    val accountMastodon = MutableLiveData<MastodonAccount?>()

    val savingHatenaCredentialEnabled = createLiveData<Boolean>(
        PreferenceKey.SAVE_HATENA_USER_ID_PASSWORD
    )

    // ------ //

    override fun onCreateView(fragment: ListPreferencesFragment) {
        combine(accountLoader.hatenaFlow, accountLoader.mastodonFlow, ::Pair)
            .onEach { (hatena, mastodon) ->
                accountHatena.value = hatena
                accountMastodon.value = mastodon
                load(fragment)
            }
            .launchIn(viewModelScope)

        viewModelScope.launch {
            accountLoader.signInAccounts(reSignIn = false)
            load(fragment)
        }
    }

    @OptIn(ExperimentalStdlibApi::class)
    override fun createList(fragment: ListPreferencesFragment): List<PreferencesAdapter.Item> = buildList {
        addSection(R.string.pref_accounts_service_name_hatena)
        if (accountHatena.value == null) {
            addButton(fragment, R.string.sign_in) {
                val context = fragment.requireContext()
                openHatenaAuthenticationActivity(context)
            }
        }
        else {
            add(PrefItemHatenaAccount(fragment, this@AccountViewModel))
            addPrefToggleItem(fragment, savingHatenaCredentialEnabled, R.string.pref_accounts_save_hatena_id_password_desc)
        }

        // --- //

        addSection(R.string.pref_accounts_service_name_mastodon)
        if (accountMastodon.value == null) {
            addButton(fragment, R.string.sign_in) {
                val context = fragment.requireContext()
                openMastodonAuthenticationActivity(context)
            }
        }
        else {
            add(PrefItemMastodonAccount(fragment, this@AccountViewModel))
        }
    }

    // ------ //

    /**
     * はてな認証画面を開く
     */
    private fun openHatenaAuthenticationActivity(context: Context) {
        val intent = Intent(context, HatenaAuthenticationActivity::class.java)
        context.startActivity(intent)
    }

    /**
     * Mastodon認証画面を開く
     */
    private fun openMastodonAuthenticationActivity(context: Context) {
        val intent = Intent(context, MastodonAuthenticationActivity::class.java)
        context.startActivity(intent)
    }

    // ------ //

    /**
     * はてなアカウントを削除する
     */
    private fun openHatenaAccountDeletionDialog(fragmentManager: FragmentManager) {
        AlertDialogFragment.Builder()
            .setTitle(R.string.confirm_dialog_title_simple)
            .setMessage(R.string.pref_accounts_delete_hatena_message)
            .setNegativeButton(R.string.dialog_cancel)
            .setPositiveButton(R.string.dialog_ok) {
                viewModelScope.launch {
                    accountLoader.deleteHatenaAccount()
                }
            }
            .create()
            .show(fragmentManager, null)
    }

    /**
     * Mastodonアカウントを削除する
     */
    private fun openMastodonAccountDeletionDialog(fragmentManager: FragmentManager) {
        AlertDialogFragment.Builder()
            .setTitle(R.string.confirm_dialog_title_simple)
            .setMessage(R.string.pref_accounts_delete_mastodon_message)
            .setNegativeButton(R.string.dialog_cancel)
            .setPositiveButton(R.string.dialog_ok) {
                viewModelScope.launch {
                    accountLoader.deleteMastodonAccount()
                }
            }
            .create()
            .show(fragmentManager, null)
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

                it.deleteButton.setOnClickListener {
                    viewModel.openHatenaAccountDeletionDialog(fragment.childFragmentManager)
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

    // ------ //

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

                it.deleteButton.setOnClickListener {
                    viewModel.openMastodonAccountDeletionDialog(fragment.childFragmentManager)
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
