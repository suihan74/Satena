package com.suihan74.satena.scenes.preferences.pages

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.suihan74.satena.R
import com.suihan74.satena.SatenaApplication
import com.suihan74.satena.dialogs.AlertDialogFragment
import com.suihan74.satena.dialogs.IgnoredEntryDialogFragment
import com.suihan74.satena.models.ignoredEntry.IgnoredEntry
import com.suihan74.satena.scenes.preferences.PreferencesFragmentBase
import com.suihan74.satena.scenes.preferences.ignored.IgnoredEntriesAdapter
import com.suihan74.satena.scenes.preferences.ignored.IgnoredEntryRepository
import com.suihan74.satena.scenes.preferences.ignored.IgnoredEntryViewModel
import com.suihan74.utilities.DividerItemDecorator
import com.suihan74.utilities.showToast
import kotlinx.android.synthetic.main.fragment_preferences_ignored_entries.view.*

class PreferencesIgnoredEntriesFragment : PreferencesFragmentBase(), AlertDialogFragment.Listener {
    private lateinit var mIgnoredEntriesAdapter : IgnoredEntriesAdapter

    private lateinit var viewModel: IgnoredEntryViewModel

    private var mDialogMenuItems: Array<Pair<String, (entry: IgnoredEntry)->Unit>>? = null

    companion object {
        fun createInstance() = PreferencesIgnoredEntriesFragment()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val vmFactory = IgnoredEntryViewModel.Factory(
            IgnoredEntryRepository(SatenaApplication.instance.ignoredEntryDao)
        )
        viewModel = ViewModelProvider(this, vmFactory)[IgnoredEntryViewModel::class.java]
        viewModel.init()
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
                val dialog = IgnoredEntryDialogFragment.createInstance(entry) { modified ->
                    if (entry.query != modified.query && viewModel.entries.value?.contains(modified) == true) {
                        context?.showToast(R.string.msg_ignored_entry_dialog_already_existed)
                        return@createInstance false
                    }
                    else {
                        viewModel.update(modified)

                        context?.showToast(R.string.msg_ignored_entry_dialog_succeeded, modified.query)
                        return@createInstance true
                    }
                }
                dialog.show(childFragmentManager, "dialog")
            }

            override fun onItemLongClicked(entry: IgnoredEntry): Boolean {
                AlertDialogFragment.Builder(R.style.AlertDialogStyle)
                    .setTitle("${entry.type.name} ${entry.query}")
                    .setNegativeButton(R.string.dialog_cancel)
                    .setItems(mDialogMenuItems!!.map { it.first })
                    .setAdditionalData("entry", entry)
                    .show(childFragmentManager, "menu_dialog")

                return true
            }
        }

        root.ignored_entries_list.apply {
            val dividerItemDecoration = DividerItemDecorator(ContextCompat.getDrawable(context!!,
                R.drawable.recycler_view_item_divider
            )!!)
            addItemDecoration(dividerItemDecoration)
            layoutManager = LinearLayoutManager(context)
            adapter = mIgnoredEntriesAdapter
        }

        root.add_button.setOnClickListener {
            val dialog = IgnoredEntryDialogFragment.createInstance { ignoredEntry ->
                if (viewModel.entries.value?.contains(ignoredEntry) == true) {
                    context?.showToast(R.string.msg_ignored_entry_dialog_already_existed)
                    return@createInstance false
                }
                else {
                    viewModel.add(ignoredEntry)

                    context?.showToast(R.string.msg_ignored_entry_dialog_succeeded, ignoredEntry.query)
                    return@createInstance true
                }
            }
            dialog.show(childFragmentManager, "dialog")
        }

        viewModel.entries.observe(this, Observer {
            mIgnoredEntriesAdapter.setItem(it)
        })

        return root
    }

    override fun onSelectItem(dialog: AlertDialogFragment, which: Int) {
        val entry = dialog.getAdditionalData<IgnoredEntry>("entry")!!
        mDialogMenuItems!![which].second.invoke(entry)
    }
}
