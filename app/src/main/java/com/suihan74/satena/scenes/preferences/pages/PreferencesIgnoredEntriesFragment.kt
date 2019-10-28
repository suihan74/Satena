package com.suihan74.satena.scenes.preferences.pages

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.suihan74.satena.R
import com.suihan74.satena.SatenaApplication
import com.suihan74.satena.dialogs.IgnoredEntryDialogFragment
import com.suihan74.satena.models.IgnoredEntriesKey
import com.suihan74.satena.models.IgnoredEntry
import com.suihan74.satena.scenes.preferences.ignored.IgnoredEntriesAdapter
import com.suihan74.utilities.DividerItemDecorator
import com.suihan74.utilities.SafeSharedPreferences
import com.suihan74.utilities.get

class PreferencesIgnoredEntriesFragment : Fragment() {
    private lateinit var mIgnoredEntriesAdapter : IgnoredEntriesAdapter
    private lateinit var mIgnoredEntries : ArrayList<IgnoredEntry>

    companion object {
        fun createInstance() =
            PreferencesIgnoredEntriesFragment()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val root = inflater.inflate(R.layout.fragment_preferences_ignored_entries, container, false)

        val prefs = SafeSharedPreferences.create<IgnoredEntriesKey>(context!!)
        mIgnoredEntries = ArrayList(prefs.get<List<IgnoredEntry>>(IgnoredEntriesKey.IGNORED_ENTRIES))

        mIgnoredEntriesAdapter = object : IgnoredEntriesAdapter(mIgnoredEntries) {
            override fun onItemClicked(entry: IgnoredEntry) {
                val dialog = IgnoredEntryDialogFragment.createInstance(entry) { fm, modified ->
                    fm.get<PreferencesIgnoredEntriesFragment>()?.modifyItem(entry, modified)
                    true
                }
                dialog.show(fragmentManager!!, "dialog")
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
            ) { fm, ignoredEntry ->
                if (mIgnoredEntries.contains(ignoredEntry)) {
                    SatenaApplication.showToast("既に存在する非表示設定です")
                    return@createInstance false
                } else {
                    fm.get<PreferencesIgnoredEntriesFragment>()?.addItem(ignoredEntry)

                    SatenaApplication.showToast("${ignoredEntry.query} を非表示にしました")
                    return@createInstance true
                }
            }
            dialog.show(fragmentManager!!, "dialog")
        }

        return root
    }

    fun addItem(entry: IgnoredEntry) {
        mIgnoredEntries.add(entry)
        mIgnoredEntriesAdapter.addItem(entry)
        val prefs = SafeSharedPreferences.create<IgnoredEntriesKey>(context!!)
        prefs.edit {
            putObject(IgnoredEntriesKey.IGNORED_ENTRIES, mIgnoredEntries)
        }
    }

    fun removeItem(entry: IgnoredEntry) {
        mIgnoredEntries.remove(entry)
        mIgnoredEntriesAdapter.removeItem(entry)
        val prefs = SafeSharedPreferences.create<IgnoredEntriesKey>(context!!)
        prefs.edit {
            put(IgnoredEntriesKey.IGNORED_ENTRIES, mIgnoredEntries)
        }
    }

    fun modifyItem(older: IgnoredEntry, newer: IgnoredEntry) {
        val position = mIgnoredEntries.indexOf(older)
        mIgnoredEntries[position] = newer
        mIgnoredEntriesAdapter.modifyItem(older, newer)
        val prefs = SafeSharedPreferences.create<IgnoredEntriesKey>(context!!)
        prefs.edit {
            put(IgnoredEntriesKey.IGNORED_ENTRIES, mIgnoredEntries)
        }
    }
}
