package com.suihan74.satena.fragments

import android.app.AlertDialog
import android.content.Context
import android.os.Bundle
import android.support.v7.widget.Toolbar
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import com.suihan74.HatenaLib.EntriesType
import com.suihan74.HatenaLib.HatenaClient
import com.suihan74.HatenaLib.SearchType
import com.suihan74.satena.R

class SearchEntriesFragment : MultipurposeSingleTabEntriesFragment() {
    private lateinit var mRoot : View
    private lateinit var mQuery : String
    private lateinit var mSearchType : SearchType
    private var mEntriesType : EntriesType = EntriesType.Recent

    companion object {
        fun createInstance(query: String, searchType: SearchType) = SearchEntriesFragment().apply {
            mQuery = query
            mSearchType = searchType
        }

        fun createInstance() = SearchEntriesFragment().apply {
            mQuery = ""
            mSearchType = SearchType.Text
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString("query", mQuery)
        outState.putInt("search_type", mSearchType.int)
        outState.putInt("entries_type", mEntriesType.int)
    }

    override fun onRestoreSaveInstanceState(savedInstanceState: Bundle) {
        mQuery = savedInstanceState.getString("query")!!
        mSearchType = SearchType.fromInt(savedInstanceState.getInt("search_type"))
        mEntriesType = EntriesType.fromInt(savedInstanceState.getInt("entries_type"))
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        mRoot = super.onCreateView(inflater, container, savedInstanceState)!!

        showSearchLayout()

        mRoot.findViewById<EditText>(R.id.search_query).apply {
            visibility = View.VISIBLE
            text.clear()
            text.append(mQuery)

            if (savedInstanceState == null) {
                if (!mQuery.isBlank()) {
                    isFocusable = false
                    isFocusableInTouchMode = false
                }
            }
            else {
                isFocusable = false
                isFocusableInTouchMode = false

                setOnClickListener {
                    it.isFocusable = true
                    it.isFocusableInTouchMode = true
                    it.requestFocus()

                    val im = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                    im.showSoftInput(it, 0)
                    setSelection(text.length)

                    setOnClickListener(null)
                }
            }

            setOnEditorActionListener { _, action, _ ->
                when (action) {
                    EditorInfo.IME_ACTION_SEARCH -> search()
                    else -> false
                }
            }
        }

        // 検索対象(tag/text)を切り替え
        fun setSearchTypeText(s: SearchType) : String = when (s) {
            SearchType.Text -> getString(R.string.search_type_text)
            SearchType.Tag -> getString(R.string.search_type_tag)
        }

        mRoot.findViewById<Button>(R.id.search_type_button).apply {
            text = setSearchTypeText(mSearchType)
            setOnClickListener {
                val items = SearchType.values().map { setSearchTypeText(it) }.toTypedArray()
                AlertDialog.Builder(context!!, R.style.AlertDialogStyle)
                    .setTitle("検索対象")
                    .setNegativeButton("Cancel", null)
                    .setSingleChoiceItems(items, SearchType.values().indexOf(mSearchType)) { dialog, which ->
                        val newSearchType = SearchType.values()[which]
                        text = setSearchTypeText(newSearchType)
                        if (newSearchType != mSearchType) {
                            mSearchType = newSearchType
                            refreshEntries()
                        }
                        else {
                            mSearchType = newSearchType
                        }
                        dialog.dismiss()
                    }
                    .show()
            }
        }

        // 検索順(hot/recent)を切り替え
        fun setEntriesTypeText(e: EntriesType) : String = when (e) {
            EntriesType.Recent -> getString(R.string.entries_tab_recent)
            EntriesType.Hot -> getString(R.string.entries_tab_hot)
        }

        mRoot.findViewById<Button>(R.id.search_order_button).apply {
            text = setEntriesTypeText(mEntriesType)
            setOnClickListener {
                val items = EntriesType.values().map { setEntriesTypeText(it) }.toTypedArray()
                AlertDialog.Builder(context!!, R.style.AlertDialogStyle)
                    .setTitle("検索順")
                    .setNegativeButton("Cancel", null)
                    .setSingleChoiceItems(items, EntriesType.values().indexOf(mEntriesType)) { dialog, which ->
                        val newEntriesType = EntriesType.values()[which]
                        text = setEntriesTypeText(newEntriesType)
                        if (newEntriesType != mEntriesType) {
                            mEntriesType = newEntriesType
                            refreshEntries()
                        }
                        else {
                            mEntriesType = newEntriesType
                        }
                        dialog.dismiss()
                    }
                    .show()
            }
        }

        mRoot.findViewById<ImageButton>(R.id.search_button).apply {
            setOnClickListener {
                search()
            }
        }

        refreshEntries()

        return mRoot
    }

    private fun search() : Boolean {
        val imm = activity!!.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        if (imm.isActive) {
            imm.hideSoftInputFromWindow(activity!!.currentFocus!!.windowToken, InputMethodManager.HIDE_NOT_ALWAYS)
        }

        val editText = mRoot.findViewById<EditText>(R.id.search_query)
        val newQuery = editText.text.toString()

        return if (newQuery == mQuery) false
        else {
            mQuery = newQuery
            refreshEntries()
            true
        }
    }

    override fun refreshEntries() {
        mRoot.findViewById<Toolbar>(R.id.toolbar).apply {
            title = "検索($mSearchType): $mQuery"
        }
        super.refreshEntries("エントリ検索失敗") { offset ->
            HatenaClient.searchEntriesAsync(mQuery, mSearchType, mEntriesType, of = offset)
        }
    }
}
