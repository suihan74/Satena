package com.suihan74.satena.scenes.preferences.pages

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import com.suihan74.satena.R
import com.suihan74.satena.SatenaApplication
import com.suihan74.satena.databinding.FragmentPreferencesIgnoredEntriesBinding
import com.suihan74.satena.dialogs.AlertDialogFragment
import com.suihan74.satena.dialogs.IgnoredEntryDialogFragment
import com.suihan74.satena.models.ignoredEntry.IgnoredEntry
import com.suihan74.satena.scenes.preferences.ignored.IgnoredEntriesAdapter
import com.suihan74.satena.scenes.preferences.ignored.IgnoredEntryViewModel
import com.suihan74.utilities.Listener
import com.suihan74.utilities.bindings.setDivider
import com.suihan74.utilities.lazyProvideViewModel
import com.suihan74.utilities.showAllowingStateLoss

class PreferencesIgnoredEntriesFragment : Fragment() {
    companion object {
        fun createInstance() = PreferencesIgnoredEntriesFragment()
    }

    // ------ //

    private val viewModel: IgnoredEntryViewModel by lazyProvideViewModel {
        IgnoredEntryViewModel(
            SatenaApplication.instance.ignoredEntriesRepository
        )
    }

    // ------ //

    /** メニューダイアログ用のタグ */
    private val DIALOG_MENU by lazy { "DIALOG_MENU" }

    /** 非表示エントリ追加ダイアログ用のタグ */
    private val DIALOG_IGNORE_ENTRY by lazy { "DIALOG_IGNORE_ENTRY" }

    // ------ //

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val binding = FragmentPreferencesIgnoredEntriesBinding.inflate(inflater, container, false)

        val ignoredEntriesAdapter = object : IgnoredEntriesAdapter() {
            private fun openIgnoredEntryDialog(entry: IgnoredEntry, fragmentManager: FragmentManager) {
                val dialog = IgnoredEntryDialogFragment.createInstance(entry)
                dialog.showAllowingStateLoss(fragmentManager, DIALOG_IGNORE_ENTRY)
            }

            override fun onItemClicked(entry: IgnoredEntry) {
                openIgnoredEntryDialog(entry, childFragmentManager)
            }

            override fun onItemLongClicked(entry: IgnoredEntry): Boolean {
                val items = arrayOf<Pair<String, Listener<AlertDialogFragment>>>(
                    // 画面回転対策でfragmentManagerを明示的に渡している
                    getString(R.string.pref_ignored_entries_menu_edit) to { f -> openIgnoredEntryDialog(entry, f.parentFragmentManager) },
                    getString(R.string.pref_ignored_entries_menu_remove) to { viewModel.delete(entry) }
                )

                AlertDialogFragment.Builder()
                    .setTitle("${entry.type.name} ${entry.query}")
                    .setNegativeButton(R.string.dialog_cancel)
                    .setItems(items.map { it.first }) { f, which ->
                        items[which].second.invoke(f)
                    }
                    .create()
                    .showAllowingStateLoss(childFragmentManager, DIALOG_MENU)

                return true
            }
        }

        binding.ignoredEntriesList.apply {
            setDivider(R.drawable.recycler_view_item_divider)
            layoutManager = LinearLayoutManager(context)
            adapter = ignoredEntriesAdapter
        }

        binding.addButton.setOnClickListener {
            val dialog = IgnoredEntryDialogFragment.createInstance()
            dialog.showAllowingStateLoss(childFragmentManager, DIALOG_IGNORE_ENTRY)
        }

        viewModel.entries.observe(viewLifecycleOwner, Observer {
            ignoredEntriesAdapter.setItem(it)
        })

        return binding.root
    }
}
