package com.suihan74.satena.scenes.bookmarks.detail.tabs

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.BindingAdapter
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import com.suihan74.satena.R
import com.suihan74.satena.databinding.FragmentStarRelationsTabBinding
import com.suihan74.satena.scenes.bookmarks.BookmarksActivity
import com.suihan74.satena.scenes.bookmarks.detail.BookmarkDetailFragment
import com.suihan74.satena.scenes.bookmarks.detail.BookmarkDetailViewModel
import com.suihan74.satena.scenes.bookmarks.detail.DetailTabAdapter
import com.suihan74.satena.scenes.bookmarks.repository.StarRelation
import com.suihan74.utilities.ScrollableToTop
import com.suihan74.utilities.extensions.*
import com.suihan74.utilities.provideViewModel
import kotlinx.android.synthetic.main.fragment_star_relations_tab.view.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * スターをつけたユーザーのブコメを表示するタブ
 */
class StarRelationsTabFragment : Fragment(), ScrollableToTop {

    companion object {
        fun createInstance(
            tabType: DetailTabAdapter.TabType
        ) = StarRelationsTabFragment().withArguments {
            putEnum(ARG_TAB_TYPE, tabType)
        }

        private const val ARG_TAB_TYPE = "ARG_TAB_TYPE"
    }

    // ------ //

    private val bookmarksActivity : BookmarksActivity
        get() = requireActivity() as BookmarksActivity

    private val bookmarkDetailFragment : BookmarkDetailFragment
        get() = parentFragment as BookmarkDetailFragment

    private val bookmarkDetailViewModel : BookmarkDetailViewModel
        get() = bookmarkDetailFragment.viewModel

    val viewModel by lazy {
        provideViewModel(this) {
            StarRelationsTabViewModel(
                tabType = requireArguments().getEnum<DetailTabAdapter.TabType>(ARG_TAB_TYPE)!!,
                repository = bookmarkDetailViewModel.repository
            )
        }
    }

    val tabType: DetailTabAdapter.TabType
        get() = viewModel.tabType

    // ------ //

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val binding = DataBindingUtil.inflate<FragmentStarRelationsTabBinding>(
            inflater,
            R.layout.fragment_star_relations_tab,
            container,
            false
        ).also {
            it.vm = bookmarkDetailViewModel
            it.tabType = tabType
            it.lifecycleOwner = viewLifecycleOwner
        }

        binding.swipeLayout.run {
            val context = requireContext()
            setProgressBackgroundColorSchemeColor(context.getThemeColor(R.attr.swipeRefreshBackground))
            setColorSchemeColors(context.getThemeColor(R.attr.colorPrimary))
            setOnRefreshListener {
                lifecycleScope.launch(Dispatchers.Main) {
                    bookmarkDetailViewModel.updateList(tabType, forceUpdate = true)
                    isRefreshing = false
                }
            }
        }

        binding.recyclerView.adapter = StarRelationsAdapter(tabType, viewLifecycleOwner).also { adapter ->
            adapter.setOnClickItemListener {
                val item = it.item ?: return@setOnClickItemListener
                viewModel.onClickItem(bookmarksActivity, item)
            }

            adapter.setOnLongLickItemListener {
                val item = it.item ?: return@setOnLongLickItemListener
                viewModel.openStarRelationMenuDialog(
                    bookmarksActivity,
                    item,
                    childFragmentManager,
                    lifecycleScope
                )
            }
        }

        return binding.root
    }

    override fun scrollToTop() {
        requireView().recycler_view.scrollToPosition(0)
    }

    // ------ //

    object BindingAdapters {
        @JvmStatic
        @BindingAdapter("starRelations", "ignoredUsers")
        fun setStarRelations(view: RecyclerView, items: List<StarRelation>?, ignoredUsers: List<String>?) {
            view.adapter.alsoAs<StarRelationsAdapter> { adapter ->
                if (items == null) {
                    adapter.setItems(null)
                }
                else {
                    adapter.setStarRelations(items, ignoredUsers.orEmpty())
                }
            }
        }
    }
}
