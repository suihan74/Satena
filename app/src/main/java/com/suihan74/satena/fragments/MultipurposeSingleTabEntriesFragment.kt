package com.suihan74.satena.fragments

import android.content.Context
import android.os.Bundle
import android.support.design.widget.AppBarLayout
import android.support.v4.content.ContextCompat
import android.support.v4.widget.SwipeRefreshLayout
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.Toolbar
import android.transition.Slide
import android.transition.TransitionSet
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.ProgressBar
import com.suihan74.HatenaLib.Category
import com.suihan74.HatenaLib.Entry
import com.suihan74.satena.R
import com.suihan74.satena.adapters.EntriesAdapter
import com.suihan74.satena.models.PreferenceKey
import com.suihan74.utilities.*
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

abstract class MultipurposeSingleTabEntriesFragment : CoroutineScopeFragment() {
    private lateinit var mRoot : View
    private lateinit var mProgressBar : ProgressBar
    private lateinit var mSearchLayout : View
    private lateinit var mSearchEditText : View
    private lateinit var mSwipeRefreshLayout : SwipeRefreshLayout

    private var mEntriesAdapter : EntriesAdapter? = null
    private var mEntriesScrollingUpdater: RecyclerViewScrollingUpdater? = null

    init {
        enterTransition = TransitionSet().addTransition(Slide(Gravity.BOTTOM))
    }

    open fun onRestoreSaveInstanceState(savedInstanceState: Bundle) {}

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        mRoot = inflater.inflate(R.layout.fragment_user_entries, container, false).apply {
            mProgressBar = findViewById<ProgressBar>(R.id.detail_progress_bar).apply {
                visibility = View.INVISIBLE
            }

            mSearchLayout = findViewById<View>(R.id.search_layout).apply {
                visibility = View.INVISIBLE
            }

            mSearchEditText = findViewById<View>(R.id.search_query).apply {
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

        val prefs = SafeSharedPreferences.create<PreferenceKey>(context!!)
        mRoot.findViewById<Toolbar>(R.id.toolbar).apply {
            layoutParams = (layoutParams as AppBarLayout.LayoutParams).apply {
                val switchToolbarDisplay = prefs.getBoolean(PreferenceKey.ENTRIES_HIDING_TOOLBAR_BY_SCROLLING)
                scrollFlags =
                    if (switchToolbarDisplay)
                        AppBarLayout.LayoutParams.SCROLL_FLAG_SCROLL or AppBarLayout.LayoutParams.SCROLL_FLAG_ENTER_ALWAYS
                    else 0
            }
        }

        val editText = mRoot.findViewById<EditText>(R.id.search_query)
        val context = context
        if (context != null && editText.isFocusable && editText.visibility == View.VISIBLE) {
            editText.requestFocus()
            val im = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            im.showSoftInput(editText, 0)
            editText.setSelection(editText.text.length)
        }
    }

    fun showSearchLayout() {
        mSearchLayout.visibility = View.VISIBLE
        mSearchEditText.visibility = View.VISIBLE
    }

    fun showProgressBar() {
        mProgressBar.visibility = View.VISIBLE
    }

    fun hideProgressBar() {
        mProgressBar.visibility = View.INVISIBLE
    }

    fun refreshEntries(updater: (Int?)->Deferred<List<Entry>>) = refreshEntries("エントリ取得失敗", updater)

    fun refreshEntries(errorMessage: String, updater: (Int?)->Deferred<List<Entry>>) = launch(Dispatchers.Main) {
        if (!mSwipeRefreshLayout.isRefreshing) {
            showProgressBar()
        }

        try {
            val entries = updater(null).await()

            val dividerItemDecoration = DividerItemDecorator(ContextCompat.getDrawable(context!!,
                R.drawable.recycler_view_item_divider
            )!!)
            mRoot.findViewById<RecyclerView>(R.id.entries_list).apply {
                addItemDecoration(dividerItemDecoration)
                layoutManager = LinearLayoutManager(context)

                if (mEntriesAdapter == null) {
                    mEntriesAdapter = EntriesAdapter(this@MultipurposeSingleTabEntriesFragment, Category.All, 0, entries)
                    adapter = mEntriesAdapter
                }
                else {
                    mEntriesAdapter!!.setEntries(entries)
                }

                val mEntriesAdapter = mEntriesAdapter!!
                if (mEntriesScrollingUpdater != null) {
                    removeOnScrollListener(mEntriesScrollingUpdater!!)
                }
                mEntriesScrollingUpdater = object : RecyclerViewScrollingUpdater(mEntriesAdapter) {
                    override fun load() {
                        this@MultipurposeSingleTabEntriesFragment.launch(Dispatchers.Main) {
                            try {
                                val additionalEntries = updater(mEntriesAdapter.entireOffset).await()
                                mEntriesAdapter.addEntries(additionalEntries)
                            }
                            catch (e: Exception) {
                                Log.d("FailedToFetchEntries", Log.getStackTraceString(e))
                                activity!!.showToast(errorMessage)
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

    abstract fun refreshEntries() : Any
}
