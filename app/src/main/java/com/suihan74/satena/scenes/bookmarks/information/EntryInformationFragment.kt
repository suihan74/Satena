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
import androidx.recyclerview.widget.RecyclerView
import com.beloo.widget.chipslayoutmanager.ChipsLayoutManager
import com.suihan74.hatenaLib.Entry
import com.suihan74.hatenaLib.HatenaClient
import com.suihan74.satena.databinding.FragmentEntryInformationBinding
import com.suihan74.satena.scenes.bookmarks.BookmarksActivity
import com.suihan74.satena.scenes.bookmarks.viewModel.BookmarksViewModel
import com.suihan74.satena.scenes.entries2.EntriesActivity
import com.suihan74.utilities.RecyclerState
import com.suihan74.utilities.RecyclerType
import com.suihan74.utilities.bindings.setVisibility
import com.suihan74.utilities.extensions.alsoAs
import com.suihan74.utilities.extensions.makeSpannedFromHtml
import com.suihan74.utilities.extensions.putObjectExtra
import com.suihan74.utilities.lazyProvideViewModel

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

    private val viewModel by lazyProvideViewModel {
        EntryInformationViewModel(bookmarksViewModel.entry)
    }

    // ------ //

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val binding = FragmentEntryInformationBinding.inflate(inflater, container, false).also {
            it.vm = viewModel
            it.bookmarkVM = bookmarksViewModel
            it.activity = bookmarksActivity
            it.lifecycleOwner = viewLifecycleOwner
        }

        binding.pageUrl.apply {
            setOnClickListener { viewModel.onClickPageUrl(bookmarksActivity) }
            setOnLongClickListener { viewModel.onLongClickPageUrl(bookmarksActivity) }
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

        binding.relatedEntriesList.adapter = RelatedEntriesAdapter(viewLifecycleOwner).also { adapter ->
            adapter.setOnItemClickedListener {
                val intent = Intent(bookmarksActivity, BookmarksActivity::class.java).apply {
                    putObjectExtra(BookmarksActivity.EXTRA_ENTRY, it)
                }
                startActivity(intent)
            }
        }

        return binding.root
    }

    // ------ //

    object BindingAdapters {
        @JvmStatic
        @BindingAdapter("relatedEntries")
        fun setRelatedEntries(recyclerView: RecyclerView, entries: List<Entry>?) {
            recyclerView.adapter.alsoAs<RelatedEntriesAdapter> { adapter ->
                adapter.submitList(
                    entries?.map { RecyclerState(RecyclerType.BODY, it) }.orEmpty()
                )
            }
        }

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
            view.setVisibility(entry != null && HatenaClient.isUrlCommentPages(entry.url), disabledDefault = View.INVISIBLE)
        }
    }
}
