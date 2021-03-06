package com.suihan74.satena.scenes.bookmarks.detail.tabs

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.BindingAdapter
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
import com.suihan74.utilities.extensions.ContextExtensions.showToast
import com.suihan74.utilities.lazyProvideViewModel
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

    val viewModel by lazyProvideViewModel {
        StarRelationsTabViewModel(
            tabType = requireArguments().getEnum<DetailTabAdapter.TabType>(ARG_TAB_TYPE)!!,
            repository = bookmarkDetailViewModel.repository
        )
    }

    val tabType: DetailTabAdapter.TabType
        get() = viewModel.tabType

    private var _binding : FragmentStarRelationsTabBinding? = null
    private val binding get() = _binding!!

    // ------ //

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentStarRelationsTabBinding.inflate(inflater, container, false).also {
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
                    runCatching {
                        bookmarkDetailViewModel.updateList(tabType, forceUpdate = true)
                    }.onFailure {
                        lifecycleScope.launch(Dispatchers.Main) {
                            showToast(R.string.msg_update_stars_failed)
                        }
                    }
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
                lifecycleScope.launch {
                    viewModel.openStarRelationMenuDialog(item, childFragmentManager)
                }
            }
        }

        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun scrollToTop() {
        binding.recyclerView.scrollToPosition(0)
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
