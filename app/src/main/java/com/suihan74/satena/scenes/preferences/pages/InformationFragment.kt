package com.suihan74.satena.scenes.preferences.pages

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.databinding.BindingAdapter
import androidx.databinding.ViewDataBinding
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.oss.licenses.OssLicensesMenuActivity
import com.suihan74.satena.R
import com.suihan74.satena.SatenaApplication
import com.suihan74.satena.databinding.FragmentPreferencesInformation2Binding
import com.suihan74.satena.databinding.ListviewItemAppInfoBinding
import com.suihan74.satena.dialogs.ReleaseNotesDialogFragment
import com.suihan74.satena.scenes.preferences.PreferencesAdapter
import com.suihan74.utilities.extensions.alsoAs
import org.threeten.bp.LocalDateTime

class InformationFragment : Fragment() {
    companion object {
        fun createInstance() = InformationFragment()
    }

    // ------ //

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val binding = FragmentPreferencesInformation2Binding.inflate(inflater, container, false).also {
            it.lifecycleOwner = viewLifecycleOwner
            initializeRecyclerView(it.recyclerView)
        }
        return binding.root
    }

    // ------ //

    @OptIn(ExperimentalStdlibApi::class)
    private fun initializeRecyclerView(recyclerView: RecyclerView) {
        val adapter = PreferencesAdapter(viewLifecycleOwner)

        val items = buildList {
            add(PreferencesAdapter.Section(R.string.pref_information_section_app))
            add(AppInfoHeaderItem())
            add(PreferencesAdapter.Button(R.string.pref_information_release_notes_desc) {
                ReleaseNotesDialogFragment.createInstance()
                    .show(childFragmentManager, null)
            })

            // --- //

            add(PreferencesAdapter.Section(R.string.pref_information_section_developer))
            add(PreferencesAdapter.Button(R.string.developer))
            add(PreferencesAdapter.Button(R.string.pref_information_developer_website) {
                runCatching {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.developer_website)))
                    startActivity(intent)
                }
            })
            add(PreferencesAdapter.Button(R.string.pref_information_developer_twitter) {
                runCatching {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.developer_twitter)))
                    startActivity(intent)
                }
            })
            add(PreferencesAdapter.Button(R.string.pref_information_developer_email) {
                runCatching {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.developer_email)))
                    startActivity(intent)
                }
            })

            // --- //

            add(PreferencesAdapter.Section(R.string.pref_information_section_info))
            add(PreferencesAdapter.Button(R.string.pref_information_hatena_rules) {
                runCatching {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://policies.hatena.ne.jp/rule"))
                    startActivity(intent)
                }
            })
            add(PreferencesAdapter.Button(R.string.pref_information_privacy_policy) {
                runCatching {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.privacy_policy)))
                    startActivity(intent)
                }
            })
            add(PreferencesAdapter.Button(R.string.pref_information_licenses_desc) {
                runCatching {
                    val intent = Intent(activity, OssLicensesMenuActivity::class.java).apply {
                        putExtra("title", "Licenses")
                    }
                    startActivity(intent)
                }
            })
        }

        adapter.submitList(items)

        recyclerView.adapter = adapter
    }

    // ------ //

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

object TextViewBindingAdapters {
    @JvmStatic
    @BindingAdapter("android:text")
    fun bindTextResource(textView: TextView, textId: Int?) {
        textView.text =
            if (textId == null || textId == 0) ""
            else textView.context.getText(textId)
    }
}
