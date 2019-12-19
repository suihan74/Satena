package com.suihan74.satena.scenes.bookmarks2.information

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
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.RecyclerView
import com.beloo.widget.chipslayoutmanager.ChipsLayoutManager
import com.bumptech.glide.Glide
import com.suihan74.HatenaLib.HatenaClient
import com.suihan74.HatenaLib.SearchType
import com.suihan74.satena.ActivityBase
import com.suihan74.satena.R
import com.suihan74.satena.scenes.bookmarks2.BookmarksActivity
import com.suihan74.satena.scenes.bookmarks2.BookmarksViewModel
import com.suihan74.satena.scenes.entries.EntriesActivity
import com.suihan74.satena.showCustomTabsIntent
import com.suihan74.utilities.CoroutineScopeFragment
import com.suihan74.utilities.makeSpannedfromHtml
import com.suihan74.utilities.showToast
import com.suihan74.utilities.toVisibility
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class EntryInformationFragment : CoroutineScopeFragment() {
    lateinit var activityViewModel: BookmarksViewModel

    companion object {
        fun createInstance() = EntryInformationFragment()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        activityViewModel = ViewModelProviders.of(requireActivity())[BookmarksViewModel::class.java]
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_entry_information, container, false)

        val entry = activityViewModel.entry

        view.findViewById<TextView>(R.id.title).text = entry.title

        view.findViewById<TextView>(R.id.page_url).apply {
            text = makeSpannedfromHtml("<u>${Uri.decode(entry.url)}</u>")

            setOnClickListener {
                activity!!.showCustomTabsIntent(entry)
            }
        }

        view.findViewById<TextView>(R.id.description).text = entry.description

        view.findViewById<ImageView>(R.id.icon)?.let {
            Glide.with(view)
                .load(entry.imageUrl)
                .into(it)
        }

        val tagsAdapter = object : TagsAdapter() {
            override fun onItemClicked(tag: String) {
                val intent = Intent(activity, EntriesActivity::class.java).apply {
                    putExtra(EntriesActivity.EXTRA_DISPLAY_TAG, tag)
                }
                startActivity(intent)
            }
        }

        view.findViewById<RecyclerView>(R.id.tags_list).apply {
            layoutManager = ChipsLayoutManager.newBuilder(context!!)
                .setMaxViewsInRow(4)
                .setRowStrategy(ChipsLayoutManager.STRATEGY_DEFAULT)
                .build()

            adapter = tagsAdapter
        }

        view.findViewById<Button>(R.id.to_lower_floor_button).apply {
            visibility = HatenaClient.isUrlCommentPages(entry.url).toVisibility(defaultInvisible = View.INVISIBLE)

            setOnClickListener {
                val activity = activity as ActivityBase
                activity.showProgressBar()

                launch(Dispatchers.Main) {
                    val url = HatenaClient.getEntryUrlFromCommentPageUrl(entry.url)
                    changeFloor(url)
                    activity.hideProgressBar()
                }
            }
        }

        view.findViewById<Button>(R.id.to_upper_floor_button).apply {
            visibility = (entry.count > 0).toVisibility(defaultInvisible = View.INVISIBLE)

            setOnClickListener {
                val activity = activity as ActivityBase
                activity.showProgressBar()

                launch(Dispatchers.Main) {
                    val url = HatenaClient.getCommentPageUrlFromEntryUrl(entry.url)
                    changeFloor(url)
                    activity.hideProgressBar()
                }
            }
        }

        // タグ情報を監視
        activityViewModel.bookmarksEntry.observe(this, Observer {
            tagsAdapter.setTags(
                it.tags.map { t -> t.first }.take(10)
            )
        })

        return view
    }

    private suspend fun changeFloor(url: String) {
        try {
            val entry = HatenaClient.searchEntriesAsync(url, SearchType.Text).await()
                .firstOrNull { it.url == url }
                ?: HatenaClient.getEmptyEntryAsync(url).await()

            withContext(Dispatchers.Main) {
                val intent = Intent(context, BookmarksActivity::class.java).apply {
                    putExtra(BookmarksActivity.EXTRA_ENTRY, entry)
                }
                context?.startActivity(intent)
            }
        }
        catch (e: Exception) {
            Log.d("SearchEntry", e.message)
            context?.showToast(R.string.msg_get_entry_information_failed)
        }
    }
}
