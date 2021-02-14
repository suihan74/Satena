package com.suihan74.satena.scenes.preferences.pages

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.annotation.StringRes
import androidx.databinding.ViewDataBinding
import com.google.android.gms.oss.licenses.OssLicensesMenuActivity
import com.suihan74.satena.R
import com.suihan74.satena.SatenaApplication
import com.suihan74.satena.databinding.ListviewItemAppInfoBinding
import com.suihan74.satena.dialogs.ReleaseNotesDialogFragment
import com.suihan74.satena.scenes.preferences.PreferencesAdapter
import com.suihan74.satena.scenes.preferences.addButton
import com.suihan74.satena.scenes.preferences.addSection
import com.suihan74.utilities.extensions.alsoAs
import org.threeten.bp.LocalDateTime

/**
 * 「情報」画面
 */
class InformationViewModel(private val context: Context) : ListPreferencesViewModel(context) {
    @OptIn(ExperimentalStdlibApi::class)
    override fun createList(fragment: ListPreferencesFragment) = buildList {
        val fragmentManager = fragment.childFragmentManager

        addSection(R.string.pref_information_section_app)
        add(AppInfoHeaderItem())
        addButton(R.string.pref_information_open_play_store_desc) {
            runCatching {
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    data = Uri.parse(context.getString(R.string.play_store))
                    `package` = "com.android.vending"
                }
                context.startActivity(intent)
            }
        }
        addButton(R.string.pref_information_release_notes_desc) {
            ReleaseNotesDialogFragment.createInstance()
                .show(fragmentManager, null)
        }

        // --- //

        addSection(R.string.pref_information_section_developer)
        addButton(R.string.developer) { openUrl(R.string.developer_hatena) }
        addButton(R.string.pref_information_developer_website) { openUrl(R.string.developer_website) }
        addButton(R.string.pref_information_developer_twitter) { openUrl(R.string.developer_twitter) }
        addButton(R.string.pref_information_developer_email) {
            runCatching {
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
        addButton(R.string.pref_information_hatena_rules) { openUrl(R.string.hatena_rule) }
        addButton(R.string.pref_information_privacy_policy) { openUrl(R.string.privacy_policy) }
        addButton(R.string.pref_information_licenses_desc) {
            runCatching {
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
    private fun openUrl(url: String) {
        runCatching {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            context.startActivity(intent)
        }
    }

    private fun openUrl(@StringRes textId: Int) = openUrl(context.getString(textId))

    // ------ //

    /**
     * アプリ情報表示項目
     */
    class AppInfoHeaderItem : PreferencesAdapter.Item {
        override val layoutId: Int = R.layout.listview_item_app_info
        override fun bind(binding: ViewDataBinding) {
            binding.alsoAs<ListviewItemAppInfoBinding> {
                it.copyrightYear = 2019.let { startYear -> LocalDateTime.now().year.let { y ->
                    if (y <= startYear) "$startYear-"
                    else "$startYear-$y"
                } }
                it.versionName = "version: " + SatenaApplication.instance.versionName
            }
        }
    }
}
