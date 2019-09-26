package com.suihan74.satena.dialogs

import android.app.AlertDialog
import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.CheckBox
import android.widget.EditText
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import com.google.android.material.tabs.TabLayout
import com.suihan74.satena.R
import com.suihan74.satena.models.IgnoreTarget
import com.suihan74.satena.models.IgnoredEntry
import com.suihan74.satena.models.IgnoredEntryType
import com.suihan74.utilities.showToast
import com.suihan74.utilities.toVisibility

class IgnoredEntryDialogFragment : DialogFragment() {
    private class Members {
        var positiveAction : ((FragmentManager, IgnoredEntry)->Boolean)? = null

        var editingUrl = ""
        var editingText = ""

        var selectedTab : String? = null
        var isEditMode = false

        var initialTarget = IgnoreTarget.ENTRY
        var ignoredEntry : IgnoredEntry? = null
    }

    private var members = Members()

    companion object {
        fun createInstance(url: String, title: String, positiveAction: ((FragmentManager, IgnoredEntry)->Boolean)) = IgnoredEntryDialogFragment().apply {
            members.editingUrl = Regex("""^https?://""").find(url)?.let {
                url.substring(it.range.last + 1)
            } ?: url

            members.editingText = title
            members.positiveAction = positiveAction
            members.isEditMode = false
        }

        fun createInstance(ignoredEntry: IgnoredEntry, positiveAction: ((FragmentManager, IgnoredEntry) -> Boolean)) = IgnoredEntryDialogFragment().apply {
            members.editingUrl = ignoredEntry.query
            members.editingText = ignoredEntry.query
            members.positiveAction = positiveAction
            members.ignoredEntry = ignoredEntry
            members.initialTarget = ignoredEntry.target
            members.isEditMode = true
        }

        private var savedMembers : Members? = null
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        savedMembers = members
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val inflater = LayoutInflater.from(context)
        val content = inflater.inflate(R.layout.fragment_dialog_ignored_entry, null)
        setStyle(STYLE_NORMAL, R.style.AlertDialogStyle)

        savedMembers?.let {
            members = it
            savedMembers = null
        }

        if (members.ignoredEntry != null) {
            members.selectedTab = when (members.ignoredEntry!!.type) {
                IgnoredEntryType.URL -> context!!.getString(R.string.ignored_entry_dialog_tab_url)
                IgnoredEntryType.TEXT -> context!!.getString(R.string.ignored_entry_dialog_tab_text)
            }
        }

        val urlTab = getString(R.string.ignored_entry_dialog_tab_url)
        val textTab = getString(R.string.ignored_entry_dialog_tab_text)
        members.selectedTab = members.selectedTab
            ?: if (members.isEditMode) members.selectedTab else urlTab

        val queryText = content.findViewById<EditText>(R.id.query_text).apply {
            setText(members.editingUrl)
            hint = members.selectedTab

            setHorizontallyScrolling(false)
            maxLines = Int.MAX_VALUE
        }

        val descText = content.findViewById<TextView>(R.id.desc_text).apply {
            when (members.selectedTab) {
                urlTab -> setText(R.string.ignored_entry_dialog_desc_url)
                textTab -> setText(R.string.ignored_entry_dialog_desc_text)
            }
        }

        content.findViewById<TabLayout>(R.id.tab_layout).apply {
            visibility = (!members.isEditMode).toVisibility()

            if (members.selectedTab != null) {
                val idx = if (members.selectedTab == urlTab) 0 else 1
                getTabAt(idx)?.select()
            }

            addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
                override fun onTabReselected(tab: TabLayout.Tab?) {}
                override fun onTabUnselected(tab: TabLayout.Tab?) {
                    when (tab!!.text) {
                        urlTab -> members.editingUrl = queryText.text.toString()
                        textTab -> members.editingText = queryText.text.toString()
                    }
                }

                override fun onTabSelected(tab: TabLayout.Tab?) {
                    members.selectedTab = tab!!.text.toString()
                    queryText.hint = members.selectedTab
                    when (members.selectedTab) {
                        urlTab -> {
                            queryText.setText(members.editingUrl)
                            descText.setText(R.string.ignored_entry_dialog_desc_url)
                            hideIgnoreTargetArea(content)
                        }
                        textTab -> {
                            queryText.setText(members.editingText)
                            descText.setText(R.string.ignored_entry_dialog_desc_text)
                            showIgnoreTargetArea(content)
                        }
                    }
                }
            })
        }

        when (members.selectedTab) {
            textTab -> {
                setIgnoreTarget(content, members.initialTarget)
                showIgnoreTargetArea(content)
            }
            else -> hideIgnoreTargetArea(content)
        }

        return AlertDialog.Builder(context, R.style.AlertDialogStyle)
            .setView(content)
            .setMessage("非表示エントリ登録")
            .setPositiveButton("登録", null)
            .setNegativeButton("Cancel", null)
            .show()
            .apply {
                getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener {
                    if (queryText.text.isNullOrBlank()) {
                        context.showToast("クエリを入力してください")
                        return@setOnClickListener
                    }

                    val ignoredEntry = IgnoredEntry(
                        type = when (members.selectedTab) {
                            urlTab -> IgnoredEntryType.URL
                            textTab -> IgnoredEntryType.TEXT
                            else -> throw RuntimeException("invalid tab")
                        },
                        query = queryText.text.toString(),
                        target = getIgnoreTarget(content)
                    )
                    if (members.positiveAction?.invoke(fragmentManager!!, ignoredEntry) != false) {
                        this.dismiss()
                    }
                }
            }
    }

    private fun showIgnoreTargetArea(root: View) {
        root.apply {
            findViewById<View>(R.id.target_desc_text).visibility = View.VISIBLE
            findViewById<View>(R.id.target_entry_checkbox).visibility = View.VISIBLE
            findViewById<View>(R.id.target_bookmark_checkbox).visibility = View.VISIBLE
        }
    }

    private fun hideIgnoreTargetArea(root: View) {
        root.apply {
            findViewById<View>(R.id.target_desc_text).visibility = View.GONE
            findViewById<View>(R.id.target_entry_checkbox).visibility = View.GONE
            findViewById<View>(R.id.target_bookmark_checkbox).visibility = View.GONE
        }
    }

    private fun getIgnoreTarget(root: View) : IgnoreTarget {
        val entry = if (root.findViewById<CheckBox>(R.id.target_entry_checkbox).isChecked) IgnoreTarget.ENTRY else IgnoreTarget.NONE
        val bookmark = if (root.findViewById<CheckBox>(R.id.target_bookmark_checkbox).isChecked) IgnoreTarget.BOOKMARK else IgnoreTarget.NONE
        return entry or bookmark
    }

    private fun setIgnoreTarget(root: View, target: IgnoreTarget) {
        root.apply {
            findViewById<CheckBox>(R.id.target_entry_checkbox).isChecked = target contains IgnoreTarget.ENTRY
            findViewById<CheckBox>(R.id.target_bookmark_checkbox).isChecked = target contains IgnoreTarget.BOOKMARK
        }
    }
}
