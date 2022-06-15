package com.suihan74.satena.scenes.preferences.pages

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.TextView
import androidx.databinding.BindingAdapter
import androidx.databinding.ViewDataBinding
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewModelScope
import com.suihan74.satena.R
import com.suihan74.satena.SatenaApplication
import com.suihan74.satena.databinding.ListviewItemPrefsSignInHatenaBinding
import com.suihan74.satena.databinding.ListviewItemPrefsSignInMastodonBinding
import com.suihan74.satena.dialogs.AlertDialogFragment
import com.suihan74.satena.models.PreferenceKey
import com.suihan74.satena.models.TootVisibility
import com.suihan74.satena.scenes.authentication.HatenaAuthenticationActivity
import com.suihan74.satena.scenes.authentication.MastodonAuthenticationActivity
import com.suihan74.satena.scenes.preferences.*
import com.suihan74.utilities.AccountLoader
import com.suihan74.utilities.extensions.ContextExtensions.showToast
import com.suihan74.utilities.extensions.alsoAs
import com.suihan74.utilities.extensions.requireActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 「アカウント」画面
 */
class AccountFragment : ListPreferencesFragment() {
    override val viewModel
        get() = requireActivity<PreferencesActivity>().accountViewModel
}

// ------ //

class AccountViewModel(
    context: Context,
    private val accountLoader: AccountLoader
) : ListPreferencesViewModel(context) {

    val accountHatena = accountLoader.hatenaFlow

    val accountMastodon = accountLoader.mastodonFlow

    private val mastodonStatusVisibility = createLiveDataEnum(
        PreferenceKey.MASTODON_POST_VISIBILITY,
        { it.ordinal },
        { TootVisibility.values()[it] }
    )

    // ------ //

    override fun onCreateView(fragment: ListPreferencesFragment) {
        combine(accountHatena, accountMastodon, ::Pair)
            .onEach { load(fragment) }
            .launchIn(viewModelScope)

        viewModelScope.launch {
            runCatching {
                accountLoader.signInAccounts(reSignIn = false)
            }.onFailure {
                withContext(Dispatchers.Main) {
                    SatenaApplication.instance.showToast(R.string.msg_hatena_sign_in_failed)
                }
            }
            load(fragment)
        }
    }

    override fun createList(
        context: Context,
        fragmentManager: FragmentManager
    ): List<PreferencesAdapter.Item> = buildList {
        addSection(R.string.pref_accounts_service_name_hatena)
        if (accountHatena.value == null) {
            addButton(context, R.string.sign_in) {
                openHatenaAuthenticationActivity(context)
            }
        }
        else {
            add(PrefItemHatenaAccount(context, fragmentManager, this@AccountViewModel))
        }

        // --- //

        addSection(R.string.pref_accounts_service_name_mastodon)
        if (accountMastodon.value == null) {
            addButton(context, R.string.sign_in) {
                openMastodonAuthenticationActivity(context)
            }
        }
        else {
            add(PrefItemMastodonAccount(fragmentManager, this@AccountViewModel))
            addPrefItem(mastodonStatusVisibility, R.string.pref_accounts_mastodon_status_visibility_desc) {
                openEnumSelectionDialog(
                    TootVisibility.values(),
                    mastodonStatusVisibility,
                    R.string.pref_accounts_mastodon_status_visibility_desc,
                    fragmentManager
                )
            }
        }
    }

    // ------ //

    /**
     * はてな認証画面を開く
     */
    private fun openHatenaAuthenticationActivity(context: Context) {
        val intent = Intent(context, HatenaAuthenticationActivity::class.java)
        context.alsoAs<PreferencesActivity> {
            it.hatenaAuthenticationLauncher.launch(intent)
        } ?: run {
            context.startActivity(intent)
        }
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
            .setPositiveButton(R.string.dialog_ok) { fragment ->
                val activity = fragment.requireActivity() as PreferencesActivity
                activity.lifecycleScope.launch {
                    runCatching {
                        accountLoader.deleteHatenaAccount()
                    }.onSuccess {
                        activity.ignoredUsersViewModel.clear()
                        activity.followingsViewModel.clear()
                    }
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
        private val context: Context,
        private val fragmentManager: FragmentManager,
        private val viewModel: AccountViewModel
    ) : PreferencesAdapter.Item {
        override val layoutId: Int = R.layout.listview_item_prefs_sign_in_hatena

        override val description: String
            get() = viewModel.accountHatena.value?.name ?: "Hatena"

        override fun bind(binding: ViewDataBinding) {
            binding.alsoAs<ListviewItemPrefsSignInHatenaBinding> {
                it.vm = viewModel

                it.root.setOnClickListener {
                    viewModel.openHatenaAuthenticationActivity(context)
                }

                it.deleteButton.setOnClickListener {
                    viewModel.openHatenaAccountDeletionDialog(fragmentManager)
                }
            }
        }

        override fun areItemsTheSame(old: PreferencesAdapter.Item, new: PreferencesAdapter.Item) =
            old is PrefItemHatenaAccount && new is PrefItemHatenaAccount &&
                    old.viewModel == new.viewModel

        override fun areContentsTheSame(old: PreferencesAdapter.Item, new: PreferencesAdapter.Item) =
            old is PrefItemHatenaAccount && new is PrefItemHatenaAccount &&
                    old.viewModel.accountHatena.value == new.viewModel.accountHatena.value
    }

    // ------ //

    /**
     * サインイン済みのMastodonアカウントボタン
     */
    class PrefItemMastodonAccount(
        private val fragmentManager: FragmentManager,
        private val viewModel: AccountViewModel
    ) : PreferencesAdapter.Item {
        override val layoutId: Int = R.layout.listview_item_prefs_sign_in_mastodon

        override val description: String
            get() = viewModel.accountMastodon.value?.userName ?: "Mastodon"

        override fun bind(binding: ViewDataBinding) {
            binding.alsoAs<ListviewItemPrefsSignInMastodonBinding> {
                it.vm = viewModel

                it.root.setOnClickListener { v ->
                    viewModel.openMastodonAuthenticationActivity(v.context)
                }

                it.deleteButton.setOnClickListener {
                    viewModel.openMastodonAccountDeletionDialog(fragmentManager)
                }
            }
        }

        override fun areItemsTheSame(old: PreferencesAdapter.Item, new: PreferencesAdapter.Item) =
            old is PrefItemMastodonAccount && new is PrefItemMastodonAccount &&
                    old.viewModel == new.viewModel

        override fun areContentsTheSame(old: PreferencesAdapter.Item, new: PreferencesAdapter.Item) =
            old is PrefItemMastodonAccount && new is PrefItemMastodonAccount &&
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
