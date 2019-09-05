package com.suihan74.satena.fragments

import android.net.Uri
import android.os.Bundle
import android.support.customtabs.CustomTabsIntent
import android.support.v4.content.ContextCompat
import android.support.v7.widget.RecyclerView
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import com.beloo.widget.chipslayoutmanager.ChipsLayoutManager
import com.bumptech.glide.Glide
import com.suihan74.HatenaLib.*
import com.suihan74.utilities.FragmentContainerActivity
import com.suihan74.utilities.makeSpannedfromHtml
import com.suihan74.satena.BrowserToolbarManager
import com.suihan74.satena.R
import com.suihan74.satena.adapters.TagsAdapter
import com.suihan74.utilities.CoroutineScopeFragment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class EntryInformationFragment : CoroutineScopeFragment() {
    private lateinit var mRoot : View
    private var mEntry : Entry = emptyEntry()
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
                val intent = CustomTabsIntent.Builder(null)
                    .setShowTitle(true)
                    .enableUrlBarHiding()
                    .addDefaultShareMenuItem()
                    .setToolbarColor(ContextCompat.getColor(activity!!, R.color.colorPrimary))
                    .setSecondaryToolbarViews(
                        BrowserToolbarManager.createRemoteViews(activity!!, mEntry),
                        BrowserToolbarManager.getClickableIds(),
                        BrowserToolbarManager.getOnClickPendingIntent(activity!!)
                    )
                    .build()
                intent.launchUrl(activity, Uri.parse(mEntry.url))
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
                    val fragment =
                        SearchEntriesFragment.createInstance(tag, SearchType.Tag)
                    (activity as FragmentContainerActivity).showFragment(fragment, null)
                }
            }
        }

        if (mContainsPostFragment) {
            val fragment = BookmarkPostFragment.createInstance(mEntry, bookmarksEntry).apply {
                setOnPostedListener {
                    activity?.onBackPressed()
                }
            }

            childFragmentManager.beginTransaction()
                .replace(R.id.bottom_contents_layout, fragment)
                .commit()

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
                    isClickable = false

                    launch(Dispatchers.Main) {
                        val url = HatenaClient.getEntryUrlFromCommentPageUrl(mEntry.url)
                        changeFloor(url)
                        isClickable = true
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
                    isClickable = false

                    launch(Dispatchers.Main) {
                        val url = HatenaClient.getCommentPageUrlFromEntryUrl(mEntry.url)
                        changeFloor(url)
                        isClickable = true
                    }
                }
            }
        }

        retainInstance = true
        return view
    }

    private suspend fun changeFloor(url: String) {
        var entry : Entry? = null
        try {
            entry = HatenaClient.searchEntriesAsync(url, SearchType.Text).await()
                .firstOrNull { it.url == url }
        }
        catch (e: Exception) {
            Log.e("SearchEntries", Log.getStackTraceString(e))
        }

        if (entry == null) {
            val dummy = HatenaClient.getEmptyBookmarksEntryAsync(url).await()
            entry = Entry(0, dummy.title, "", 0, url, url, "", "", null)
        }

        val fragment = BookmarksFragment.createInstance(entry)
        (activity as FragmentContainerActivity).showFragment(fragment)
    }
}
