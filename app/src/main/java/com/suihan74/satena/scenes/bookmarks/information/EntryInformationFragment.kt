package com.suihan74.satena.scenes.bookmarks.information

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.databinding.BindingAdapter
import androidx.fragment.app.Fragment
import com.beloo.widget.chipslayoutmanager.ChipsLayoutManager
import com.suihan74.hatenaLib.Entry
import com.suihan74.hatenaLib.HatenaClient
import com.suihan74.satena.databinding.FragmentEntryInformation3Binding
import com.suihan74.satena.scenes.bookmarks.BookmarksActivity
import com.suihan74.satena.scenes.bookmarks.viewModel.BookmarksViewModel
import com.suihan74.satena.scenes.entries2.EntriesActivity
import com.suihan74.satena.startInnerBrowser
import com.suihan74.utilities.bindings.setVisibility
import com.suihan74.utilities.extensions.makeSpannedFromHtml

/** ドロワ部分にエントリ情報を表示する */
class EntryInformationFragment : Fragment() {

    companion object {
        fun createInstance() = EntryInformationFragment()
    }

    // ------ //

    private val bookmarksActivity
        get() = requireActivity() as BookmarksActivity

    private val bookmarksViewModel: BookmarksViewModel
        get() = bookmarksActivity.bookmarksViewModel

    // ------ //

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val binding = FragmentEntryInformation3Binding.inflate(inflater, container, false).also {
            it.vm = bookmarksViewModel
            it.lifecycleOwner = viewLifecycleOwner
        }

        binding.pageUrl.apply {
            setOnClickListener {
                val entry = bookmarksViewModel.entry.value ?: return@setOnClickListener
                bookmarksActivity.let {
                    it.closeDrawer()
                    it.startInnerBrowser(entry)
                }
            }

            setOnLongClickListener {
                val entry = bookmarksViewModel.entry.value ?: return@setOnLongClickListener false
                val intent = Intent(Intent.ACTION_SEND).also {
                    it.putExtra(Intent.EXTRA_TEXT, entry.url)
                    it.type = "text/plain"
                }
                startActivity(intent)
                true
            }
        }

        // スターをつける
        binding.addStarButton.setOnClickListener {
            bookmarksViewModel.openAddStarPopup(
                requireActivity(),
                viewLifecycleOwner,
                binding.addStarButton
            ) { color ->
                bookmarksViewModel.postStarToEntry(requireContext(), color, childFragmentManager)
            }
        }

        // -1階
        binding.toLowerFloorButton.setOnClickListener {
            val entry = bookmarksViewModel.entry.value ?: return@setOnClickListener
            val url = HatenaClient.getEntryUrlFromCommentPageUrl(entry.url)
            changeFloor(url)
        }

        // +1階 (今見ているページのコメントページに移動)
        binding.toUpperFloorButton.setOnClickListener {
            val entry = bookmarksViewModel.entry.value ?: return@setOnClickListener
            val url = HatenaClient.getCommentPageUrlFromEntryUrl(entry.url)
            changeFloor(url)
        }

        binding.tagsList.apply {
            layoutManager = ChipsLayoutManager.newBuilder(requireContext())
                .setMaxViewsInRow(4)
                .setRowStrategy(ChipsLayoutManager.STRATEGY_DEFAULT)
                .build()

            adapter = TagsAdapter().also { adapter ->
                adapter.setOnItemClickedListener { tag ->
                    bookmarksActivity.closeDrawer()
                    val intent = Intent(bookmarksActivity, EntriesActivity::class.java).apply {
                        putExtra(EntriesActivity.EXTRA_SEARCH_TAG, tag)
                    }
                    startActivity(intent)
                }
            }
        }

        return binding.root
    }

    private fun changeFloor(url: String) {
        bookmarksActivity.closeDrawer()
        val intent = Intent(bookmarksActivity, BookmarksActivity::class.java).apply {
            putExtra(BookmarksActivity.EXTRA_ENTRY_URL, url)
            bookmarksViewModel.entry.value?.let { entry ->
                Regex("""(\S+)\s*のブックマーク\s*/\s*はてなブックマーク$""")
                    .find(entry.title)
                    ?.groupValues
                    ?.get(1)
                    ?.let { userName ->
                        putExtra(BookmarksActivity.EXTRA_TARGET_USER, userName)
                    }
            }
        }
        startActivity(intent)
    }

    // ------ //

    object BindingAdapters {
        /** エントリURLを装飾してボタンに表示する */
        @JvmStatic
        @BindingAdapter("entryPageUrl")
        fun setEntryPageUrl(view: TextView, url: String?) {
            view.text = url?.let { makeSpannedFromHtml("<u>${Uri.decode(it)}</u>") } ?: ""
        }

        /** +1階ボタンの表示可否 */
        @JvmStatic
        @BindingAdapter("upperFloorButtonVisibility")
        fun setUpperFloorButtonVisibility(view: View, entry: Entry?) {
            view.setVisibility(entry != null && entry.count > 0)
        }

        /** -1階ボタンの表示可否 */
        @JvmStatic
        @BindingAdapter("lowerFloorButtonVisibility")
        fun setLowerFloorButtonVisibility(view: View, entry: Entry?) {
            view.setVisibility(entry != null && HatenaClient.isUrlCommentPages(entry.url))
        }
    }
}
