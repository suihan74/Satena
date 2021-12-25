package com.suihan74.satena.scenes.entries2.pages

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.bottomappbar.BottomAppBar
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.suihan74.hatenaLib.BookmarkResult
import com.suihan74.hatenaLib.Entry
import com.suihan74.satena.databinding.FragmentEntries2Binding
import com.suihan74.satena.models.Category
import com.suihan74.satena.models.PreferenceKey
import com.suihan74.satena.scenes.entries2.*
import com.suihan74.utilities.SafeSharedPreferences
import com.suihan74.utilities.extensions.alsoAs
import com.suihan74.utilities.extensions.setOnTabLongClickListener
import com.suihan74.utilities.extensions.touchSlop
import kotlin.math.min

abstract class MultipleTabsEntriesFragment : EntriesFragment() {
    private var _binding : FragmentEntries2Binding? = null
    protected val binding get() = _binding!!

    private val entriesTabPager : ViewPager2?
        get() = _binding?.entriesTabPager

    private val entriesTabAdapter : EntriesTabAdapter?
        get() = entriesTabPager?.adapter as? EntriesTabAdapter

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
        initializeTabPager(binding.entriesTabPager, savedInstanceState == null)

        return binding.root
    }

    override fun onResume() {
        super.onResume()
        activityViewModel.currentTabPosition.value = binding.entriesTabPager.currentItem
        resetPagerSensitivity(binding.entriesTabPager)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun initializeTabPager(pager: ViewPager2, firstLaunched: Boolean) {
        val tabAdapter = EntriesTabAdapter(this)
        pager.adapter = tabAdapter
        pager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                activityViewModel.currentTabPosition.value = position
            }
        })

        // タブ初期選択
        if (firstLaunched) {
            val prefs = SafeSharedPreferences.create<PreferenceKey>(context)
            prefs.getObject<EntriesDefaultTabSettings>(PreferenceKey.ENTRIES_DEFAULT_TABS)!!.also { settings ->
                val category = viewModel.category.value!!
                val setting = settings.getOrDefault(category)
                val tabPosition =
                    if (setting == EntriesDefaultTabSettings.MAINTAIN) activityViewModel.currentTabPosition.value ?: 0
                    else setting
                pager.setCurrentItem(
                    min(tabPosition, tabAdapter.itemCount - 1),
                    false
                )
            }
        }
    }

    private var defaultTouchSlop : Int? = null
    private fun resetPagerSensitivity(pager: ViewPager2) {
        runCatching {
            if (defaultTouchSlop == null) defaultTouchSlop = pager.touchSlop
            val scale = 1 / (activityViewModel.pagerScrollSensitivity)
            pager.touchSlop = (defaultTouchSlop!! * scale).toInt()
        }.onFailure {
            Log.e("viewPager2", Log.getStackTraceString(it))
        }
    }

    // ------ //

    /** 全てのタブのリストを再構成する */
    override fun reloadLists() {
        entriesTabAdapter?.reloadLists()
    }

    /** リストを再構成する(取得を行わない単なる再配置) */
    override fun refreshLists() {
        entriesTabAdapter?.refreshLists()
    }

    /** エントリに付けたブクマを削除 */
    override fun removeBookmark(entry: Entry) {
        entriesTabAdapter?.removeBookmark(entry)
    }

    /** エントリに付けたブクマを更新する */
    override fun updateBookmark(entry: Entry, bookmarkResult: BookmarkResult?) {
        entriesTabAdapter?.updateBookmark(entry, bookmarkResult)
    }

    /** 与えられたタブのコンテンツを最上までスクロールする */
    private fun scrollContentToTop(tabPosition: Int) {
        entriesTabAdapter?.findFragment(tabPosition).alsoAs<EntriesTabFragment> {
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
            if (!activityViewModel.repository.changeHomeByLongTap) return@l false

            val category = viewModel.category.value!!
            if (category == Category.Memorial15th) return@l false

            val tab = EntriesTabType.fromTabOrdinal(idx, category)
            activityViewModel.openDefaultTabSettingDialog(requireContext(), category, tab, childFragmentManager)
            return@l true
        }

        entriesTabPager?.let { tabPager ->
            TabLayoutMediator(tabLayout, tabPager) { tab, position ->
                tab.text = getTabTitle(position)
            }.attach()
        }
        tabLayout.addOnTabSelectedListener(listener)
        tabLayout.setOnTabLongClickListener(longClickListener)

        return true
    }

    override fun scrollToTop() {
        entriesTabPager?.currentItem.alsoAs<Int> {
            scrollContentToTop(it)
        }
    }
}
