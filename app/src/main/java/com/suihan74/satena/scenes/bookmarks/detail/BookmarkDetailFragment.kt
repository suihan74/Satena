package com.suihan74.satena.scenes.bookmarks.detail

import android.content.res.Configuration
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
import kotlinx.android.synthetic.main.fragment_bookmark_detail.view.*

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

        // タブの設定
        viewModel.bookmark.observe(viewLifecycleOwner) { bookmark ->
            val adapter = DetailTabAdapter(bookmark, childFragmentManager)
            adapter.setup(requireContext(), binding.tabLayout, binding.tabPager)
        }

        // ブクマに対するメニュー
        binding.menuButton.setOnClickListener {
            val bookmark = viewModel.bookmark.value ?: return@setOnClickListener
            bookmarksViewModel.openBookmarkMenuDialog(
                requireActivity(),
                bookmark,
                childFragmentManager
            )
        }

        // コメントの文字列選択
        // 選択テキストを画面下部で強調表示，スターを付ける際に引用文とする
        binding.comment.customSelectionActionModeCallback = object : ActionMode.Callback {
            override fun onPrepareActionMode(mode: ActionMode?, menu: Menu?) : Boolean {
                val result = runCatching {
                    binding.comment.let {
                        it.text.substring(it.selectionStart, it.selectionEnd)
                    }
                }
                viewModel.selectedText.value = result.getOrNull()
                return false
            }
            override fun onDestroyActionMode(mode: ActionMode?) {
                viewModel.selectedText.value = null
            }
            override fun onCreateActionMode(mode: ActionMode?, menu: Menu?) = true
            override fun onActionItemClicked(mode: ActionMode?, item: MenuItem?) = false
        }

        // TODO: スター付与ボタン設定

        binding.showStarsButton.setOnClickListener {
            // TODO: 表示状態を切り替えるようにする
            openStarMenu()
        }

        return binding.root
    }

    // ------ //

    private fun showStarButton(layoutId: Int, counterId: Int, dimenId: Int) =
        when (resources.configuration.orientation) {
            Configuration.ORIENTATION_LANDSCAPE -> showStarButtonLandscape(layoutId, counterId, dimenId)
            else -> showStarButtonPortrait(layoutId, counterId, dimenId)
        }

    private fun showStarButtonPortrait(layoutId: Int, counterId: Int, dimenId: Int) {
        val view = requireView()
        val layout = view.findViewById<View>(layoutId)
        val counter = view.findViewById<View>(counterId)
        val pos = requireContext().resources.getDimension(dimenId)

        layout.animate()
            .withEndAction {
                counter.animate()
                    .translationXBy(100f)
                    .translationX(0f)
                    .alphaBy(0.0f)
                    .alpha(1.0f)
                    .duration = 100
            }
            .translationYBy(0f)
            .translationY(-pos)
            .alphaBy(0f)
            .alpha(1f)
            .duration = 100
    }

    private fun showStarButtonLandscape(layoutId: Int, counterId: Int, dimenId: Int) {
        val view = requireView()
        val layout = view.findViewById<View>(layoutId)
        val counter = view.findViewById<View>(counterId)
        val pos = requireContext().resources.getDimension(dimenId)

        layout.animate()
            .withEndAction {
                counter.animate()
                    .translationYBy(50f)
                    .translationY(0f)
                    .alphaBy(0.0f)
                    .alpha(1.0f)
                    .duration = 100
            }
            .translationXBy(0f)
            .translationX(-pos)
            .alphaBy(0f)
            .alpha(1f)
            .duration = 100
    }

    private fun hideStarButton(layoutId: Int, counterId: Int) =
        when (resources.configuration.orientation) {
            Configuration.ORIENTATION_LANDSCAPE -> hideStarButtonLandscape(layoutId, counterId)
            else -> hideStarButtonPortrait(layoutId, counterId)
        }

    private fun hideStarButtonPortrait(layoutId: Int, counterId: Int) {
        val view = requireView()
        val layout = view.findViewById<View>(layoutId)
        val counter = view.findViewById<View>(counterId)

        counter.animate()
            .withEndAction {
                layout.animate()
                    .translationY(0f)
                    .alpha(0f)
                    .duration = 100
            }
            .translationX(100f)
            .alphaBy(1.0f)
            .alpha(0.0f)
            .duration = 100
    }

    private fun hideStarButtonLandscape(layoutId: Int, counterId: Int) {
        val view = requireView()
        val layout = view.findViewById<View>(layoutId)
        val counter = view.findViewById<View>(counterId)

        counter.animate()
            .withEndAction {
                layout.animate()
                    .translationX(0f)
                    .alpha(0f)
                    .duration = 100
            }
            .translationY(50f)
            .alphaBy(1.0f)
            .alpha(0.0f)
            .duration = 100
    }

    private fun openStarMenu() {
        showStarButton(
            R.id.purple_star_layout,
            R.id.purple_stars_count,
            R.dimen.purple_star_position
        )
        showStarButton(
            R.id.blue_star_layout,
            R.id.blue_stars_count,
            R.dimen.blue_star_position
        )
        showStarButton(
            R.id.red_star_layout,
            R.id.red_stars_count,
            R.dimen.red_star_position
        )
        showStarButton(
            R.id.green_star_layout,
            R.id.green_stars_count,
            R.dimen.green_star_position
        )
        showStarButton(
            R.id.yellow_star_layout,
            R.id.yellow_stars_count,
            R.dimen.yellow_star_position
        )

        requireView().show_stars_button.setImageResource(R.drawable.ic_baseline_close)
    }

    private fun closeStarMenu() {
        hideStarButton(R.id.purple_star_layout, R.id.purple_stars_count)
        hideStarButton(R.id.blue_star_layout, R.id.blue_stars_count)
        hideStarButton(R.id.red_star_layout, R.id.red_stars_count)
        hideStarButton(R.id.green_star_layout, R.id.green_stars_count)
        hideStarButton(R.id.yellow_star_layout, R.id.yellow_stars_count)

        requireView().show_stars_button.setImageResource(R.drawable.ic_add_star_filled)
    }
}
