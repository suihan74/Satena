package com.suihan74.satena.scenes.bookmarks.detail

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.suihan74.HatenaLib.Bookmark
import com.suihan74.satena.R
import com.suihan74.satena.SatenaApplication
import com.suihan74.satena.dialogs.AlertDialogFragment
import com.suihan74.satena.dialogs.AlertDialogListener
import com.suihan74.satena.scenes.bookmarks.BookmarksActivity
import com.suihan74.satena.scenes.entries.EntriesActivity
import com.suihan74.utilities.DividerItemDecorator
import com.suihan74.utilities.FragmentContainerActivity

class MentionedBookmarksTabFragment : Fragment(), AlertDialogListener {
    private lateinit var mRoot : View

    companion object {
        fun createInstance(targetBookmark: Bookmark, tabMode: StarsTabAdapter.Tab) = MentionedBookmarksTabFragment().apply {
            arguments = Bundle().apply {
                putSerializable(ARG_KEY_TARGET_BOOKMARK, targetBookmark)
                putInt(ARG_KEY_TAB_MODE, tabMode.int)
            }
        }

        private const val ARG_KEY_TARGET_BOOKMARK = "target_bookmark"
        private const val ARG_KEY_TAB_MODE = "tab_mode"
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val root = inflater.inflate(R.layout.fragment_stars_tab, container, false)
        mRoot = root

        val bookmarksFragment = (activity as? BookmarksActivity)?.bookmarksFragment
            ?: throw RuntimeException("MentionedBookmarksTabFragment#onCreateView")

        val (targetBookmark, tabMode) = arguments!!.run {
            Pair(
                getSerializable(ARG_KEY_TARGET_BOOKMARK) as Bookmark,
                StarsTabAdapter.Tab.fromInt(getInt(ARG_KEY_TAB_MODE))
            ) }

        root.findViewById<RecyclerView>(R.id.stars_list).apply {
            val dividerItemDecoration = DividerItemDecorator(ContextCompat.getDrawable(context!!, R.drawable.recycler_view_item_divider)!!)
            val bookmarks = bookmarksFragment.bookmarksEntry?.bookmarks ?: emptyList()
            val starsMap = bookmarksFragment.starsMap

            val displayBookmarks = StarsTabAdapter.getMentions(targetBookmark, bookmarks, tabMode)

            addItemDecoration(dividerItemDecoration)
            setHasFixedSize(true)
            layoutManager = LinearLayoutManager(context)
            adapter = object : MentionedBookmarksAdapter(displayBookmarks, starsMap) {
                override fun onItemClicked(user: String) {
                    val target = bookmarks.firstOrNull { it.user == user } ?: return
                    (activity as FragmentContainerActivity).apply {
                        showFragment(
                            BookmarkDetailFragment.createInstance(
                                target
                            ), "detail_id:${user}"
                        )
                    }
                }

                override fun onItemLongClicked(user: String): Boolean {
                    val items = arrayListOf(getString(R.string.mentioned_item_menu_show_user_entries))
                    AlertDialogFragment.Builder(R.style.AlertDialogStyle)
                        .setTitle(user)
                        .setNegativeButton(R.string.dialog_cancel)
                        .setItems(items)
                        .setAdditionalData("user", user)
                        .show(childFragmentManager, "menu_dialog")

                    return true
                }
            }
        }

        return root
    }

    fun scrollToTop() {
        mRoot.findViewById<RecyclerView>(R.id.stars_list).scrollToPosition(0)
    }

    override fun onSelectItem(dialog: AlertDialogFragment, which: Int) {
        val user = dialog.getAdditionalData<String>("user") ?: return
        val intent = Intent(context, EntriesActivity::class.java).apply {
            putExtra(EntriesActivity.EXTRA_DISPLAY_USER, user)
        }
        startActivity(intent)
    }
}
