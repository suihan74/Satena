package com.suihan74.satena.scenes.entries2.pages

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.suihan74.hatenaLib.BookmarkResult
import com.suihan74.hatenaLib.Entry
import com.suihan74.satena.R
import com.suihan74.satena.scenes.entries2.EntriesFragment
import com.suihan74.satena.scenes.entries2.EntriesTabFragmentBase

abstract class SingleTabEntriesFragment : EntriesFragment() {

    companion object {
        /** コンテンツフラグメントのタグ */
        private const val FRAGMENT_MAIN_CONTENT = "FRAGMENT_MAIN_CONTENT"
    }

    //////////////////////////////////////////////////

    /** コンテンツ部分に表示するフラグメントを生成する */
    abstract fun generateContentFragment(viewModelKey: String) : EntriesTabFragmentBase

    //////////////////////////////////////////////////


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
                .replace(R.id.content_layout, fragment, FRAGMENT_MAIN_CONTENT)
                .commitAllowingStateLoss()
        }

        return root
    }

    override fun refreshLists() {
        val fragment = childFragmentManager.findFragmentByTag(FRAGMENT_MAIN_CONTENT) as? EntriesTabFragmentBase ?: return
        fragment.refreshList()
    }

    override fun removeBookmark(entry: Entry) {
        val fragment = childFragmentManager.findFragmentByTag(FRAGMENT_MAIN_CONTENT) as? EntriesTabFragmentBase ?: return
        fragment.removeBookmark(entry)
    }

    /** エントリに付けたブクマを更新する */
    override fun updateBookmark(entry: Entry, bookmarkResult: BookmarkResult) {
        val fragment = childFragmentManager.findFragmentByTag(FRAGMENT_MAIN_CONTENT) as? EntriesTabFragmentBase ?: return
        fragment.updateBookmark(entry, bookmarkResult)
    }
}
