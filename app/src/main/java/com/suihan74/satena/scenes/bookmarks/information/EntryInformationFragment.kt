package com.suihan74.satena.scenes.bookmarks.information

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.beloo.widget.chipslayoutmanager.ChipsLayoutManager
import com.bumptech.glide.Glide
import com.suihan74.HatenaLib.BookmarksEntry
import com.suihan74.HatenaLib.Entry
import com.suihan74.HatenaLib.HatenaClient
import com.suihan74.HatenaLib.SearchType
import com.suihan74.satena.ActivityBase
import com.suihan74.satena.R
import com.suihan74.satena.scenes.bookmarks.BookmarksActivity
import com.suihan74.satena.scenes.bookmarks.BookmarksFragment
import com.suihan74.satena.scenes.entries.EntriesActivity
import com.suihan74.satena.scenes.post.BookmarkPostFragment
import com.suihan74.satena.showCustomTabsIntent
import com.suihan74.utilities.CoroutineScopeFragment
import com.suihan74.utilities.makeSpannedfromHtml
import com.suihan74.utilities.showToast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class EntryInformationFragment : CoroutineScopeFragment() {
    private lateinit var mRoot : View
    private var mEntry : Entry = Entry.createEmpty()
    private var mContainsPostFragment: Boolean = false

    var bookmarksEntry : BookmarksEntry? = null
        set(value) {
            field = value
            if (this::mRoot.isInitialized) {
                val adapter = mRoot.findViewById<RecyclerView>(R.id.tags_list).adapter as TagsAdapter
                adapter.setTags(
                    value?.tags
                        ?.map { it.first }
                        ?.take(10) ?: emptyList())
            }
        }

    companion object {
        fun createInstance(entry: Entry, bookmarksEntry: BookmarksEntry? = null, containsPostFragment: Boolean = false) : EntryInformationFragment =
            EntryInformationFragment().apply {
                this.mEntry = entry
                this.bookmarksEntry = bookmarksEntry
                this.mContainsPostFragment = containsPostFragment
            }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_entry_information, container, false)
        mRoot = view

        view.findViewById<TextView>(R.id.title).text = mEntry.title

        view.findViewById<TextView>(R.id.page_url).apply {
            text = makeSpannedfromHtml("<u>${Uri.decode(mEntry.url)}</u>")

            setOnClickListener {
                activity!!.showCustomTabsIntent(mEntry)
            }
        }

        view.findViewById<TextView>(R.id.description).text = mEntry.description

        view.findViewById<ImageView>(R.id.icon).let {
            Glide.with(view)
                .load(mEntry.imageUrl)
                .into(it)
        }

        view.findViewById<RecyclerView>(R.id.tags_list).apply {
            layoutManager = ChipsLayoutManager.newBuilder(context!!)
                .setMaxViewsInRow(4)
                .setRowStrategy(ChipsLayoutManager.STRATEGY_DEFAULT)
                .build()

            adapter = object : TagsAdapter(
                if (bookmarksEntry != null) {
                    bookmarksEntry!!.tags.map { it.first }.take(10).toList()
                }
                else {
                    emptyList()
                }
            ) {
                override fun onItemClicked(tag: String) {
                    val intent = Intent(activity, EntriesActivity::class.java).apply {
                        putExtra(EntriesActivity.EXTRA_DISPLAY_TAG, tag)
                    }
                    startActivity(intent)
                }
            }
        }

        if (mContainsPostFragment) {
            if (HatenaClient.signedIn()) {
                val fragment = BookmarkPostFragment.createInstance(
                    mEntry,
                    bookmarksEntry
                ).apply {
                    setOnPostedListener {
                        activity?.onBackPressed()
                    }
                }

                childFragmentManager.beginTransaction()
                    .replace(R.id.bottom_contents_layout, fragment)
                    .commit()
            }

            view.findViewById<Button>(R.id.to_lower_floor_button).visibility = View.GONE
            view.findViewById<Button>(R.id.to_upper_floor_button).visibility = View.GONE
        }
        else {
            view.findViewById<Button>(R.id.to_lower_floor_button).apply {
                visibility = if (HatenaClient.isUrlCommentPages(mEntry.url)) {
                    View.VISIBLE
                } else {
                    View.INVISIBLE
                }

                setOnClickListener {
                    val activity = activity as ActivityBase
                    activity.showProgressBar()

                    launch(Dispatchers.Main) {
                        val url = HatenaClient.getEntryUrlFromCommentPageUrl(mEntry.url)
                        changeFloor(url)
                        activity.hideProgressBar()
                    }
                }
            }

            view.findViewById<Button>(R.id.to_upper_floor_button).apply {
                visibility = if (mEntry.count > 0) {
                    View.VISIBLE
                } else {
                    View.INVISIBLE
                }

                setOnClickListener {
                    val activity = activity as ActivityBase
                    activity.showProgressBar()

                    launch(Dispatchers.Main) {
                        val url = HatenaClient.getCommentPageUrlFromEntryUrl(mEntry.url)
                        changeFloor(url)
                        activity.hideProgressBar()
                    }
                }
            }
        }

        //retainInstance = true
        return view
    }

    private suspend fun changeFloor(url: String) {
        val activity = activity as ActivityBase

        var entry : Entry? = null
        try {
            entry = HatenaClient.searchEntriesAsync(url, SearchType.Text).await()
                .firstOrNull { it.url == url }
        }
        catch (e: Exception) {
            Log.d("SearchEntry", e.message)
        }

        if (entry == null) {
            try {
                val dummy = HatenaClient.getEmptyBookmarksEntryAsync(url).await()
                entry = Entry(0, dummy.title, "", 0, url, url, "", "", null)
            }
            catch (e: Exception) {
                Log.d("MakeDummyEntry", e.message)
            }
        }

        withContext(Dispatchers.Main) {
            if (entry == null) {
                activity.showToast("エントリ情報の取得に失敗しました")
            }
            else {
                val fragment =
                    BookmarksFragment.createInstance(
                        entry
                    )
                activity.showFragment(fragment,
                    BookmarksActivity.FRAGMENT_TAG_MAIN_CONTENT
                )
            }
        }
    }
}
