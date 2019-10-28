package com.suihan74.satena.scenes.entries

import android.os.Bundle
import android.transition.AutoTransition
import android.transition.TransitionSet
import android.util.Log
import android.view.*
import androidx.appcompat.widget.SearchView
import androidx.viewpager.widget.ViewPager
import com.google.android.material.tabs.TabLayout
import com.suihan74.HatenaLib.Entry
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
        get() = SatenaApplication.instance.getString(mCurrentCategory?.textId ?: 0)

    private lateinit var mEntriesTabPager : ViewPager

    private lateinit var mEntriesTabLayout : TabLayout
    private lateinit var mEntriesTabAdapter : EntriesTabAdapter

    private lateinit var mView : View

    private var mCurrentCategory : Category? = null

    // マイブックマークの検索クエリ
    private var mSearchQuery : String? = null
    val searchQuery: String?
        get() = mSearchQuery

    private val mTasks = ArrayList<Deferred<Any>>()

    companion object {
        fun createInstance(category: Category) = EntriesFragment().apply {
            mCurrentCategory = category
        }

        private const val BUNDLE_CATEGORY = "mCategory"
        private const val BUNDLE_CURRENT_TAB = "currentTab"
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(BUNDLE_CATEGORY, mEntriesTabAdapter.category.int)
        if (this::mEntriesTabPager.isInitialized) {
            outState.putInt(BUNDLE_CURRENT_TAB, mEntriesTabPager.currentItem)
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
        val category = mCurrentCategory
                    ?: savedInstanceState?.let { Category.fromInt(it.getInt(BUNDLE_CATEGORY)) }
                    ?: activity.homeCategory

        mCurrentCategory = category

        // マイブックマーク画面ではツールバーに検索ボタンを表示する
        setHasOptionsMenu(category == Category.MyBookmarks)

        // エントリリスト用アダプタ作成
        mEntriesTabAdapter = EntriesTabAdapter(this, category)
    }


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        mView = inflater.inflate(R.layout.fragment_entries, container, false)

        val prefs = SafeSharedPreferences.create<PreferenceKey>(context!!)

        // タブの設定
        mEntriesTabPager = mView.findViewById(R.id.entries_tab_pager)
        mEntriesTabPager.adapter = mEntriesTabAdapter
        mEntriesTabLayout = mView.findViewById<TabLayout>(R.id.main_tab_layout).apply {
            setupWithViewPager(mEntriesTabPager)
            addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
                override fun onTabSelected(tab: TabLayout.Tab?) {
                    if (mCurrentCategory == Category.MyBookmarks && tab != null) {
                        when(EntriesTabType.MYBOOKMARKS.int + tab.position) {
                            EntriesTabType.MYBOOKMARKS.int -> {
                                setHasOptionsMenu(true)
                            }

                            EntriesTabType.READLATER.int -> {
                                setHasOptionsMenu(false)
                            }
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
                val currentCategory = mCurrentCategory ?: return@setOnTabLongClickListener true
                val catKey = PreferenceKey.ENTRIES_HOME_CATEGORY
                val tabKey = PreferenceKey.ENTRIES_INITIAL_TAB
                if (prefs.getInt(catKey) != currentCategory.int || prefs.getInt(tabKey) != idx) {
                    prefs.edit {
                        put(catKey, currentCategory.int)
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
            launch(Dispatchers.Main) {
                mEntriesTabAdapter.refreshAllTab(mEntriesTabPager)
            }
        }

        return mView
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.search_my_entries, menu)

        // 検索クエリ
        (menu.findItem(R.id.search_view)?.actionView as? SearchView)?.run {
            isSubmitButtonEnabled = true
            queryHint = "検索クエリ"

            if (mSearchQuery != null) {
                setQuery(mSearchQuery, false)
                isIconified = false
            }

            setOnQueryTextListener(object : SearchView.OnQueryTextListener {
                override fun onQueryTextChange(newText: String?): Boolean = false
                override fun onQueryTextSubmit(query: String?): Boolean {
                    mSearchQuery = query
                    refreshEntriesTabs(Category.MyBookmarks)
                    return false
                }
            })

            setOnCloseListener {
                mSearchQuery = null
                refreshEntriesTabs(Category.MyBookmarks)
                return@setOnCloseListener false
            }
        }
    }

    fun refreshEntriesAsync(tabPosition: Int? = null, offset: Int? = null) : Deferred<List<Entry>> =
        mEntriesTabAdapter.getEntriesAsync(tabPosition ?: mEntriesTabLayout.selectedTabPosition, offset)

    fun refreshEntriesTabs(category: Category) {
        val mainActivity = activity as EntriesActivity
        mainActivity.showProgressBar()
        mainActivity.updateToolbar()

        val categoryChanged = mCurrentCategory != category
        mCurrentCategory = category
        mEntriesTabAdapter.setCategory(mEntriesTabPager, category)

        if (categoryChanged) {
            setHasOptionsMenu(category == Category.MyBookmarks)
        }

        for (tabPosition in 0 until mEntriesTabAdapter.count) {
            val tabFragment = mEntriesTabAdapter.findFragment(mEntriesTabPager, tabPosition)
            val tab = mEntriesTabLayout.getTabAt(tabPosition)!!

            mEntriesTabAdapter.setCategory(mEntriesTabPager, category)
            tab.text = mEntriesTabAdapter.getPageTitle(tabPosition)

            val deferred = async(Dispatchers.Main) {
                try {
                    if (isActive) {
                        val entries = refreshEntriesAsync(tabPosition).await()
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
            mTasks.add(deferred)
        }

        launch(Dispatchers.Main) {
            try {
                mTasks.awaitAll()
            }
            catch (e: Exception) {
                activity!!.showToast("エントリーリスト更新失敗")
            }
            finally {
                mTasks.clear()
                mainActivity.hideProgressBar()
            }
        }
    }
}
