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
import com.google.android.material.tabs.TabLayout
import com.suihan74.satena.R
import com.suihan74.satena.models.IgnoreTarget
import com.suihan74.satena.models.IgnoredEntry
import com.suihan74.satena.models.IgnoredEntryType
import com.suihan74.utilities.showToast

class IgnoredEntryDialogFragment : DialogFragment() {
    private lateinit var mPositiveAction : ((IgnoredEntry)->Boolean)

    private var mEditingUrl = ""
    private var mEditingText = ""

    private var mSelectedTab : String? = null
    private var mIsEditMode = false

    private var mInitialTarget = IgnoreTarget.ENTRY
    private var mIgnoredEntry : IgnoredEntry? = null

    companion object {
        fun createInstance(url: String, title: String, positiveAction: ((IgnoredEntry)->Boolean)) = IgnoredEntryDialogFragment().apply {
            mEditingUrl = Regex("""^https?://""").find(url)?.let {
                url.substring(it.range.last + 1)
            } ?: url

            mEditingText = title
            mPositiveAction = positiveAction
            mIsEditMode = false
        }

        fun createInstance(ignoredEntry: IgnoredEntry, positiveAction: ((IgnoredEntry) -> Boolean)) = IgnoredEntryDialogFragment().apply {
            mEditingUrl = ignoredEntry.query
            mEditingText = ignoredEntry.query
            mPositiveAction = positiveAction
            mIgnoredEntry = ignoredEntry
            mInitialTarget = ignoredEntry.target
            mIsEditMode = true
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val inflater = LayoutInflater.from(context)
        val content = inflater.inflate(R.layout.fragment_dialog_ignored_entry, null)
        setStyle(STYLE_NORMAL, R.style.AlertDialogStyle)

        if (mIgnoredEntry != null) {
            mSelectedTab = when (mIgnoredEntry!!.type) {
                IgnoredEntryType.URL -> context!!.getString(R.string.ignored_entry_dialog_tab_url)
                IgnoredEntryType.TEXT -> context!!.getString(R.string.ignored_entry_dialog_tab_text)
            }
        }

        val urlTab = getString(R.string.ignored_entry_dialog_tab_url)
        val textTab = getString(R.string.ignored_entry_dialog_tab_text)
        mSelectedTab = if (mIsEditMode) mSelectedTab else urlTab

        val queryText = content.findViewById<EditText>(R.id.query_text).apply {
            setText(mEditingUrl)
            hint = mSelectedTab

            setHorizontallyScrolling(false)
            maxLines = Int.MAX_VALUE
        }

        val descText = content.findViewById<TextView>(R.id.desc_text).apply {
            when (mSelectedTab) {
                urlTab -> setText(R.string.ignored_entry_dialog_desc_url)
                textTab -> setText(R.string.ignored_entry_dialog_desc_text)
            }
        }

        content.findViewById<TabLayout>(R.id.tab_layout).apply {
            visibility = if (mIsEditMode) View.GONE else View.VISIBLE

            addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
                override fun onTabReselected(tab: TabLayout.Tab?) {}
                override fun onTabUnselected(tab: TabLayout.Tab?) {
                    when (tab!!.text) {
                        urlTab -> mEditingUrl = queryText.text.toString()
                        textTab -> mEditingText = queryText.text.toString()
                    }
                }

                override fun onTabSelected(tab: TabLayout.Tab?) {
                    mSelectedTab = tab!!.text.toString()
                    queryText.hint = mSelectedTab
                    when (mSelectedTab) {
                        urlTab -> {
                            queryText.setText(mEditingUrl)
                            descText.setText(R.string.ignored_entry_dialog_desc_url)
                            hideIgnoreTargetArea(content)
                        }
                        textTab -> {
                            queryText.setText(mEditingText)
                            descText.setText(R.string.ignored_entry_dialog_desc_text)
                            showIgnoreTargetArea(content)
                        }
                    }
                }
            })
        }

        when (mSelectedTab) {
            textTab -> {
                setIgnoreTarget(content, mInitialTarget)
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
                        type = when (mSelectedTab) {
                            urlTab -> IgnoredEntryType.URL
                            textTab -> IgnoredEntryType.TEXT
                            else -> throw RuntimeException("invalid tab")
                        },
                        query = queryText.text.toString(),
                        target = getIgnoreTarget(content)
                    )
                    if (mPositiveAction.invoke(ignoredEntry)) {
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
