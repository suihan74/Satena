package com.suihan74.satena.scenes.preferences.pages

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.databinding.ViewDataBinding
import androidx.fragment.app.FragmentManager
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
    override fun createList(fragmentManager: FragmentManager) = buildList {
        addSection(R.string.pref_information_section_app)
        add(AppInfoHeaderItem())
        addButton(R.string.pref_information_release_notes_desc) {
            ReleaseNotesDialogFragment.createInstance()
                .show(fragmentManager, null)
        }

        // --- //

        addSection(R.string.pref_information_section_developer)
        addButton(R.string.developer)
        addButton(R.string.pref_information_developer_website) {
            runCatching {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(context.getString(R.string.developer_website)))
                context.startActivity(intent)
            }
        }
        addButton(R.string.pref_information_developer_twitter) {
            runCatching {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(context.getString(R.string.developer_twitter)))
                context.startActivity(intent)
            }
        }
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
        addButton(R.string.pref_information_hatena_rules) {
            runCatching {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://policies.hatena.ne.jp/rule"))
                context.startActivity(intent)
            }
        }
        addButton(R.string.pref_information_privacy_policy) {
            runCatching {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(context.getString(R.string.privacy_policy)))
                context.startActivity(intent)
            }
        }
        addButton(R.string.pref_information_licenses_desc) {
            runCatching {
                val intent = Intent(context, OssLicensesMenuActivity::class.java).apply {
                    putExtra("title", "Licenses")
                }
                context.startActivity(intent)
            }
        }
    }

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
