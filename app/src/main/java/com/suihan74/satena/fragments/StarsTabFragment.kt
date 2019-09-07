package com.suihan74.satena.fragments

import android.app.AlertDialog
import android.os.Bundle
import android.support.v4.content.ContextCompat
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.suihan74.HatenaLib.*
import com.suihan74.utilities.DividerItemDecorator
import com.suihan74.utilities.FragmentContainerActivity
import com.suihan74.utilities.showToast
import com.suihan74.satena.R
import com.suihan74.satena.adapters.StarsAdapter
import com.suihan74.utilities.CoroutineScopeFragment
import kotlinx.coroutines.*
import kotlin.collections.ArrayList

class StarsTabFragment : CoroutineScopeFragment() {
    private var mBookmarksFragment : BookmarksFragment? = null
    private lateinit var mBookmark : Bookmark
    private lateinit var mStarsTabMode : StarsAdapter.StarsTabMode

    private lateinit var mStarsAdapter: StarsAdapter

    companion object {
        fun createInstance(
            fragment: BookmarksFragment,
            b: Bookmark,
            tabMode: StarsAdapter.StarsTabMode
        ) = StarsTabFragment().apply {
            mBookmarksFragment = fragment
            mBookmark = b
            mStarsTabMode = tabMode
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_stars_tab, container, false)

        val bookmarksEntry = mBookmarksFragment!!.bookmarksEntry!!
        val starsMap = mBookmarksFragment!!.starsMap
        val allBookmarks = bookmarksEntry.bookmarks

        // initialize bookmarks list
        view.findViewById<RecyclerView>(R.id.stars_list).apply {
            val dividerItemDecoration = DividerItemDecorator(ContextCompat.getDrawable(context!!, R.drawable.recycler_view_item_divider)!!)

            addItemDecoration(dividerItemDecoration)
            setHasFixedSize(true)
            layoutManager = LinearLayoutManager(context)
            mStarsAdapter = object : StarsAdapter(context, mBookmark, starsMap, allBookmarks, mStarsTabMode) {
                override fun onItemClicked(user: String, star: Star?) {
                    val target = allBookmarks.firstOrNull { it.user == user } ?: return

                    (activity as FragmentContainerActivity).apply {
                        showFragment(
                            BookmarkDetailFragment.createInstance(
                                mBookmarksFragment!!,
                                target
                            ), null)
                    }
                }

                override fun onItemLongClicked(user: String, star: Star?): Boolean {
                    val items = arrayListOf<Pair<String, ()->Any>>(
                        "ブクマしたエントリを見る" to {
                            (activity as FragmentContainerActivity).apply {
                                showFragment(
                                    UserEntriesFragment.createInstance(
                                        user
                                    ), null)
                            }
                        })

                    if (mStarsTabMode == StarsTabMode.TO_USER && HatenaClient.account?.name == user) {
                        items.add("スターを削除する" to {
                            launch(Dispatchers.Main) {
                                try {
                                    val tasks = ArrayList<Deferred<Any>>()
                                    for (i in 1..star!!.count) {
                                        tasks.add(HatenaClient.deleteStarAsync(mBookmark.getBookmarkUrl(bookmarksEntry), star))
                                    }
                                    tasks.awaitAll()
                                    removeItem(user)
                                    activity!!.showToast("${user}へのスターを削除しました")
                                }
                                catch (e: Exception) {
                                    activity!!.showToast("${user}へのスターの削除に失敗しました")
                                    Log.d("failedToDeleteStar", Log.getStackTraceString(e))
                                }
                            }
                        })
                    }

                    AlertDialog.Builder(context, R.style.AlertDialogStyle)
                        .setTitle(user)
                        .setNegativeButton("Cancel", null)
                        .setItems(items.map { it.first }.toTypedArray()) { _, which ->
                            items[which].second()
                        }
                        .show()

                    return true
                }
            }
            adapter = mStarsAdapter
        }

        /*
        // スワイプ更新機能の設定
        val swipeRefreshLayout = view.findViewById<SwipeRefreshLayout>(R.id.bookmarks_swipe_layout)
        val schemeColor = TypedValue()
        activity!!.theme.resolveAttribute(android.R.attr.colorPrimary, schemeColor, true)
        swipeRefreshLayout.setColorSchemeColors(schemeColor.data)

        swipeRefreshLayout.setOnRefreshListener {
            launch(Dispatchers.Main) {
                val bookmarksActivity = activity as BookmarksActivity

                bookmarksActivity.bookmarksFragment.refreshBookmarksAsync().await().let {
                    recyclerView.scrollToPosition(0)
                    swipeRefreshLayout.isRefreshing = false
                }
            }
        }
        */

        retainInstance = true
        return view
    }
}
