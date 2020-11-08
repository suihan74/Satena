package com.suihan74.satena.scenes.bookmarks.detail

import android.os.Bundle
import android.transition.Fade
import android.transition.Slide
import android.transition.TransitionSet
import android.view.*
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import com.suihan74.hatenaLib.Bookmark
import com.suihan74.satena.R
import com.suihan74.satena.databinding.FragmentBookmarkDetail3Binding
import com.suihan74.satena.scenes.bookmarks.BookmarksActivity
import com.suihan74.satena.scenes.bookmarks.viewModel.BookmarksViewModel
import com.suihan74.utilities.extensions.getObject
import com.suihan74.utilities.extensions.putObject
import com.suihan74.utilities.extensions.withArguments
import com.suihan74.utilities.provideViewModel

/**
 * 選択したひとつのブクマ情報を表示する画面
 */
class BookmarkDetailFragment : Fragment() {

    companion object {
        fun createInstance(bookmark: Bookmark) = BookmarkDetailFragment().withArguments {
            putObject(ARG_BOOKMARK, bookmark)
        }

        private const val ARG_BOOKMARK = "ARG_BOOKMARK"
    }

    // ------ //

    val bookmarksActivity : BookmarksActivity
        get() = requireActivity() as BookmarksActivity

    val bookmarksViewModel : BookmarksViewModel
        get() = bookmarksActivity.bookmarksViewModel

    val viewModel by lazy {
        provideViewModel(this) {
            val args = requireArguments()
            val bookmark = args.getObject<Bookmark>(ARG_BOOKMARK)!!
            val repository = bookmarksViewModel.repository
            BookmarkDetailViewModel(repository, bookmark)
        }
    }

    // ------ //

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 画面遷移アニメーション
        enterTransition = TransitionSet()
            .addTransition(Fade())
            .addTransition(Slide(Gravity.END))
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val binding = DataBindingUtil.inflate<FragmentBookmarkDetail3Binding>(
            inflater,
            R.layout.fragment_bookmark_detail3,
            container,
            false
        ).also {
            it.vm = viewModel
            it.bookmarksViewModel = bookmarksViewModel
            it.lifecycleOwner = viewLifecycleOwner
        }

        return binding.root
    }
}
