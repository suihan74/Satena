package com.suihan74.satena.scenes.preferences.pages

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.observe
import androidx.recyclerview.widget.LinearLayoutManager
import com.suihan74.satena.R
import com.suihan74.satena.SatenaApplication
import com.suihan74.satena.databinding.FragmentPreferencesIgnoredEntriesBinding
import com.suihan74.satena.dialogs.AlertDialogFragment
import com.suihan74.satena.dialogs.IgnoredEntryDialogFragment
import com.suihan74.satena.models.ignoredEntry.IgnoredEntry
import com.suihan74.satena.scenes.preferences.PreferencesFragmentBase
import com.suihan74.satena.scenes.preferences.ignored.IgnoredEntriesAdapter
import com.suihan74.satena.scenes.preferences.ignored.IgnoredEntriesRepository
import com.suihan74.satena.scenes.preferences.ignored.IgnoredEntryViewModel
import com.suihan74.utilities.bindings.setDivider
import com.suihan74.utilities.extensions.showToast
import com.suihan74.utilities.lazyProvideViewModel
import com.suihan74.utilities.showAllowingStateLoss

class PreferencesIgnoredEntriesFragment : PreferencesFragmentBase() {
    companion object {
        fun createInstance() = PreferencesIgnoredEntriesFragment()
    }

    // ------ //

    private val viewModel: IgnoredEntryViewModel by lazyProvideViewModel {
        IgnoredEntryViewModel(
            IgnoredEntriesRepository(
                SatenaApplication.instance.ignoredEntryDao
            )
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
            override fun onItemClicked(entry: IgnoredEntry) {
                val dialog = IgnoredEntryDialogFragment.createInstance(entry) { dialog, modified ->
                    if (entry.query != modified.query && viewModel.entries.value?.contains(modified) == true) {
                        context?.showToast(R.string.msg_ignored_entry_dialog_already_existed)
                    }
                    else {
                        viewModel.update(
                            modified,
                            onSuccess = {
                                context?.showToast(R.string.msg_ignored_entry_dialog_succeeded, modified.query)
                                dialog.dismiss()
                            },
                            onError = { e ->
                                context?.showToast(R.string.msg_ignored_entry_dialog_failed)
                                Log.e("error", Log.getStackTraceString(e))
                            }
                        )
                    }
                    return@createInstance false
                }
                dialog.showAllowingStateLoss(childFragmentManager, DIALOG_IGNORE_ENTRY)
            }

            override fun onItemLongClicked(entry: IgnoredEntry): Boolean {
                val items = arrayOf(
                    getString(R.string.pref_ignored_entries_menu_edit) to { onItemClicked(entry) },
                    getString(R.string.pref_ignored_entries_menu_remove) to { viewModel.delete(entry) }
                )

                AlertDialogFragment.Builder()
                    .setTitle("${entry.type.name} ${entry.query}")
                    .setNegativeButton(R.string.dialog_cancel)
                    .setItems(items.map { it.first }) { _, which ->
                        items[which].second.invoke()
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
            val dialog = IgnoredEntryDialogFragment.createInstance { dialog, ignoredEntry ->
                if (viewModel.entries.value?.contains(ignoredEntry) == true) {
                    context?.showToast(R.string.msg_ignored_entry_dialog_already_existed)
                }
                else {
                    viewModel.add(
                        ignoredEntry,
                        onSuccess = {
                            context?.showToast(R.string.msg_ignored_entry_dialog_succeeded, ignoredEntry.query)
                            dialog.dismiss()
                        },
                        onError = { e ->
                            context?.showToast(R.string.msg_ignored_entry_dialog_failed)
                            Log.e("error", Log.getStackTraceString(e))
                        }
                    )
                }
                return@createInstance false
            }
            dialog.showAllowingStateLoss(childFragmentManager, DIALOG_IGNORE_ENTRY)
        }

        viewModel.entries.observe(viewLifecycleOwner) {
            ignoredEntriesAdapter.setItem(it)
        }

        return binding.root
    }
}
