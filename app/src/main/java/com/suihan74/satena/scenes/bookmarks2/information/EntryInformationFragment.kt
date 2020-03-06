package com.suihan74.satena.scenes.bookmarks2.information

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.activity.addCallback
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.beloo.widget.chipslayoutmanager.ChipsLayoutManager
import com.bumptech.glide.Glide
import com.suihan74.hatenaLib.HatenaClient
import com.suihan74.satena.R
import com.suihan74.satena.scenes.bookmarks2.BookmarksActivity
import com.suihan74.satena.scenes.bookmarks2.BookmarksViewModel
import com.suihan74.satena.scenes.entries.EntriesActivity
import com.suihan74.satena.showCustomTabsIntent
import com.suihan74.utilities.makeSpannedFromHtml
import com.suihan74.utilities.toVisibility
import kotlinx.android.synthetic.main.fragment_entry_information.view.*

class EntryInformationFragment : Fragment() {
    lateinit var activityViewModel: BookmarksViewModel

    private val bookmarksActivity
        get() = activity as? BookmarksActivity

    private var onBackPressedCallback: OnBackPressedCallback? = null

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

        view.title.text = entry.title

        view.page_url.apply {
            text = makeSpannedFromHtml("<u>${Uri.decode(entry.url)}</u>")

            setOnClickListener {
                activity!!.showCustomTabsIntent(entry)
            }
        }

        view.description.text = entry.description

        Glide.with(view)
            .load(entry.imageUrl)
            .into(view.icon)

        val tagsAdapter = object : TagsAdapter() {
            override fun onItemClicked(tag: String) {
                val intent = Intent(activity, EntriesActivity::class.java).apply {
                    putExtra(EntriesActivity.EXTRA_DISPLAY_TAG, tag)
                }
                startActivity(intent)
            }
        }

        view.tags_list.apply {
            layoutManager = ChipsLayoutManager.newBuilder(context!!)
                .setMaxViewsInRow(4)
                .setRowStrategy(ChipsLayoutManager.STRATEGY_DEFAULT)
                .build()

            adapter = tagsAdapter
        }

        // -1階
        view.to_lower_floor_button.apply {
            visibility = HatenaClient.isUrlCommentPages(entry.url).toVisibility(defaultInvisible = View.INVISIBLE)

            setOnClickListener {
                val url = HatenaClient.getEntryUrlFromCommentPageUrl(entry.url)
                changeFloor(url)
            }
        }

        // +1階 (今見ているページのコメントページに移動)
        view.to_upper_floor_button.apply {
            visibility = (entry.count > 0).toVisibility(defaultInvisible = View.INVISIBLE)

            setOnClickListener {
                val url = HatenaClient.getCommentPageUrlFromEntryUrl(entry.url)
                changeFloor(url)
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

    /** このFragmentを表示しているDrawerLayoutがオープンされたときに呼ぶ */
    fun onShown() {
        // 戻るボタンを監視
        onBackPressedCallback = requireActivity().onBackPressedDispatcher.addCallback(this) {
            bookmarksActivity?.onBackPressedCallback?.handleOnBackPressed()
            onBackPressedCallback?.remove()
        }
    }

    private fun changeFloor(url: String) {
        val intent = Intent(context, BookmarksActivity::class.java).apply {
            putExtra(BookmarksActivity.EXTRA_ENTRY_URL, url)
        }
        context?.startActivity(intent)
    }
}
