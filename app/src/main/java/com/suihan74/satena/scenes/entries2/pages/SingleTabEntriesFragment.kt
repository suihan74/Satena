package com.suihan74.satena.scenes.entries2.pages

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.bottomappbar.BottomAppBar
import com.google.android.material.tabs.TabLayout
import com.suihan74.hatenaLib.BookmarkResult
import com.suihan74.hatenaLib.Entry
import com.suihan74.satena.R
import com.suihan74.satena.scenes.entries2.EntriesActivity
import com.suihan74.satena.scenes.entries2.EntriesFragment
import com.suihan74.satena.scenes.entries2.EntriesTabFragmentBase
import com.suihan74.satena.scenes.entries2.UserBottomItem

abstract class SingleTabEntriesFragment : EntriesFragment() {
    companion object {
        const val TAG_CONTENT_FRAGMENT = "TAG_CONTENT_FRAGMENT"
    }

    //////////////////////////////////////////////////

    /** コンテンツ部分に表示するフラグメントを生成する */
    abstract fun generateContentFragment(viewModelKey: String) : EntriesTabFragmentBase

    //////////////////////////////////////////////////

    /** コンテンツ本体のフラグメントを取得する */
    private val contentFragment : EntriesTabFragmentBase?
        get() = childFragmentManager.findFragmentByTag(TAG_CONTENT_FRAGMENT) as? EntriesTabFragmentBase

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        super.onCreateView(inflater, container, savedInstanceState)
        val root = inflater.inflate(R.layout.fragment_entries2_single, container, false)

        if (savedInstanceState == null) {
            val fragment = generateContentFragment(viewModelKey)
            childFragmentManager.beginTransaction()
                .replace(R.id.content_layout, fragment, TAG_CONTENT_FRAGMENT)
                .commitAllowingStateLoss()
        }

        return root
    }

    override fun refreshLists() {
        contentFragment?.refreshList()
    }

    override fun reloadLists() {
        contentFragment?.reload()
    }

    override fun removeBookmark(entry: Entry) {
        contentFragment?.removeBookmark(entry)
    }

    /** エントリに付けたブクマを更新する */
    override fun updateBookmark(entry: Entry, bookmarkResult: BookmarkResult) {
        contentFragment?.updateBookmark(entry, bookmarkResult)
    }

    /** EntriesActivityのタブと下部アプリバーをこのフラグメントの情報で更新する */
    override fun updateActivityAppBar(activity: EntriesActivity, tabLayout: TabLayout, bottomAppBar: BottomAppBar?) : Boolean {
        // 下部メニューの作成
        activity.setOnBottomMenuItemClickListener { item ->
            when (item) {
                UserBottomItem.SCROLL_TO_TOP -> {
                    contentFragment?.scrollToTop()
                    activity.showAppBar()
                }

                else -> {}
            }
        }
        return false
    }
}
