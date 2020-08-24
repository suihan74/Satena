package com.suihan74.satena.scenes.entries2

import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentPagerAdapter
import com.suihan74.hatenaLib.BookmarkResult
import com.suihan74.hatenaLib.Entry
import com.suihan74.utilities.map

class EntriesTabAdapter(
    private val container: ViewGroup,
    private val fragment: EntriesFragment
) : FragmentPagerAdapter(fragment.childFragmentManager, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT) {

    override fun getItem(position: Int) : Fragment =
        EntriesTabFragment.createInstance(fragment.viewModelKey, fragment.category, position)

    override fun getPageTitle(position: Int): CharSequence? =
        fragment.getTabTitle(position)

    override fun getCount() = fragment.tabCount

    /** すべてのタブのリストを再構成する */
    fun reloadLists() {
        map<EntriesTabFragment>(container) {
            it.reload()
        }
    }

    /** すべてのタブのリストを再構成する(取得を行わない単なる再配置) */
    fun refreshLists() {
        map<EntriesTabFragment>(container) {
            it.refreshList()
        }
    }

    /** エントリに付けたブクマを削除 */
    fun removeBookmark(entry: Entry) {
        map<EntriesTabFragment>(container) {
            it.removeBookmark(entry)
        }
    }

    /** エントリに付けたブクマを更新する */
    fun updateBookmark(entry: Entry, bookmarkResult: BookmarkResult) {
        map<EntriesTabFragment>(container) {
            it.updateBookmark(entry, bookmarkResult)
        }
    }
}
