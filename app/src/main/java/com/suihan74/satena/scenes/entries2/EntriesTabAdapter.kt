package com.suihan74.satena.scenes.entries2

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.suihan74.hatenaLib.BookmarkResult
import com.suihan74.hatenaLib.Entry
import com.suihan74.utilities.extensions.alsoAs

class EntriesTabAdapter(
    private val fragment: EntriesFragment
) : FragmentStateAdapter(fragment) {

    override fun createFragment(position: Int) : Fragment =
        EntriesTabFragment.createInstance(fragment.viewModelKey, fragment.category, position)

    override fun getItemCount() = fragment.tabCount

    /** すべてのタブのリストを再構成する */
    fun reloadLists() {
        map {
            it.reload()
        }
    }

    /** すべてのタブのリストを再構成する(取得を行わない単なる再配置) */
    fun refreshLists() {
        map {
            it.refreshList()
        }
    }

    /** エントリに付けたブクマを削除 */
    fun removeBookmark(entry: Entry) {
        map {
            it.removeBookmark(entry)
        }
    }

    /** エントリに付けたブクマを更新する */
    fun updateBookmark(entry: Entry, bookmarkResult: BookmarkResult?) {
        map {
            it.updateBookmark(entry, bookmarkResult)
        }
    }

    // ------ //

    fun findFragment(position: Int) : Fragment? =
        fragment.childFragmentManager.findFragmentByTag("f$position")

    private fun map(action: (EntriesTabFragment)->Unit) {
        repeat(itemCount) { i ->
            findFragment(i)?.alsoAs<EntriesTabFragment> { f ->
                action(f)
            }
        }
    }
}
