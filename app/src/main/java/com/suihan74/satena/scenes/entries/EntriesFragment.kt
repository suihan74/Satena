package com.suihan74.satena.scenes.entries

import android.os.Bundle
import android.transition.AutoTransition
import android.transition.TransitionSet
import android.util.Log
import android.view.*
import android.widget.Spinner
import androidx.appcompat.widget.SearchView
import androidx.viewpager.widget.ViewPager
import com.google.android.material.tabs.TabLayout
import com.suihan74.HatenaLib.Entry
import com.suihan74.HatenaLib.Issue
import com.suihan74.satena.R
import com.suihan74.satena.SatenaApplication
import com.suihan74.satena.models.Category
import com.suihan74.satena.models.EntriesTabType
import com.suihan74.satena.models.PreferenceKey
import com.suihan74.utilities.CoroutineScopeFragment
import com.suihan74.utilities.SafeSharedPreferences
import com.suihan74.utilities.setOnTabLongClickListener
import com.suihan74.utilities.showToast
import kotlinx.coroutines.*

class EntriesFragment : CoroutineScopeFragment() {
    override val title: String
        get() = SatenaApplication.instance.getString(currentCategory?.textId ?: 0)

    private lateinit var mEntriesTabPager : ViewPager

    private lateinit var mEntriesTabLayout : TabLayout
    private lateinit var mEntriesTabAdapter : EntriesTabAdapter

    private lateinit var mView : View

    /** 表示中のカテゴリ */
    var currentCategory : Category? = null
        private set

    /** 表示中の特集 */
    var currentIssue : Issue? = null
        private set

    /** マイブックマークの検索クエリ */
    var searchQuery : String? = null
        private set

    override val subtitle: String?
        get() = currentIssue?.name

    companion object {
        fun createInstance(category: Category) = EntriesFragment().apply {
            currentCategory = category
        }

        private const val BUNDLE_CATEGORY = "BUNDLE_CATEGORY"
        private const val BUNDLE_ISSUE = "BUNDLE_ISSUE"
        private const val BUNDLE_CURRENT_TAB = "BUNDLE_CURRENT_TAB"
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(BUNDLE_CATEGORY, mEntriesTabAdapter.category.ordinal)
        if (this::mEntriesTabPager.isInitialized) {
            outState.putInt(BUNDLE_CURRENT_TAB, mEntriesTabPager.currentItem)
            outState.putSerializable(BUNDLE_ISSUE, currentIssue)
        }
        else {
            val prefs = SafeSharedPreferences.create<PreferenceKey>(context!!)
            outState.putInt(BUNDLE_CURRENT_TAB, prefs.getInt(PreferenceKey.ENTRIES_INITIAL_TAB))
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enterTransition = TransitionSet().addTransition(AutoTransition())

        val activity = activity as EntriesActivity
        val category = currentCategory
                    ?: savedInstanceState?.let { Category.fromInt(it.getInt(BUNDLE_CATEGORY)) }
                    ?: activity.homeCategory

        currentCategory = category

        // エントリリスト用アダプタ作成
        mEntriesTabAdapter = EntriesTabAdapter(this, category)
    }


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        mView = inflater.inflate(R.layout.fragment_entries, container, false)

        // マイブックマーク画面ではツールバーに検索ボタンを表示する
        setHasOptionsMenu(currentCategory == Category.MyBookmarks || currentCategory?.hasIssues == true)

        val prefs = SafeSharedPreferences.create<PreferenceKey>(context!!)

        // タブの設定
        mEntriesTabPager = mView.findViewById(R.id.entries_tab_pager)
        mEntriesTabPager.adapter = mEntriesTabAdapter
        mEntriesTabLayout = mView.findViewById<TabLayout>(R.id.main_tab_layout).apply {
            setupWithViewPager(mEntriesTabPager)
            addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
                override fun onTabSelected(tab: TabLayout.Tab?) {
                    if (currentCategory == Category.MyBookmarks && tab != null) {
                        when(tab.position) {
                            EntriesTabType.MYBOOKMARKS.tabPosition ->
                                setHasOptionsMenu(true)

                            EntriesTabType.READ_LATER.tabPosition ->
                                setHasOptionsMenu(false)
                        }
                    }
                }
                override fun onTabUnselected(p0: TabLayout.Tab?) {}
                override fun onTabReselected(tab: TabLayout.Tab?) {
                    val fragment = mEntriesTabAdapter.findFragment(mEntriesTabPager, tab!!.position)
                    fragment?.scrollToTop()
                }
            })

            // タブを長押しで最初に表示するカテゴリ・タブを設定
            setOnTabLongClickListener { idx ->
                val currentCategory = currentCategory ?: return@setOnTabLongClickListener true
                val catKey = PreferenceKey.ENTRIES_HOME_CATEGORY
                val tabKey = PreferenceKey.ENTRIES_INITIAL_TAB
                if (prefs.getInt(catKey) != currentCategory.ordinal || prefs.getInt(tabKey) != idx) {
                    prefs.edit {
                        put(catKey, currentCategory.ordinal)
                        put(tabKey, idx)
                    }

                    val catText = title
                    val tabText = context.getString(EntriesTabType.fromInt(idx).textId)
                    context.showToast("${catText}カテゴリの${tabText}タブを最初に表示するようにしました")
                }
                return@setOnTabLongClickListener true
            }
        }

        if (savedInstanceState == null) {
            mEntriesTabPager.currentItem = prefs.getInt(PreferenceKey.ENTRIES_INITIAL_TAB)
        }
        else {
            val restoreTab = savedInstanceState.getInt(BUNDLE_CURRENT_TAB)
            mEntriesTabPager.currentItem = restoreTab
            currentIssue = savedInstanceState.getSerializable(BUNDLE_ISSUE) as? Issue

            launch(Dispatchers.Main) {
                mEntriesTabAdapter.refreshAllTab(mEntriesTabPager)
            }
        }

        return mView
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        when {
            currentCategory == Category.MyBookmarks -> inflateSearchMyEntriesMenu(menu, inflater)
            currentCategory?.hasIssues == true -> inflateIssuesMenu(menu, inflater)
        }
    }

    /** マイブックマークを検索する追加メニュー */
    private fun inflateSearchMyEntriesMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.search_my_entries, menu)

        (menu.findItem(R.id.search_view)?.actionView as? SearchView)?.run {
            isSubmitButtonEnabled = true
            queryHint = getString(R.string.hint_search_my_bookmarks)

            if (searchQuery != null) {
                setQuery(searchQuery, false)
                isIconified = false
            }

            setOnQueryTextListener(object : SearchView.OnQueryTextListener {
                override fun onQueryTextChange(newText: String?): Boolean = false
                override fun onQueryTextSubmit(query: String?): Boolean {
                    searchQuery = query
                    refreshEntriesTabs(Category.MyBookmarks)
                    return false
                }
            })

            setOnCloseListener {
                searchQuery = null
                refreshEntriesTabs(Category.MyBookmarks)
                return@setOnCloseListener false
            }
        }
    }

    /** カテゴリごとの特集を選択する追加メニュー */
    private fun inflateIssuesMenu(menu: Menu, inflater: MenuInflater) {
        val activity = activity as EntriesActivity
        val issues = activity.categoryEntries
            .firstOrNull { it.code == currentCategory!!.code }
            ?.issues
            ?: return
        val spinnerItems = issues.map { it.name }

        inflater.inflate(R.menu.spinner_issues, menu)

        (menu.findItem(R.id.spinner)?.actionView as? Spinner)?.run {
            initialize(activity, spinnerItems, R.drawable.spinner_allow_issues, getString(R.string.desc_issues_spinner)) { position ->
                val prevIssue = currentIssue
                currentIssue =
                    if (position == null) {
                        null
                    }
                    else {
                        val item = spinnerItems[position]
                        issues.firstOrNull { it.name == item }
                    }

                if (prevIssue != currentIssue) {
                    refreshEntriesTabs(currentCategory!!)
                }
            }

            if (currentIssue != null) {
                val currentIssueName = currentIssue!!.name
                val position = spinnerItems.indexOfFirst { it == currentIssueName }
                if (position >= 0) {
                    setSelection(position + 1)
                }
            }
        }
    }

    suspend fun refreshEntries(tabPosition: Int? = null, offset: Int? = null) : List<Entry> =
        when {
            currentIssue != null ->
                mEntriesTabAdapter.getEntries(currentIssue!!, tabPosition ?: mEntriesTabLayout.selectedTabPosition, offset)

            else ->
                mEntriesTabAdapter.getEntries(tabPosition ?: mEntriesTabLayout.selectedTabPosition, offset)
        }

    fun refreshEntriesTabs(category: Category) {
        val activity = activity as EntriesActivity
        activity.showProgressBar()
        activity.updateToolbar()

        val categoryChanged = currentCategory != category
        currentCategory = category
        mEntriesTabAdapter.setCategory(mEntriesTabPager, category)

        if (categoryChanged) {
            currentIssue = null
            setHasOptionsMenu(category == Category.MyBookmarks || category.hasIssues)
        }

        val tasks = (0 until mEntriesTabAdapter.count).map { tabPosition ->
            val tabFragment = mEntriesTabAdapter.findFragment(mEntriesTabPager, tabPosition)
            val tab = mEntriesTabLayout.getTabAt(tabPosition)!!

            mEntriesTabAdapter.setCategory(mEntriesTabPager, category)
            tab.text = mEntriesTabAdapter.getPageTitle(tabPosition)

            return@map async(Dispatchers.Main) {
                try {
                    if (isActive) {
                        val entries = refreshEntries(tabPosition)
                        tabFragment?.setEntries(entries)
                    }
                    return@async
                }
                catch (e: Exception) {
                    Log.d("FailedToRefreshEntries", Log.getStackTraceString(e))
                    if (categoryChanged) {
                        tabFragment?.clear()
                    }
                }
            }
        }

        launch(Dispatchers.Main) {
            try {
                tasks.awaitAll()
            }
            catch (e: Exception) {
                activity.showToast(R.string.msg_update_entries_failed)
            }
            finally {
                activity.hideProgressBar()
            }
        }
    }
}
