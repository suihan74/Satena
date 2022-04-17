package com.suihan74.satena.scenes.preferences.pages

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.annotation.StringRes
import androidx.databinding.ViewDataBinding
import androidx.fragment.app.FragmentManager
import com.google.android.gms.oss.licenses.OssLicensesMenuActivity
import com.suihan74.satena.R
import com.suihan74.satena.SatenaApplication
import com.suihan74.satena.databinding.ListviewItemAppInfoBinding
import com.suihan74.satena.dialogs.ReleaseNotesDialogFragment
import com.suihan74.satena.scenes.preferences.PreferencesActivity
import com.suihan74.satena.scenes.preferences.PreferencesAdapter
import com.suihan74.satena.scenes.preferences.addButton
import com.suihan74.satena.scenes.preferences.addSection
import com.suihan74.utilities.extensions.alsoAs
import com.suihan74.utilities.extensions.requireActivity
import java.time.LocalDateTime

/**
 * 「情報」画面
 */
class InformationFragment : ListPreferencesFragment() {
    override val viewModel
        get() = requireActivity<PreferencesActivity>().informationViewModel
}

// ------ //

class InformationViewModel(context: Context) : ListPreferencesViewModel(context) {
    @OptIn(ExperimentalStdlibApi::class)
    override fun createList(
        context: Context,
        fragmentManager: FragmentManager
    ) = buildList {
        addSection(R.string.pref_information_section_app)
        add(AppInfoHeaderItem())
        addButton(context, R.string.pref_information_open_play_store_desc) {
            kotlin.runCatching {
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    data = Uri.parse(context.getString(R.string.play_store))
                    `package` = "com.android.vending"
                }
                context.startActivity(intent)
            }
        }
        addButton(context, R.string.pref_information_release_notes_desc) {
            ReleaseNotesDialogFragment.createInstance()
                .show(fragmentManager, null)
        }

        // --- //

        addSection(R.string.pref_information_section_developer)
        addButton(context, R.string.developer) { openUrl(context, R.string.developer_hatena) }
        addButton(context, R.string.pref_information_developer_website) { openUrl(context, R.string.developer_website) }
        addButton(context, R.string.pref_information_developer_twitter) { openUrl(context, R.string.developer_twitter) }
        addButton(context, R.string.pref_information_developer_email) {
            kotlin.runCatching {
                val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:")).also {
                    it.putExtra(
                        Intent.EXTRA_EMAIL,
                        arrayOf(context.getString(R.string.developer_email))
                    )
                }
                context.startActivity(intent)
            }
        }

        // --- //

        addSection(R.string.pref_information_section_info)
        addButton(context, R.string.pref_information_hatena_rules) { openUrl(context, R.string.hatena_rule) }
        addButton(context, R.string.pref_information_privacy_policy) { openUrl(context, R.string.privacy_policy) }
        addButton(context, R.string.pref_information_licenses_desc) {
            kotlin.runCatching {
                val intent = Intent(context, OssLicensesMenuActivity::class.java).apply {
                    putExtra("title", "Licenses")
                }
                context.startActivity(intent)
            }
        }
    }

    /**
     * URLを開くIntentを発行する
     */
    private fun openUrl(context: Context, url: String) {
        runCatching {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            context.startActivity(intent)
        }
    }

    private fun openUrl(context: Context, @StringRes textId: Int) =
        openUrl(context, context.getString(textId))

    // ------ //

    /**
     * アプリ情報表示項目
     */
    class AppInfoHeaderItem(private val tag: String? = null) : PreferencesAdapter.Item {
        private val copyrightYear =
            2019.let { startYear -> LocalDateTime.now().year.let { y ->
                if (y <= startYear) "$startYear-"
                else "$startYear-$y"
            } }

        private val versionName = "version: " + SatenaApplication.instance.versionName

        // ------ //

        override val layoutId: Int = R.layout.listview_item_app_info

        override fun bind(binding: ViewDataBinding) {
            binding.alsoAs<ListviewItemAppInfoBinding> {
                it.copyrightYear = copyrightYear
                it.versionName = versionName
            }
        }

        override fun areItemsTheSame(old: PreferencesAdapter.Item, new: PreferencesAdapter.Item) =
            old is AppInfoHeaderItem && new is AppInfoHeaderItem && old.tag == new.tag

        override fun areContentsTheSame(old: PreferencesAdapter.Item, new: PreferencesAdapter.Item) =
            old is AppInfoHeaderItem && new is AppInfoHeaderItem &&
                    old.copyrightYear == new.copyrightYear &&
                    old.versionName == new.versionName
    }
}
