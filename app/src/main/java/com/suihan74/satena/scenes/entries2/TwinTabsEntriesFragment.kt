package com.suihan74.satena.scenes.entries2

import android.os.Bundle
import android.view.*
import androidx.databinding.DataBindingUtil
import com.google.android.material.tabs.TabLayout
import com.suihan74.satena.R
import com.suihan74.satena.databinding.FragmentEntries2Binding
import kotlinx.android.synthetic.main.fragment_entries2.view.*

class TwinTabsEntriesFragment : EntriesFragment() {
    companion object {
        fun createInstance() = TwinTabsEntriesFragment()
    }

    // タブ管理に関する設定

    private val tabTitles = arrayOf(
        R.string.entries_tab_hot,
        R.string.entries_tab_recent
    )
    override fun getTabTitleId(position: Int) = tabTitles[position]
    override val tabCount = tabTitles.size

    // タブ設定に関する設定ここまで

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

        val view = binding.root

        // タブ設定
        view.entries_tab_pager.adapter = EntriesTabAdapter(this)
        view.main_tab_layout.apply {
            setupWithViewPager(view.entries_tab_pager)
            addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
                override fun onTabSelected(tab: TabLayout.Tab?) {}
                override fun onTabUnselected(p0: TabLayout.Tab?) {}
                override fun onTabReselected(tab: TabLayout.Tab?) {
                    val adapter = view.entries_tab_pager.adapter as EntriesTabAdapter
                    val position = tab!!.position
                    val fragment = adapter.instantiateItem(view.entries_tab_pager, position) as? EntriesTabFragment
                    fragment?.scrollToTop()
                }
            })
        }

        return view
    }
}
