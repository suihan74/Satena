package com.suihan74.satena.fragments

import android.app.AlertDialog
import android.os.Bundle
import android.view.*
import androidx.appcompat.widget.SearchView
import com.suihan74.HatenaLib.EntriesType
import com.suihan74.HatenaLib.HatenaClient
import com.suihan74.HatenaLib.SearchType
import com.suihan74.satena.R
import com.suihan74.satena.activities.MainActivity

class SearchEntriesFragment : MultipurposeSingleTabEntriesFragment() {
    private lateinit var mRoot : View
    private lateinit var mQuery : String
    private lateinit var mSearchType : SearchType
    private var mEntriesType : EntriesType = EntriesType.Recent

    override val title: String
        get() = mQuery
    override val isSearchViewVisible: Boolean = true

    companion object {
        fun createInstance(query: String, searchType: SearchType) = SearchEntriesFragment().apply {
            mQuery = query
            mSearchType = searchType
        }

        fun createInstance() = SearchEntriesFragment().apply {
            mQuery = ""
            mSearchType = SearchType.Text
        }

        private const val BUNDLE_QUERY = "mQuery"
        private const val BUNDLE_SEARCH_TYPE = "mSearchType"
        private const val BUNDLE_ENTRIES_TYPE = "mEntriesType"
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(BUNDLE_QUERY, mQuery)
        outState.putInt(BUNDLE_SEARCH_TYPE, mSearchType.int)
        outState.putInt(BUNDLE_ENTRIES_TYPE, mEntriesType.int)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        mRoot = super.onCreateView(inflater, container, savedInstanceState)!!

        savedInstanceState?.let {
            mQuery = it.getString(BUNDLE_QUERY)!!
            mSearchType = SearchType.fromInt(it.getInt(BUNDLE_SEARCH_TYPE))
            mEntriesType = EntriesType.fromInt(it.getInt(BUNDLE_ENTRIES_TYPE))
        }

        setHasOptionsMenu(true)
        if (mQuery.isNotBlank()) {
            refreshEntries()
        }

        return mRoot
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.search_entries, menu)

        val activity = activity as? MainActivity

        // 検索クエリエディタ
        activity?.searchView?.apply {
            val defaultQuery = "検索クエリ"
            visibility = View.VISIBLE
            queryHint = defaultQuery
            isSubmitButtonEnabled = false
            setOnQueryTextListener(object : SearchView.OnQueryTextListener {
                override fun onQueryTextChange(newText: String?): Boolean {
                    return false
                }

                override fun onQueryTextSubmit(query: String?): Boolean {
                    if (search(query)) {
                        queryHint = query ?: defaultQuery
                    }
                    return false
                }
            })

            if (mQuery.isNotBlank()) {
                setQuery(mQuery, true)
            }

            isIconified = false
            requestFocus()
            requestFocusFromTouch()

        }

        // テキスト/タグ
        menu.findItem(R.id.search_type)?.apply {
            // 検索対象(tag/text)を切り替え
            fun setSearchTypeText(s: SearchType) : String = when (s) {
                SearchType.Text -> getString(R.string.search_type_text)
                SearchType.Tag -> getString(R.string.search_type_tag)
            }

            title = setSearchTypeText(mSearchType)
            setOnMenuItemClickListener {
                val items = SearchType.values().map { setSearchTypeText(it) }.toTypedArray()
                AlertDialog.Builder(context!!, R.style.AlertDialogStyle)
                    .setTitle("検索対象")
                    .setNegativeButton("Cancel", null)
                    .setSingleChoiceItems(items, SearchType.values().indexOf(mSearchType)) { dialog, which ->
                        val newSearchType = SearchType.values()[which]
                        title = setSearchTypeText(newSearchType)
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
                return@setOnMenuItemClickListener true
            }
        }

        // 新着/人気
        menu.findItem(R.id.search_order)?.apply {
            fun setEntriesTypeText(e: EntriesType) : String = when (e) {
                EntriesType.Recent -> getString(R.string.entries_tab_recent)
                EntriesType.Hot -> getString(R.string.entries_tab_hot)
            }

            title = setEntriesTypeText(mEntriesType)
            setOnMenuItemClickListener {
                val items = EntriesType.values().map { setEntriesTypeText(it) }.toTypedArray()
                AlertDialog.Builder(context!!, R.style.AlertDialogStyle)
                    .setTitle("検索順")
                    .setNegativeButton("Cancel", null)
                    .setSingleChoiceItems(items, EntriesType.values().indexOf(mEntriesType)) { dialog, which ->
                        val newEntriesType = EntriesType.values()[which]
                        title = setEntriesTypeText(newEntriesType)
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
                return@setOnMenuItemClickListener true
            }
        }
    }

    private fun search(query: String? = null) : Boolean {
/*
        val imm = activity?.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
        val currentFocus = activity?.currentFocus

        if (imm?.isActive == true && currentFocus != null) {
            imm.hideSoftInputFromWindow(currentFocus.windowToken, InputMethodManager.HIDE_NOT_ALWAYS)
        }
*/
        if (query == null) return false

        mQuery = query
        refreshEntries()
        return true
    }

    override fun refreshEntries() {
        (activity as? MainActivity)?.updateToolbar()
        super.refreshEntries("エントリ検索失敗") { offset ->
            HatenaClient.searchEntriesAsync(mQuery, mSearchType, mEntriesType, of = offset)
        }
    }
}
