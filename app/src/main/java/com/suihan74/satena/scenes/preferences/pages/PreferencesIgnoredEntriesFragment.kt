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
import com.suihan74.satena.dialogs.AlertDialogFragment2
import com.suihan74.satena.dialogs.IgnoredEntryDialogFragment
import com.suihan74.satena.models.ignoredEntry.IgnoredEntry
import com.suihan74.satena.scenes.preferences.PreferencesFragmentBase
import com.suihan74.satena.scenes.preferences.ignored.IgnoredEntriesAdapter
import com.suihan74.satena.scenes.preferences.ignored.IgnoredEntriesRepository
import com.suihan74.satena.scenes.preferences.ignored.IgnoredEntryViewModel
import com.suihan74.utilities.bindings.setDivider
import com.suihan74.utilities.extensions.showToast
import com.suihan74.utilities.provideViewModel
import com.suihan74.utilities.showAllowingStateLoss
import kotlinx.android.synthetic.main.fragment_preferences_ignored_entries.view.*

class PreferencesIgnoredEntriesFragment : PreferencesFragmentBase() {
    private lateinit var mIgnoredEntriesAdapter : IgnoredEntriesAdapter

    private val viewModel: IgnoredEntryViewModel by lazy {
        provideViewModel(this) {
            IgnoredEntryViewModel(
                IgnoredEntriesRepository(
                    SatenaApplication.instance.ignoredEntryDao
                )
            )
        }
    }

    private var mDialogMenuItems: Array<Pair<String, (entry: IgnoredEntry)->Unit>>? = null

    /** メニューダイアログ用のタグ */
    private val DIALOG_MENU by lazy { "DIALOG_MENU" }

    /** 非表示エントリ追加ダイアログ用のタグ */
    private val DIALOG_IGNORE_ENTRY by lazy { "DIALOG_IGNORE_ENTRY" }

    companion object {
        fun createInstance() = PreferencesIgnoredEntriesFragment()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val root = inflater.inflate(R.layout.fragment_preferences_ignored_entries, container, false)

        mDialogMenuItems = arrayOf(
            getString(R.string.pref_ignored_entries_menu_edit) to { entry ->
                mIgnoredEntriesAdapter.onItemClicked(entry)
            },
            getString(R.string.pref_ignored_entries_menu_remove) to { entry ->
                viewModel.delete(entry)
            }
        )

        mIgnoredEntriesAdapter = object : IgnoredEntriesAdapter() {
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
                AlertDialogFragment2.Builder()
                    .setTitle("${entry.type.name} ${entry.query}")
                    .setNegativeButton(R.string.dialog_cancel)
                    .setItems(mDialogMenuItems!!.map { it.first }) { _, which ->
                        mDialogMenuItems!![which].second.invoke(entry)
                    }
                    .create()
                    .showAllowingStateLoss(childFragmentManager, DIALOG_MENU)

                return true
            }
        }

        root.ignored_entries_list.apply {
            setDivider(R.drawable.recycler_view_item_divider)
            layoutManager = LinearLayoutManager(context)
            adapter = mIgnoredEntriesAdapter
        }

        root.add_button.setOnClickListener {
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
            mIgnoredEntriesAdapter.setItem(it)
        }

        return root
    }
}
