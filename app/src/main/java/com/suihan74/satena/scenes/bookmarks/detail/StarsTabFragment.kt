package com.suihan74.satena.scenes.bookmarks.detail

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.suihan74.HatenaLib.Bookmark
import com.suihan74.HatenaLib.HatenaClient
import com.suihan74.HatenaLib.Star
import com.suihan74.satena.R
import com.suihan74.satena.SatenaApplication
import com.suihan74.satena.dialogs.AlertDialogFragment
import com.suihan74.satena.dialogs.AlertDialogListener
import com.suihan74.satena.scenes.bookmarks.BookmarksActivity
import com.suihan74.satena.scenes.bookmarks.BookmarksFragment
import com.suihan74.satena.scenes.entries.EntriesActivity
import com.suihan74.utilities.CoroutineScopeFragment
import com.suihan74.utilities.DividerItemDecorator
import com.suihan74.utilities.getThemeColor
import com.suihan74.utilities.showToast
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch

class StarsTabFragment : CoroutineScopeFragment(), AlertDialogListener {
    private lateinit var mRoot: View
    private var mBookmarksFragment : BookmarksFragment? = null
    private var mBookmarkDetailFragment : BookmarkDetailFragment? = null

    private lateinit var mBookmark : Bookmark
    private lateinit var mStarsTabMode : StarsAdapter.StarsTabMode

    private lateinit var mStarsAdapter: StarsAdapter

    companion object {
        fun createInstance(
            b: Bookmark,
            tabMode: StarsAdapter.StarsTabMode
        ) = StarsTabFragment().apply {
            arguments = Bundle().apply {
                putSerializable(ARGS_KEY_BOOKMARK, b)
                putSerializable(ARGS_KEY_STARS_TAB_MODE, tabMode)
            }
        }

        private const val ARGS_KEY_BOOKMARK = "mBookmark"
        private const val ARGS_KEY_STARS_TAB_MODE = "mStarsTabMode"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments!!.let {
            mBookmark = it.getSerializable(ARGS_KEY_BOOKMARK) as Bookmark
            mStarsTabMode = it.getSerializable(ARGS_KEY_STARS_TAB_MODE) as StarsAdapter.StarsTabMode
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_stars_tab, container, false)
        mRoot = view

        val activity = activity as? BookmarksActivity ?: throw IllegalStateException("StarsTabFragment has created from an invalid activity")

        mBookmarksFragment = activity.bookmarksFragment
        mBookmarkDetailFragment = activity.getStackedFragment<BookmarkDetailFragment>("detail_id:${mBookmark.user}")

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
                override fun removeItem(user: String) {
                    super.removeItem(user)
                    launch(Dispatchers.Main) {
                        mBookmarkDetailFragment?.updateStars()
                    }
                }

                override fun onItemClicked(user: String, star: Star?) {
                    val target = allBookmarks.firstOrNull { it.user == user } ?: return

                    activity.apply {
                        showFragment(
                            BookmarkDetailFragment.createInstance(
                                target
                            ), "detail_id:${target.user}")
                    }
                }

                override fun onItemLongClicked(user: String, star: Star?): Boolean {
                    val items = arrayListOf(getString(R.string.star_item_menu_show_user_entries))

                    if (mStarsTabMode == StarsTabMode.TO_USER && HatenaClient.account?.name == user) {
                        items.add(getString(R.string.star_item_menu_remove))
                    }

                    AlertDialogFragment.Builder(R.style.AlertDialogStyle)
                        .setTitle(user)
                        .setNegativeButton(R.string.dialog_cancel)
                        .setItems(items)
                        .setAdditionalData("star", star!!)
                        .setAdditionalData("user", user)
                        .show(childFragmentManager, "star_menu_dialog")

                    return true
                }
            }
            adapter = mStarsAdapter
        }

        // スワイプ更新機能の設定
        view.findViewById<SwipeRefreshLayout>(R.id.stars_swipe_layout).apply swipeLayout@ {
            setProgressBackgroundColorSchemeColor(activity.getThemeColor(R.attr.swipeRefreshBackground))
            setColorSchemeColors(activity.getThemeColor(R.attr.colorPrimary))
            setOnRefreshListener {
                launch(Dispatchers.Main) {
                    try {
                        mBookmarkDetailFragment?.updateStars()
                    }
                    catch (e: Exception) {
                        activity.showToast(R.string.msg_update_stars_failed)
                        Log.d("FailedToUpdateStars", Log.getStackTraceString(e))
                    }
                    finally {
                        this@swipeLayout.isRefreshing = false
                    }
                }
            }
        }

        return view
    }

    fun scrollToTop() {
        mRoot.findViewById<RecyclerView>(R.id.stars_list).scrollToPosition(0)
    }

    override fun onSelectItem(dialog: AlertDialogFragment, which: Int) {
        val user = dialog.getAdditionalData<String>("user") ?: return

        when (dialog.items?.get(which)) {
            getString(R.string.star_item_menu_show_user_entries) -> {
                val intent = Intent(SatenaApplication.instance, EntriesActivity::class.java).apply {
                    putExtra(EntriesActivity.EXTRA_DISPLAY_USER, user)
                }
                startActivity(intent)
            }

            getString(R.string.star_item_menu_remove) -> {
                val star = dialog.getAdditionalData<Star>("star") ?: return
                launch(Dispatchers.Main) {
                    try {
                        val bookmarksEntry = mBookmarksFragment!!.bookmarksEntry!!
                        val tasks = ArrayList<Deferred<Any>>()
                        for (i in 1..star.count) {
                            tasks.add(HatenaClient.deleteStarAsync(mBookmark.getBookmarkUrl(bookmarksEntry), star))
                        }
                        tasks.awaitAll()
                        mStarsAdapter.removeItem(user)
                        activity?.showToast(R.string.msg_remove_star_succeeded, user)
                    }
                    catch (e: Exception) {
                        activity?.showToast(R.string.msg_remove_star_failed, user)
                        Log.d("failedToDeleteStar", Log.getStackTraceString(e))
                    }
                }
            }
        }
    }
}
