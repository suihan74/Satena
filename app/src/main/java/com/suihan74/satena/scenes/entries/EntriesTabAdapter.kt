package com.suihan74.satena.scenes.entries

import android.util.Log
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentPagerAdapter
import androidx.viewpager.widget.ViewPager
import com.suihan74.HatenaLib.*
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
        val issue = this@EntriesTabAdapter.fragment.currentIssue
        try {
            val entries =
                if (issue == null) {
                    getEntries(tabPosition)
                }
                else {
                    getEntries(issue, tabPosition)
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
            if (initializedCount >= count && context is EntriesActivity) {
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

    suspend fun getEntries(tabPosition: Int, offset: Int? = null) : List<Entry> {
        val pos = tabPosition + if (category == Category.MyBookmarks) EntriesTabType.MYBOOKMARKS.int else 0
        return when (EntriesTabType.fromInt(pos)) {
            EntriesTabType.POPULAR -> HatenaClient.getEntriesAsync(EntriesType.Hot, category.toApiCategory(), of = offset).await()
            EntriesTabType.RECENT -> HatenaClient.getEntriesAsync(EntriesType.Recent, category.toApiCategory(), of = offset).await()
            EntriesTabType.MYBOOKMARKS -> {
                val searchQuery = fragment.searchQuery
                if (searchQuery.isNullOrBlank()) {
                    HatenaClient.getMyBookmarkedEntriesAsync(of = offset).await()
                }
                else {
                    val regexBoundary = Regex("""^%s+|%s+$""")
                    val regex = Regex("""%s+""")
                    val query = searchQuery
                        .replace(regexBoundary, "")
                        .replace(regex, "+")
                    HatenaClient.searchMyEntriesAsync(query, SearchType.Text, of = offset).await()
                }
            }
            EntriesTabType.READLATER -> HatenaClient.searchMyEntriesAsync("あとで読む", SearchType.Tag, of = offset).await()
        }
    }

    suspend fun getEntries(issue: Issue, tabPosition: Int, offset: Int? = null) : List<Entry> {
        val pos = tabPosition + if (category == Category.MyBookmarks) EntriesTabType.MYBOOKMARKS.int else 0
        return when (EntriesTabType.fromInt(pos)) {
            EntriesTabType.POPULAR ->
                HatenaClient.getEntriesAsync(EntriesType.Hot, issue, of = offset).await()

            EntriesTabType.RECENT ->
                HatenaClient.getEntriesAsync(EntriesType.Recent, issue, of = offset).await()

            else -> throw IllegalStateException()
        }
    }
}
