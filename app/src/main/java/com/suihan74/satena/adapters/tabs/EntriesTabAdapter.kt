package com.suihan74.satena.adapters.tabs

import android.util.Log
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentPagerAdapter
import androidx.viewpager.widget.ViewPager
import com.suihan74.HatenaLib.EntriesType
import com.suihan74.HatenaLib.Entry
import com.suihan74.HatenaLib.HatenaClient
import com.suihan74.HatenaLib.SearchType
import com.suihan74.satena.activities.MainActivity
import com.suihan74.satena.fragments.EntriesFragment
import com.suihan74.satena.fragments.EntriesTabFragment
import com.suihan74.satena.models.Category
import com.suihan74.satena.models.EntriesTabType
import com.suihan74.utilities.showToast
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll

class EntriesTabAdapter(
    private val fragment: EntriesFragment,
    var category : Category = Category.All
) : FragmentPagerAdapter(fragment.childFragmentManager, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT) {

    private var initializedCount = 0
    private var failureToastShowed = false

    fun refreshTabAsync(fragment: EntriesTabFragment, tabPosition: Int) = fragment.async(Dispatchers.Main) {
        val context = fragment.context ?: return@async
        try {
            val entries = when (category) {
                Category.MyBookmarks -> when (EntriesTabType.fromInt(tabPosition + EntriesTabType.MYBOOKMARKS.int)) {
                    EntriesTabType.MYBOOKMARKS -> HatenaClient.getMyBookmarkedEntriesAsync().await()
                    EntriesTabType.READLATER -> HatenaClient.searchMyEntriesAsync("あとで読む", SearchType.Tag).await()
                    else -> throw RuntimeException("unknown tab")
                }

                else -> when (EntriesTabType.fromInt(tabPosition)) {
                    EntriesTabType.POPULAR -> HatenaClient.getEntriesAsync(EntriesType.Hot, category.toApiCategory()).await()
                    EntriesTabType.RECENT -> HatenaClient.getEntriesAsync(EntriesType.Recent, category.toApiCategory()).await()
                    else -> throw RuntimeException("unknown tab")
                }
            }
            fragment.setEntries(entries)
        }
        catch (e: Exception) {
            Log.d("FailedToFetchEntries", e.message)
            fragment.setEntries(emptyList())
            if (!failureToastShowed) {
                failureToastShowed = true
                context.showToast("エントリーリスト取得失敗")
            }
        }
        finally {
            initializedCount++
            if (initializedCount >= count && context is MainActivity) {
                context.hideProgressBar()
            }
        }
    }

    fun setCategory(viewPager: ViewPager, category: Category) {
        this.category = category
        for (position in 0 until count) {
            val fragment = findFragment(viewPager, position)
            fragment?.category = category
        }
    }

    suspend fun refreshAllTab(viewPager: ViewPager) {
        initializedCount = 0
        val tasks = ArrayList<Deferred<Unit>>()
        for (position in 0 until count) {
            val fragment = findFragment(viewPager, position)
            if (fragment != null) {
                tasks.add(refreshTabAsync(fragment, position))
            }
        }
        tasks.awaitAll()
    }

    override fun getItem(tabPosition: Int) : Fragment {
        val entriesTabFragment = EntriesTabFragment.createInstance(tabPosition, category)

        refreshTabAsync(entriesTabFragment, tabPosition).start()

        return entriesTabFragment
    }

    fun findFragment(viewPager: ViewPager, position: Int) : EntriesTabFragment? {
        return try {
            instantiateItem(viewPager, position) as? EntriesTabFragment
        }
        catch (e: Exception) {
            null
        }
    }

    override fun getPageTitle(position: Int): CharSequence? {
        val context = fragment.context ?: return null
        val pos = position + if (category == Category.MyBookmarks) EntriesTabType.MYBOOKMARKS.int else 0
        return context.getText(EntriesTabType.fromInt(pos).textId)
    }

    override fun getCount() = 2

    fun getEntriesAsync(tabPosition: Int, offset: Int? = null) : Deferred<List<Entry>> {
        val pos = tabPosition + if (category == Category.MyBookmarks) EntriesTabType.MYBOOKMARKS.int else 0
        return when (EntriesTabType.fromInt(pos)) {
            EntriesTabType.POPULAR -> HatenaClient.getEntriesAsync(EntriesType.Hot, category.toApiCategory(), of = offset)
            EntriesTabType.RECENT -> HatenaClient.getEntriesAsync(EntriesType.Recent, category.toApiCategory(), of = offset)
            EntriesTabType.MYBOOKMARKS -> {
                val searchQuery = fragment.searchQuery
                if (searchQuery.isNullOrBlank()) {
                    HatenaClient.getMyBookmarkedEntriesAsync(of = offset)
                }
                else {
                    val regexBoundary = Regex("""^%s+|%s+$""")
                    val regex = Regex("""%s+""")
                    val query = searchQuery
                        .replace(regexBoundary, "")
                        .replace(regex, "+")
                    HatenaClient.searchMyEntriesAsync(query, SearchType.Text, of = offset)
                }
            }
            EntriesTabType.READLATER -> HatenaClient.searchMyEntriesAsync("あとで読む", SearchType.Tag, of = offset)
        }
    }
}
