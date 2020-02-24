package com.suihan74.satena.scenes.entries

import android.os.Bundle
import android.transition.Fade
import android.transition.TransitionSet
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Spinner
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.suihan74.HatenaLib.Entry
import com.suihan74.satena.ActivityBase
import com.suihan74.satena.R
import com.suihan74.satena.models.Category
import com.suihan74.utilities.DividerItemDecorator
import com.suihan74.utilities.RecyclerViewScrollingUpdater
import com.suihan74.utilities.getThemeColor
import com.suihan74.utilities.showToast
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

abstract class SingleTabEntriesFragmentBase : EntriesTabFragmentBase() {
    private lateinit var mRoot : View
    private lateinit var mSpinner : Spinner
    private lateinit var mSwipeRefreshLayout : SwipeRefreshLayout

    private var mEntriesAdapter : EntriesAdapter? = null
    private var mEntriesScrollingUpdater: RecyclerViewScrollingUpdater? = null

    abstract val currentCategory: Category

    override val entriesAdapter: EntriesAdapter?
        get() = mEntriesAdapter

    open fun onRestoreSaveInstanceState(savedInstanceState: Bundle) {}

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enterTransition = TransitionSet().addTransition(Fade())
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        mRoot = inflater.inflate(R.layout.fragment_user_entries, container, false).apply {
            mSpinner = findViewById<Spinner>(R.id.spinner).apply {
                visibility = View.GONE
            }

            mSwipeRefreshLayout = findViewById<SwipeRefreshLayout>(R.id.swipe_layout).apply {
                setProgressBackgroundColorSchemeColor(activity!!.getThemeColor(R.attr.swipeRefreshBackground))
                setColorSchemeColors(ContextCompat.getColor(activity!!, R.color.colorPrimary))
                setOnRefreshListener {
                    refreshEntries()
                }
            }
        }

        if (savedInstanceState != null) {
            onRestoreSaveInstanceState(savedInstanceState)
        }

        return mRoot
    }

    override fun onResume() {
        super.onResume()

        if (mEntriesAdapter != null) {
            mRoot.findViewById<RecyclerView>(R.id.entries_list).apply {
                val dividerItemDecoration = DividerItemDecorator(ContextCompat.getDrawable(context!!,
                    R.drawable.recycler_view_item_divider
                )!!)
                addItemDecoration(dividerItemDecoration)
                layoutManager = LinearLayoutManager(context)
                adapter = mEntriesAdapter
                if (mEntriesScrollingUpdater != null) {
                    addOnScrollListener(mEntriesScrollingUpdater!!)
                }
            }
        }
    }

    override fun onDestroyView() {
        hideProgressBar()
        super.onDestroyView()
    }

    fun showSpinner() : Spinner =
        mSpinner.apply { visibility = View.VISIBLE }

    fun showProgressBar() {
        (activity as? ActivityBase)?.showProgressBar()
    }

    fun hideProgressBar() {
        (activity as? ActivityBase)?.hideProgressBar()
    }

    fun refreshEntries(updater: (Int?)->Deferred<List<Entry>>) = refreshEntries(getString(R.string.msg_update_entries_failed), updater)

    fun refreshEntries(errorMessage: String, updater: (Int?)->Deferred<List<Entry>>) = launch(Dispatchers.Main) {
        if (!mSwipeRefreshLayout.isRefreshing) {
            showProgressBar()
        }

        try {
            val entries = updater(null).await()

            val dividerItemDecoration = DividerItemDecorator(ContextCompat.getDrawable(context!!,
                R.drawable.recycler_view_item_divider
            )!!)
            var scrollPosition = 0
            val recyclerView = mRoot.findViewById<RecyclerView>(R.id.entries_list)
            recyclerView.apply {
                addItemDecoration(dividerItemDecoration)
                layoutManager = LinearLayoutManager(context)

                if (mEntriesAdapter == null) {
                    mEntriesAdapter = EntriesAdapter(
                        this@SingleTabEntriesFragmentBase,
                        Category.All,
                        0
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
                    adapter = mEntriesAdapter
                }

                mEntriesAdapter!!.setEntries(entries)

                val mEntriesAdapter = mEntriesAdapter!!
                if (mEntriesScrollingUpdater != null) {
                    removeOnScrollListener(mEntriesScrollingUpdater!!)
                }
                mEntriesScrollingUpdater = object : RecyclerViewScrollingUpdater(mEntriesAdapter) {
                    override fun load() {
                        this@SingleTabEntriesFragmentBase.launch(Dispatchers.Main) {
                            try {
                                val additionalEntries = updater(mEntriesAdapter.entireOffset).await()
                                mEntriesAdapter.addEntries(additionalEntries)
                            }
                            catch (e: Exception) {
                                Log.d("FailedToFetchEntries", Log.getStackTraceString(e))
                                context?.showToast(errorMessage)
                            }
                            finally {
                                loadCompleted()
                            }
                        }
                    }
                }
                addOnScrollListener(mEntriesScrollingUpdater!!)
            }
        }
        catch (e: Exception) {
            Log.d("FailedToFetchEntries", Log.getStackTraceString(e))
            context?.showToast(errorMessage)
        }
        finally {
            hideProgressBar()
            mSwipeRefreshLayout.isRefreshing = false
        }
    }

    protected val entriesCount : Int
        get() = mEntriesAdapter?.itemCount ?: 0

    abstract fun refreshEntries() : Any
}
