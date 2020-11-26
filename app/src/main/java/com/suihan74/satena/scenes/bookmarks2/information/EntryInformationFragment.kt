package com.suihan74.satena.scenes.bookmarks2.information

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.activity.addCallback
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.observe
import com.beloo.widget.chipslayoutmanager.ChipsLayoutManager
import com.suihan74.hatenaLib.HatenaClient
import com.suihan74.satena.R
import com.suihan74.satena.databinding.FragmentEntryInformationBinding
import com.suihan74.satena.scenes.bookmarks2.BookmarksActivity
import com.suihan74.satena.scenes.bookmarks2.BookmarksViewModel
import com.suihan74.satena.scenes.entries2.EntriesActivity
import com.suihan74.satena.startInnerBrowser
import com.suihan74.utilities.bindings.setVisibility
import com.suihan74.utilities.extensions.makeSpannedFromHtml

class EntryInformationFragment : Fragment() {
    private val activityViewModel: BookmarksViewModel
        get() = bookmarksActivity!!.viewModel

    private val bookmarksActivity
        get() = activity as? BookmarksActivity

    private var onBackPressedCallback: OnBackPressedCallback? = null

    companion object {
        fun createInstance() = EntryInformationFragment()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val binding = DataBindingUtil.inflate<FragmentEntryInformationBinding>(
            inflater,
            R.layout.fragment_entry_information,
            container,
            false
        ).also {
            it.vm = activityViewModel
            it.lifecycleOwner = viewLifecycleOwner
        }

        activityViewModel.entry.observe(viewLifecycleOwner) { entry ->
            binding.pageUrl.apply {
                text = makeSpannedFromHtml("<u>${Uri.decode(entry.url)}</u>")

                setOnClickListener {
                    bookmarksActivity?.let {
                        it.closeDrawer()
                        it.startInnerBrowser(entry)
                    }
                }

                setOnLongClickListener {
                    val intent = Intent(Intent.ACTION_SEND).also {
                        it.putExtra(Intent.EXTRA_TEXT, entry.url)
                        it.type = "text/plain"
                    }
                    activity?.startActivity(intent)
                    true
                }
            }

            // -1階
            binding.toLowerFloorButton.apply {
                setVisibility(HatenaClient.isUrlCommentPages(entry.url), View.INVISIBLE)

                setOnClickListener {
                    val url = HatenaClient.getEntryUrlFromCommentPageUrl(entry.url)
                    changeFloor(url)
                }
            }

            // +1階 (今見ているページのコメントページに移動)
            binding.toUpperFloorButton.apply {
                setVisibility(entry.count > 0, View.INVISIBLE)

                setOnClickListener {
                    val url = HatenaClient.getCommentPageUrlFromEntryUrl(entry.url)
                    changeFloor(url)
                }
            }
        }

        val tagsAdapter = object : TagsAdapter() {
            override fun onItemClicked(tag: String) {
                bookmarksActivity?.let { activity ->
                    activity.closeDrawer()
                    val intent = Intent(activity, EntriesActivity::class.java).apply {
                        putExtra(EntriesActivity.EXTRA_SEARCH_TAG, tag)
                    }
                    startActivity(intent)
                }
            }
        }

        binding.tagsList.apply {
            layoutManager = ChipsLayoutManager.newBuilder(requireContext())
                .setMaxViewsInRow(4)
                .setRowStrategy(ChipsLayoutManager.STRATEGY_DEFAULT)
                .build()

            adapter = tagsAdapter
        }

        // タグ情報を監視
        activityViewModel.bookmarksEntry.observe(viewLifecycleOwner) {
            tagsAdapter.setTags(
                it?.tags?.map { t -> t.first }?.take(10) ?: emptyList()
            )
        }

        return binding.root
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
        bookmarksActivity?.let { activity ->
            activity.closeDrawer()
            val intent = Intent(activity, BookmarksActivity::class.java).apply {
                putExtra(BookmarksActivity.EXTRA_ENTRY_URL, url)
            }
            startActivity(intent)
        }
    }
}
