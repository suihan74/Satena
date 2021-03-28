package com.suihan74.satena.scenes.entries2.pages

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.bottomappbar.BottomAppBar
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.suihan74.hatenaLib.BookmarkResult
import com.suihan74.hatenaLib.Entry
import com.suihan74.satena.R
import com.suihan74.satena.databinding.FragmentEntries2Binding
import com.suihan74.satena.models.PreferenceKey
import com.suihan74.satena.scenes.entries2.*
import com.suihan74.utilities.SafeSharedPreferences
import com.suihan74.utilities.extensions.ContextExtensions.showToast
import com.suihan74.utilities.extensions.alsoAs
import com.suihan74.utilities.extensions.setOnTabLongClickListener
import kotlin.math.min

abstract class MultipleTabsEntriesFragment : EntriesFragment() {
    private var _binding : FragmentEntries2Binding? = null
    protected val binding get() = _binding!!

    private val entriesTabPager : ViewPager2
        get() = binding.entriesTabPager

    private val entriesTabAdapter : EntriesTabAdapter
        get() = entriesTabPager.adapter as EntriesTabAdapter

    protected val contentLayout : ViewGroup
        get() = binding.contentLayout

    // ------ //

    private val entriesActivity
        get() = requireActivity() as EntriesActivity

    private val activityViewModel
        get() = entriesActivity.viewModel

    // ------ //

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        super.onCreateView(inflater, container, savedInstanceState)

        _binding = FragmentEntries2Binding.inflate(inflater, container, false).apply {
            lifecycleOwner = this@MultipleTabsEntriesFragment
            vm = viewModel
        }

        // タブ設定
        val tabAdapter = EntriesTabAdapter(this)
        binding.entriesTabPager.adapter = tabAdapter
        binding.entriesTabPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                activityViewModel.currentTabPosition.value = position
            }
        })

        // タブ初期選択
        if (savedInstanceState == null) {
            val prefs = SafeSharedPreferences.create<PreferenceKey>(context)
            prefs.getObject<EntriesDefaultTabSettings>(PreferenceKey.ENTRIES_DEFAULT_TABS)!!.also { settings ->
                val category = viewModel.category.value!!
                val setting = settings.getOrDefault(category)
                val tabPosition =
                    if (setting == EntriesDefaultTabSettings.MAINTAIN) activityViewModel.currentTabPosition.value
                        ?: 0
                    else setting
                binding.entriesTabPager.setCurrentItem(
                    min(tabPosition, tabAdapter.itemCount - 1),
                    false
                )
            }
        }

        return binding.root
    }

    override fun onResume() {
        super.onResume()
        activityViewModel.currentTabPosition.value = binding.entriesTabPager.currentItem
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    /** 全てのタブのリストを再構成する */
    override fun reloadLists() {
        entriesTabAdapter.reloadLists()
    }

    /** リストを再構成する(取得を行わない単なる再配置) */
    override fun refreshLists() {
        entriesTabAdapter.refreshLists()
    }

    /** エントリに付けたブクマを削除 */
    override fun removeBookmark(entry: Entry) {
        entriesTabAdapter.removeBookmark(entry)
    }

    /** エントリに付けたブクマを更新する */
    override fun updateBookmark(entry: Entry, bookmarkResult: BookmarkResult) {
        entriesTabAdapter.updateBookmark(entry, bookmarkResult)
    }

    /** 与えられたタブのコンテンツを最上までスクロールする */
    private fun scrollContentToTop(tabPosition: Int) {
        entriesTabAdapter.findFragment(tabPosition).alsoAs<EntriesTabFragment> {
            it.scrollToTop()
        }
    }

    /** EntriesActivityのタブと下部アプリバーをこのフラグメントの情報で更新する */
    override fun updateActivityAppBar(
        activity: EntriesActivity,
        tabLayout: TabLayout,
        bottomAppBar: BottomAppBar?
    ) : Boolean {
        // タブ項目のクリックイベント
        val listener = object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {}
            override fun onTabUnselected(p0: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {
                val position = tab!!.position
                scrollContentToTop(position)
            }
        }

        // タブを長押しで最初に表示するタブを変更
        val longClickListener : (Int)->Boolean = l@ { idx ->
            val category = viewModel.category.value!!
            if (!category.displayInList || !category.willBeHome) return@l false

            val prefs = SafeSharedPreferences.create<PreferenceKey>(context)
            val isOn = prefs.getBoolean(PreferenceKey.ENTRIES_CHANGE_HOME_BY_LONG_TAPPING_TAB)
            if (!isOn) return@l false

            val homeCategoryInt = prefs.getInt(PreferenceKey.ENTRIES_HOME_CATEGORY)
            val initialTab = prefs.getInt(PreferenceKey.ENTRIES_INITIAL_TAB)

            if (category.id != homeCategoryInt || initialTab != idx) {
                val tabText = viewModel.getTabTitle(requireContext(), idx)
                prefs.edit {
                    put(PreferenceKey.ENTRIES_HOME_CATEGORY, category.id)
                    put(PreferenceKey.ENTRIES_INITIAL_TAB, idx)
                }
                activity.showToast(
                    R.string.msg_entries_initial_tab_changed,
                    getString(category.textId),
                    tabText
                )
            }
            return@l true
        }

        TabLayoutMediator(tabLayout, entriesTabPager) { tab, position ->
            tab.text = getTabTitle(position)
        }.attach()
        tabLayout.addOnTabSelectedListener(listener)
        tabLayout.setOnTabLongClickListener(longClickListener)

        return true
    }

    override fun scrollToTop() {
        entriesTabPager.currentItem.alsoAs<Int> {
            scrollContentToTop(it)
        }
    }
}
