package com.suihan74.satena.scenes.bookmarks.detail

import android.app.Activity
import android.content.res.Configuration
import android.os.Bundle
import android.transition.Fade
import android.transition.Slide
import android.transition.TransitionSet
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.addCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.widget.TooltipCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.suihan74.hatenaLib.Bookmark
import com.suihan74.hatenaLib.Entry
import com.suihan74.hatenaLib.StarColor
import com.suihan74.satena.R
import com.suihan74.satena.databinding.FragmentBookmarkDetail3Binding
import com.suihan74.satena.scenes.bookmarks.BookmarksActivity
import com.suihan74.satena.scenes.bookmarks.viewModel.BookmarksViewModel
import com.suihan74.utilities.extensions.getObject
import com.suihan74.utilities.extensions.putObject
import com.suihan74.utilities.extensions.withArguments
import com.suihan74.utilities.provideViewModel
import kotlinx.coroutines.launch

/**
 * 選択したひとつのブクマ情報を表示する画面
 */
class BookmarkDetailFragment : Fragment() {

    companion object {
        fun createInstance(entry: Entry, bookmark: Bookmark) = BookmarkDetailFragment().withArguments {
            putObject(ARG_ENTRY, entry)
            putObject(ARG_BOOKMARK, bookmark)
            putString(ARG_USER, bookmark.user)
        }

        /** 表示対象のエントリ */
        private const val ARG_ENTRY = "ARG_ENTRY"

        /** 表示対象のブクマ */
        private const val ARG_BOOKMARK = "ARG_BOOKMARK"

        /**
         * 対象ブクマのユーザー
         *
         * viewModel取得or生成時に毎回Bookmarkインスタンスをデシリアライズし直さないようにするため使用
         * (あってもなくてもそれほどはパフォーマンスに影響ないとは思う)
         */
        private const val ARG_USER = "ARG_USER"
    }

    // ------ //

    private val bookmarksActivity : BookmarksActivity
        get() = requireActivity() as BookmarksActivity

    private val bookmarksViewModel : BookmarksViewModel
        get() = bookmarksActivity.bookmarksViewModel

    val viewModel by lazy {
        val args = requireArguments()
        val user = args.getString(ARG_USER)!!
        val viewModelKey = "BookmarkDetail_$user"

        // 複数回同じユーザーの詳細画面が開かれた場合使いまわすためActivityをownerにしている
        provideViewModel(bookmarksActivity, viewModelKey) {
            val entry = args.getObject<Entry>(ARG_ENTRY)!!
            val bookmark = args.getObject<Bookmark>(ARG_BOOKMARK)!!
            val repository = bookmarksViewModel.repository
            BookmarkDetailViewModel(repository, entry, bookmark)
        }
    }

    val bookmarkPostActivityLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            lifecycleScope.launch {
                viewModel.updateBookmarksToUser()
            }
        }
    }

    // ------ //

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 画面遷移アニメーション
        enterTransition = TransitionSet()
            .addTransition(Fade())
            .addTransition(Slide(Gravity.END))

        exitTransition = TransitionSet()
            .addTransition(Fade())
            .addTransition(Slide(Gravity.START))
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val binding = FragmentBookmarkDetail3Binding.inflate(inflater, container, false).also {
            it.vm = viewModel
            it.bookmarksViewModel = bookmarksViewModel
            it.lifecycleOwner = viewLifecycleOwner
        }

        viewModel.onCreateView(viewLifecycleOwner)

        // タブの設定
        viewModel.bookmark.observe(viewLifecycleOwner) { bookmark ->
            val adapter = DetailTabAdapter(this, viewModel, bookmark)
            adapter.setup(requireContext(), binding.tabLayout, binding.tabPager)
        }

        // ブクマに対するメニュー
        binding.menuButton.setOnClickListener {
            lifecycleScope.launch {
                viewModel.openBookmarkMenuDialog(childFragmentManager)
            }
        }

        // コメントの文字列選択
        // 選択テキストを画面下部で強調表示，スターを付ける際に引用文とする
        // 「検索」ボタンを追加
        binding.comment.customSelectionActionModeCallback =
            viewModel.customSelectionActionCallback(requireContext(), binding.comment)

        // スター付与ボタン設定
        initializeStarButtons(binding)

        // ブクマボタン設定
        binding.bookmarkButton.setOnClickListener {
            lifecycleScope.launch {
                viewModel.openPostBookmarkActivity(this@BookmarkDetailFragment)
            }
        }

        binding.showStarsButton.setOnClickListener {
            viewModel.starsMenuOpened.value = viewModel.starsMenuOpened.value != true
        }

        // 戻るボタンでスターメニューを閉じる
        val onBackPressedCallback = requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            false,
            { viewModel.starsMenuOpened.value = false }
        )

        viewModel.starsMenuOpened.observe(viewLifecycleOwner) {
            when (it) {
                true -> openStarMenu(binding)
                false -> closeStarMenu(binding)
            }
            onBackPressedCallback.isEnabled = it
        }

        return binding.root
    }

    // ------ //

    /** スター付与ボタンの挙動を設定 */
    private fun initializeStarButtons(binding: FragmentBookmarkDetail3Binding) {
        fun postStar(color: StarColor) : (View)->Unit = {
            bookmarksViewModel.postStarToBookmark(
                requireContext(),
                viewModel.bookmark.value!!,
                color,
                viewModel.selectedText.value,
                childFragmentManager
            )
        }

        binding.yellowStarButton.setOnClickListener(postStar(StarColor.Yellow))
        binding.redStarButton.setOnClickListener(postStar(StarColor.Red))
        binding.greenStarButton.setOnClickListener(postStar(StarColor.Green))
        binding.blueStarButton.setOnClickListener(postStar(StarColor.Blue))
        binding.purpleStarButton.setOnClickListener(postStar(StarColor.Purple))
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

    private fun showBookmarkButton(layoutId: Int, dimenId: Int) =
        when (resources.configuration.orientation) {
            Configuration.ORIENTATION_LANDSCAPE -> showBookmarkButtonLandscape(layoutId, dimenId)
            else -> showBookmarkButtonPortrait(layoutId, dimenId)
        }


    private fun showBookmarkButtonPortrait(layoutId: Int, dimenId: Int) {
        val view = requireView()
        val layout = view.findViewById<View>(layoutId)
        val pos = requireContext().resources.getDimension(dimenId)

        layout.animate()
            .translationXBy(0f)
            .translationX(-pos)
            .alphaBy(0f)
            .alpha(1f)
            .duration = 100
    }

    private fun showBookmarkButtonLandscape(layoutId: Int, dimenId: Int) {
        val view = requireView()
        val layout = view.findViewById<View>(layoutId)
        val pos = requireContext().resources.getDimension(dimenId)

        layout.animate()
            .translationYBy(0f)
            .translationY(-pos)
            .alphaBy(0f)
            .alpha(1f)
            .duration = 100
    }

    // ------ ///

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

    private fun hideBookmarkButton(layoutId: Int) =
        when (resources.configuration.orientation) {
            Configuration.ORIENTATION_LANDSCAPE -> hideBookmarkButtonLandscape(layoutId)
            else -> hideBookmarkButtonPortrait(layoutId)
        }

    private fun hideBookmarkButtonPortrait(layoutId: Int) {
        val view = requireView()
        val layout = view.findViewById<View>(layoutId)
        layout.animate()
            .translationX(0f)
            .alpha(0f)
            .duration = 100
    }

    private fun hideBookmarkButtonLandscape(layoutId: Int) {
        val view = requireView()
        val layout = view.findViewById<View>(layoutId)
        layout.animate()
            .translationY(0f)
            .alpha(0f)
            .duration = 100
    }

    // ------ //

    private fun openStarMenu(binding: FragmentBookmarkDetail3Binding) {
        showBookmarkButton(
            R.id.bookmark_button,
            R.dimen.yellow_star_position
        )
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

        binding.showStarsButton.setImageResource(R.drawable.ic_baseline_close)
        TooltipCompat.setTooltipText(
            binding.showStarsButton,
            getString(R.string.dialog_close)
        )
    }

    private fun closeStarMenu(binding: FragmentBookmarkDetail3Binding) {
        hideBookmarkButton(R.id.bookmark_button)
        hideStarButton(R.id.purple_star_layout, R.id.purple_stars_count)
        hideStarButton(R.id.blue_star_layout, R.id.blue_stars_count)
        hideStarButton(R.id.red_star_layout, R.id.red_stars_count)
        hideStarButton(R.id.green_star_layout, R.id.green_stars_count)
        hideStarButton(R.id.yellow_star_layout, R.id.yellow_stars_count)

        binding.showStarsButton.setImageResource(R.drawable.ic_baseline_add)
        TooltipCompat.setTooltipText(
            binding.showStarsButton,
            getString(R.string.description_select_posting_star)
        )
    }
}
