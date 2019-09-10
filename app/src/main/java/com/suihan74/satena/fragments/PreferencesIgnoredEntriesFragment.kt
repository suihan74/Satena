package com.suihan74.satena.fragments

import android.app.AlertDialog
import android.os.Bundle
import android.support.design.widget.FloatingActionButton
import android.support.v4.app.Fragment
import android.support.v4.content.ContextCompat
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.suihan74.satena.R
import com.suihan74.satena.adapters.IgnoredEntriesAdapter
import com.suihan74.satena.dialogs.IgnoredEntryDialogFragment
import com.suihan74.satena.models.IgnoredEntriesKey
import com.suihan74.satena.models.IgnoredEntry
import com.suihan74.utilities.DividerItemDecorator
import com.suihan74.utilities.SafeSharedPreferences
import com.suihan74.utilities.showToast

class PreferencesIgnoredEntriesFragment : Fragment() {
    private lateinit var mIgnoredEntriesAdapter : IgnoredEntriesAdapter
    private lateinit var mIgnoredEntries : ArrayList<IgnoredEntry>

    companion object {
        fun createInstance() = PreferencesIgnoredEntriesFragment()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val root = inflater.inflate(R.layout.fragment_preferences_ignored_entries, container, false)


        val prefs = SafeSharedPreferences.create<IgnoredEntriesKey>(context!!)
        mIgnoredEntries = ArrayList(prefs.get<List<IgnoredEntry>>(IgnoredEntriesKey.IGNORED_ENTRIES))

        mIgnoredEntriesAdapter = object : IgnoredEntriesAdapter(mIgnoredEntries) {
            override fun onItemClicked(entry: IgnoredEntry) {
                val dialog = IgnoredEntryDialogFragment.createInstance(entry) { modified ->
                    mIgnoredEntriesAdapter.modifyItem(entry, modified)
                    true
                }
                dialog.show(fragmentManager, "dialog")
            }

            override fun onItemLongClicked(entry: IgnoredEntry): Boolean {
                val items = arrayOf(
                    "編集" to { onItemClicked(entry) },
                    "削除" to { this@PreferencesIgnoredEntriesFragment.removeItem(entry) }
                )

                AlertDialog.Builder(context, R.style.AlertDialogStyle)
                    .setTitle("${entry.type.name} ${entry.query}")
                    .setItems(items.map { it.first }.toTypedArray()) { _, which ->
                        items[which].second.invoke()
                    }
                    .setNegativeButton("Cancel", null)
                    .show()

                return true
            }
        }

        root.findViewById<RecyclerView>(R.id.ignored_entries_list).apply {
            val dividerItemDecoration = DividerItemDecorator(ContextCompat.getDrawable(context!!,
                R.drawable.recycler_view_item_divider
            )!!)
            addItemDecoration(dividerItemDecoration)
            layoutManager = LinearLayoutManager(context)
            adapter = mIgnoredEntriesAdapter
        }

        root.findViewById<FloatingActionButton>(R.id.add_button).setOnClickListener {
            val dialog = IgnoredEntryDialogFragment.createInstance(
                "",
                ""
            ) { ignoredEntry ->
                if (mIgnoredEntries.contains(ignoredEntry)) {
                    context!!.showToast("既に存在する非表示設定です")
                    return@createInstance false
                } else {
                    mIgnoredEntries.add(ignoredEntry)

                    prefs.edit {
                        putObject(IgnoredEntriesKey.IGNORED_ENTRIES, mIgnoredEntries)
                    }

                    mIgnoredEntriesAdapter.addItem(ignoredEntry)

                    context!!.showToast("${ignoredEntry.query} を非表示にしました")
                    return@createInstance true
                }
            }
            dialog.show(fragmentManager, "dialog")
        }

        return root
    }

    fun removeItem(entry: IgnoredEntry) {
        mIgnoredEntries.remove(entry)
        mIgnoredEntriesAdapter.removeItem(entry)

        val prefs = SafeSharedPreferences.create<IgnoredEntriesKey>(context!!)
        prefs.edit {
            put(IgnoredEntriesKey.IGNORED_ENTRIES, mIgnoredEntries)
        }
    }
}
