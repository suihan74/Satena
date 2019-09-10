package com.suihan74.satena.fragments

import android.os.Bundle
import android.support.v4.content.ContextCompat
import android.support.v4.widget.SwipeRefreshLayout
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.suihan74.HatenaLib.Category
import com.suihan74.HatenaLib.Entry
import com.suihan74.satena.R
import com.suihan74.satena.activities.ActivityBase
import com.suihan74.satena.adapters.EntriesAdapter
import com.suihan74.utilities.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class EntriesTabFragment : CoroutineScopeFragment() {
    private var mTabPosition: Int = -1
    private var mEntries : List<Entry> = emptyList()
    private lateinit var mEntriesAdapter : EntriesAdapter
    private lateinit var mEntriesScrollingUpdater : RecyclerViewScrollingUpdater
    private lateinit var mView : View
    private lateinit var mCategory: Category

    private var mEntriesFragment: EntriesFragment? = null

    var category: Category
        get() = mCategory
        set(value) {
            mCategory = value
            mEntriesAdapter.category = value
        }

    companion object {
        fun createInstance(tabPosition: Int, category: Category) = EntriesTabFragment().apply {
            mTabPosition = tabPosition
            mCategory = category
        }

        private const val BUNDLE_BASE = "com.suihan74.satena.fragments.EntriesTabFragment."
        private const val BUNDLE_CATEGORY = BUNDLE_BASE + "mCategory"
        private const val BUNDLE_TAB_POSITION = BUNDLE_BASE + "mTabPosition"
    }

    fun setEntries(entriesList: List<Entry>) {
        mEntries = entriesList
        if (this::mEntriesAdapter.isInitialized) {
            mEntriesAdapter.setEntries(mEntries)
            scrollToTop()

            if (this::mEntriesScrollingUpdater.isInitialized) {
                mEntriesScrollingUpdater.refreshInvokingPosition(mEntriesAdapter.itemCount)
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.run {
            putInt(BUNDLE_CATEGORY, mCategory.int)
            putInt(BUNDLE_TAB_POSITION, mTabPosition)
        }
    }

    override fun onViewStateRestored(savedInstanceState: Bundle?) {
        super.onViewStateRestored(savedInstanceState)
        savedInstanceState?.let {
            mTabPosition = it.getInt(BUNDLE_TAB_POSITION)
        }
    }

    override fun onResume() {
        super.onResume()
        mEntriesAdapter.onResume()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_entries_tab, container, false)
        mView = view

        val activity = activity as ActivityBase
        mEntriesFragment = activity.currentFragment as? EntriesFragment

        savedInstanceState?.let {
            mCategory = Category.fromInt(it.getInt(BUNDLE_CATEGORY))
        }

        // initialize entries list
        val recyclerView = view.findViewById<RecyclerView>(R.id.entries_list)
        val dividerItemDecoration = DividerItemDecorator(ContextCompat.getDrawable(context!!,
            R.drawable.recycler_view_item_divider
        )!!)
        recyclerView.apply {
            addItemDecoration(dividerItemDecoration)
            layoutManager = LinearLayoutManager(context)
            mEntriesAdapter = EntriesAdapter(this@EntriesTabFragment, category, mTabPosition, mEntries)
            adapter = mEntriesAdapter
            mEntriesScrollingUpdater = object : RecyclerViewScrollingUpdater(mEntriesAdapter.itemCount - 1) {
                override fun load() {
                    launch(Dispatchers.Main) {
                        try {
                            mEntriesFragment?.refreshEntriesAsync(mTabPosition, mEntriesAdapter.entireOffset)?.await()?.let {
                                mEntriesAdapter.addEntries(it)
                            }
                        }
                        catch (e: Exception) {
                            Log.d("FailedToFetchEntries", Log.getStackTraceString(e))
                            activity.showToast("エントリーリスト更新失敗")
                        }
                        finally {
                            loadCompleted(mEntriesAdapter.itemCount - 1)
                        }
                    }
                }
            }
            addOnScrollListener(mEntriesScrollingUpdater)
        }

        // スワイプ更新機能の設定
        view.findViewById<SwipeRefreshLayout>(R.id.entries_swipe_layout).apply swipeLayout@ {
            setProgressBackgroundColorSchemeColor(activity.getThemeColor(R.attr.swipeRefreshBackground))
            setColorSchemeColors(ContextCompat.getColor(activity, R.color.colorPrimary))
            setOnRefreshListener {
                launch(Dispatchers.Main) {
                    try {
                        mEntriesFragment?.refreshEntriesAsync(mTabPosition)?.await()?.let {
                            mEntriesAdapter.setEntries(it)
                            mEntriesScrollingUpdater.refreshInvokingPosition(mEntriesAdapter.itemCount)
                        }
                    }
                    catch (e: Exception) {
                        Log.d("FailedToFetchEntries", Log.getStackTraceString(e))
                        activity.showToast("エントリーリスト更新失敗")
                    }
                    finally {
                        this@swipeLayout.isRefreshing = false
                    }
                }
            }
        }

        retainInstance = true
        return view
    }


    fun scrollToTop() = launch(Dispatchers.Main) {
        val recyclerView = mView.findViewById<RecyclerView>(R.id.entries_list)
        recyclerView.scrollToPosition(0)
    }

    fun clear() {
        setEntries(ArrayList())
    }
}
