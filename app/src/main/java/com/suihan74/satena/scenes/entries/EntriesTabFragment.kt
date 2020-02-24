package com.suihan74.satena.scenes.entries

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.suihan74.HatenaLib.EntriesType
import com.suihan74.HatenaLib.Entry
import com.suihan74.HatenaLib.HatenaClient
import com.suihan74.satena.ActivityBase
import com.suihan74.satena.R
import com.suihan74.satena.models.Category
import com.suihan74.satena.models.ignoredEntry.IgnoredEntry
import com.suihan74.satena.models.ignoredEntry.IgnoredEntryDao
import com.suihan74.utilities.DividerItemDecorator
import com.suihan74.utilities.RecyclerViewScrollingUpdater
import com.suihan74.utilities.getThemeColor
import com.suihan74.utilities.showToast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class EntriesTabRepository(
    private val ignoredEntryDao : IgnoredEntryDao,
    private val client: HatenaClient
) {
    lateinit var entriesType: EntriesType
    lateinit var category: Category

    private var ignoredEntries: List<IgnoredEntry> = emptyList()

    private val mEntries by lazy { ArrayList<Entry>() }
    val entries: List<Entry>
        get() = mEntries.filterNot { entry ->
            ignoredEntries.any { ig -> ig.isMatched(entry) }
        }

    suspend fun init(entriesType: EntriesType, category: Category) {
        try {
            ignoredEntries = withContext(Dispatchers.IO) {
                ignoredEntryDao.getAllEntries()
            }

            this.entriesType = entriesType
            this.category = category
            val hatenaCategory = category.categoryInApi ?: throw IllegalArgumentException()
            val response = client.getEntriesAsync(entriesType, hatenaCategory).await()
            mEntries.clear()
            mEntries.addAll(response)
        }
        catch (e: Exception) {
            throw RuntimeException("failed to initialize entries")
        }
    }

    suspend fun loadAdditional() {
        try {
            val hatenaCategory = category.categoryInApi ?: throw IllegalStateException()
            val response = client.getEntriesAsync(entriesType, hatenaCategory, mEntries.size).await()
            mEntries.addAll(response)
        }
        catch (e: Exception) {
            throw RuntimeException("failed to load additional entries")
        }
    }

    suspend fun loadRecent() {
        try {
            val hatenaCategory = category.categoryInApi ?: throw IllegalStateException()
            val response = client.getEntriesAsync(entriesType, hatenaCategory).await()
            val recent = response.filterNot { mEntries.any { existed -> existed.id == it.id } }
            mEntries.addAll(0, recent)
        }
        catch (e: Exception) {
            throw RuntimeException("failed to load additional entries")
        }
    }
}

class EntriesTabViewModel(
    private val repository: EntriesTabRepository
) : ViewModel() {

    val entries by lazy { MutableLiveData<List<Entry>>() }
    val entriesType by lazy { MutableLiveData<EntriesType>() }
    val category by lazy { MutableLiveData<Category>() }

    class Factory(private val repository: EntriesTabRepository) : ViewModelProvider.NewInstanceFactory() {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>) =
            EntriesTabViewModel(repository) as T
    }
}

class EntriesTabFragment : EntriesTabFragmentBase() {
    private var mView : View? = null
    private var mEntriesAdapter : EntriesAdapter? = null
    private var mEntriesScrollingUpdater : RecyclerViewScrollingUpdater? = null

    private var mTabPosition: Int = -1
    private var mEntries : List<Entry> = emptyList()
    private var mCategory: Category? = null

    private var mEntriesFragment: EntriesFragment? = null

    override val entriesAdapter: EntriesAdapter?
        get() = mEntriesAdapter

    var category: Category
        get() = mCategory!!

        set(value) {
            mCategory = value
            mEntriesAdapter?.category = value
        }

    companion object {
        fun createInstance(tabPosition: Int, category: Category) = EntriesTabFragment().apply {
            mTabPosition = tabPosition
            mCategory = category
        }

        private const val BUNDLE_CATEGORY = "mCategory"
        private const val BUNDLE_TAB_POSITION = "mTabPosition"
    }

    fun setEntries(entriesList: List<Entry>) {
        mEntries = entriesList
        mEntriesAdapter?.setEntries(mEntries)
        scrollToTop()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.run {
            putInt(BUNDLE_CATEGORY, mCategory?.ordinal ?: 0)
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
        mEntriesAdapter?.onResume()
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
        var scrollPosition = 0
        val recyclerView = view.findViewById<RecyclerView>(R.id.entries_list)
        val dividerItemDecoration = DividerItemDecorator(ContextCompat.getDrawable(context!!,
            R.drawable.recycler_view_item_divider
        )!!)
        recyclerView.apply {
            addItemDecoration(dividerItemDecoration)
            layoutManager = LinearLayoutManager(context)
            mEntriesAdapter = EntriesAdapter(
                this@EntriesTabFragment,
                category,
                mTabPosition
            ).apply {
                registerAdapterDataObserver(object : RecyclerView.AdapterDataObserver() {
                    override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
                        if (scrollPosition >= 0) {
                            recyclerView.scrollToPosition(scrollPosition)
                            scrollPosition = -1
                        }
                    }
                })
            }
//            mEntriesAdapter!!.setEntries(mEntries)
            adapter = mEntriesAdapter
            mEntriesScrollingUpdater = object : RecyclerViewScrollingUpdater(mEntriesAdapter!!) {
                override fun load() {
                    launch(Dispatchers.Main) {
                        try {
                            mEntriesFragment?.refreshEntries(mTabPosition, mEntriesAdapter!!.entireOffset)?.let {
                                mEntriesAdapter!!.addEntries(it)
                            }
                        }
                        catch (e: Exception) {
                            Log.d("FailedToFetchEntries", Log.getStackTraceString(e))
                            activity.showToast(R.string.msg_update_entries_failed)
                        }
                        finally {
                            loadCompleted()
                        }
                    }
                }
            }
            addOnScrollListener(mEntriesScrollingUpdater!!)
        }

        // スワイプ更新機能の設定
        view.findViewById<SwipeRefreshLayout>(R.id.entries_swipe_layout).apply swipeLayout@ {
            setProgressBackgroundColorSchemeColor(activity.getThemeColor(R.attr.swipeRefreshBackground))
            setColorSchemeColors(ContextCompat.getColor(activity, R.color.colorPrimary))
            setOnRefreshListener {
                launch(Dispatchers.Main) {
                    try {
                        mEntriesFragment?.refreshEntries(mTabPosition)?.let {
                            mEntriesAdapter?.setEntries(it)
//                            mEntriesScrollingUpdater.refreshInvokingPosition(mEntriesAdapter.itemCount)
                        }
                    }
                    catch (e: Exception) {
                        Log.d("FailedToFetchEntries", Log.getStackTraceString(e))
                        activity.showToast(R.string.msg_update_entries_failed)
                    }
                    finally {
                        this@swipeLayout.isRefreshing = false
                    }
                }
            }
        }

        (activity as EntriesActivity).model.ignoredEntries.observe(this, Observer {
            mEntriesAdapter?.updateIgnoredEntries()
        })

        return view
    }


    fun scrollToTop() = launch(Dispatchers.Main) {
        val recyclerView = mView?.findViewById<RecyclerView>(R.id.entries_list)
        recyclerView?.scrollToPosition(0)
    }

    fun clear() {
        setEntries(ArrayList())
    }
}
