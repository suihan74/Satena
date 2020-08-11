package com.suihan74.satena.scenes.entries2.pages

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import com.google.android.material.tabs.TabLayout
import com.suihan74.hatenaLib.BookmarkResult
import com.suihan74.hatenaLib.Entry
import com.suihan74.satena.R
import com.suihan74.satena.databinding.FragmentEntries2Binding
import com.suihan74.satena.models.PreferenceKey
import com.suihan74.satena.scenes.entries2.EntriesFragment
import com.suihan74.satena.scenes.entries2.EntriesTabAdapter
import com.suihan74.satena.scenes.entries2.EntriesTabFragment
import com.suihan74.utilities.SafeSharedPreferences
import com.suihan74.utilities.setOnTabLongClickListener
import com.suihan74.utilities.showToast
import kotlinx.android.synthetic.main.fragment_entries2.view.*

abstract class TwinTabsEntriesFragment : EntriesFragment() {
    private var binding : FragmentEntries2Binding? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        super.onCreateView(inflater, container, savedInstanceState)

        val binding = DataBindingUtil.inflate<FragmentEntries2Binding>(inflater, R.layout.fragment_entries2, container, false).apply {
            lifecycleOwner = this@TwinTabsEntriesFragment
            vm = viewModel
        }
        this.binding = binding

        val view = binding.root

        // タブ設定
        view.entries_tab_pager.adapter = EntriesTabAdapter(view.entries_tab_pager, this)

        // タブ初期選択
        val prefs = SafeSharedPreferences.create<PreferenceKey>(context)
        val initialTabPosition = prefs.getInt(PreferenceKey.ENTRIES_INITIAL_TAB)
        view.entries_tab_pager.setCurrentItem(initialTabPosition, false)

        return view
    }

    /** 全てのタブのリストを再構成する */
    override fun refreshLists() {
        val adapter = binding?.entriesTabPager?.adapter as? EntriesTabAdapter ?: return
        adapter.refreshLists()
    }

    /** エントリに付けたブクマを削除 */
    override fun removeBookmark(entry: Entry) {
        val adapter = binding?.entriesTabPager?.adapter as? EntriesTabAdapter ?: return
        adapter.removeBookmark(entry)
    }

    /** エントリに付けたブクマを更新する */
    override fun updateBookmark(entry: Entry, bookmarkResult: BookmarkResult) {
        val adapter = binding?.entriesTabPager?.adapter as? EntriesTabAdapter ?: return
        adapter.updateBookmark(entry, bookmarkResult)
    }

    override fun updateTabsLayout(tabLayout: TabLayout) : Boolean {
        val entriesTabPager = view?.entries_tab_pager ?: return false

        val listener = object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {}
            override fun onTabUnselected(p0: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {
                val adapter = entriesTabPager.adapter as EntriesTabAdapter
                val position = tab!!.position
                val fragment = adapter.instantiateItem(entriesTabPager, position) as? EntriesTabFragment
                fragment?.scrollToTop()
            }
        }

        // タブを長押しで最初に表示するタブを変更
        val longClickListener : (Int)->Boolean = l@ { idx ->
            val category = viewModel.category.value!!
            if (!category.displayInList) return@l false

            val prefs = SafeSharedPreferences.create<PreferenceKey>(context)
            val isOn = prefs.getBoolean(PreferenceKey.ENTRIES_CHANGE_HOME_BY_LONG_TAPPING_TAB)
            if (!isOn) return@l false

            val homeCategoryInt = prefs.getInt(PreferenceKey.ENTRIES_HOME_CATEGORY)
            val initialTab = prefs.getInt(PreferenceKey.ENTRIES_INITIAL_TAB)

            if (category.ordinal != homeCategoryInt || initialTab != idx) {
                val tabText = viewModel.getTabTitle(requireContext(), idx)
                prefs.edit {
                    put(PreferenceKey.ENTRIES_HOME_CATEGORY, category.ordinal)
                    put(PreferenceKey.ENTRIES_INITIAL_TAB, idx)
                }
                activity?.showToast(
                    R.string.msg_entries_initial_tab_changed,
                    getString(category.textId),
                    tabText
                )
            }
            return@l true
        }

        tabLayout.setupWithViewPager(entriesTabPager)
        tabLayout.addOnTabSelectedListener(listener)
        tabLayout.setOnTabLongClickListener(longClickListener)

        return true
    }
}
