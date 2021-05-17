package com.suihan74.satena.scenes.bookmarks

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import androidx.activity.OnBackPressedCallback
import androidx.activity.addCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import com.suihan74.satena.databinding.FragmentBookmarksFabs3Binding
import com.suihan74.satena.scenes.bookmarks.viewModel.BookmarksViewModel
import com.suihan74.satena.scenes.bookmarks.viewModel.ContentsViewModel
import com.suihan74.satena.scenes.post.BookmarkPostActivity
import com.suihan74.utilities.bindings.setVisibility
import com.suihan74.utilities.extensions.*

/** 画面下部FAB部分のFragment */
class FloatingActionButtonsFragment : Fragment() {

    companion object {
        fun createInstance() = FloatingActionButtonsFragment()
    }

    // ------ //

    private val bookmarksActivity : BookmarksActivity
        get() = requireActivity() as BookmarksActivity

    private val bookmarksViewModel : BookmarksViewModel
        get() = bookmarksActivity.bookmarksViewModel

    private val contentsViewModel : ContentsViewModel
        get() = bookmarksActivity.contentsViewModel

    /** ブクマ投稿ダイアログを開くボタンを複数回押されてもダイアログが複数出ないようにする */
    private var bookmarkButtonClicked = false

    /** 戻るボタンの監視用コールバック */
    private var onBackPressedCallbackForKeyword : OnBackPressedCallback? = null
    private var onBackPressedCallbackForScroll : OnBackPressedCallback? = null

    /** ブクマ投稿画面遷移用ランチャ */
    private val postBookmarkLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        bookmarksViewModel.onActivityResult(BookmarkPostActivity.REQUEST_CODE, result.resultCode, result.data)
    }

    // ------ //

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val binding = FragmentBookmarksFabs3Binding.inflate(
            inflater,
            container,
            false
        ).also {
            it.bookmarksViewModel = bookmarksViewModel
            it.contentsViewModel = contentsViewModel
            it.lifecycleOwner = viewLifecycleOwner
        }

        // FAB初期化
        initFABs(binding)

        // 戻るボタンを監視
        onBackPressedCallbackForKeyword = requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            false
        ) {
            if (binding.bookmarksSearchText.visibility == View.VISIBLE) {
                binding.bookmarksSearchText.visibility = View.GONE
                bookmarksViewModel.filteringText.value = null
            }
            isEnabled = false
        }

        onBackPressedCallbackForScroll = requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            false
        ) {
            binding.bookmarksScrollTopButton.hide()
            binding.bookmarksScrollBottomButton.hide()
            binding.bookmarksOpenMyBookmarkButton.hide()
            isEnabled = false
        }

        return binding.root
    }

    override fun onResume() {
        super.onResume()
        bookmarkButtonClicked = false
    }

    /** 画面下部メニューを初期化 */
    private fun initFABs(binding: FragmentBookmarksFabs3Binding) {
        val scrollFABs = arrayOf(
            binding.bookmarksScrollTopButton,
            binding.bookmarksOpenMyBookmarkButton,
            binding.bookmarksScrollBottomButton
        )

        binding.bookmarksScrollTopButton.setOnClickListener {
            contentsViewModel.scrollCurrentTabToTop()
        }

        binding.bookmarksOpenMyBookmarkButton.setOnClickListener {
            // 自分のブコメの詳細画面に遷移
            bookmarksViewModel.userBookmark?.let { bookmark ->
                activity.alsoAs<BookmarkDetailOpenable> { container ->
                    contentsViewModel.openBookmarkDetail(container, bookmark)
                }
            }

            scrollFABs.forEach { fab -> fab.hide() }
            onBackPressedCallbackForScroll?.isEnabled = false
        }

        binding.bookmarksScrollBottomButton.setOnClickListener {
            contentsViewModel.scrollCurrentTabToBottom()
        }

        // 初期状態を設定
        scrollFABs.forEach { it.hide() }
        if (contentsViewModel.selectedTab.value != BookmarksTabType.CUSTOM) {
            binding.customSettingsButton.hide()
        }
        binding.bookmarksSearchText.setVisibility(!bookmarksViewModel.filteringText.value.isNullOrBlank())

        // ブクマ投稿ボタン
        binding.bookmarkButton.setOnClickListener {
            if (bookmarkButtonClicked) return@setOnClickListener
            bookmarkButtonClicked = true

            val intent = Intent(context, BookmarkPostActivity::class.java).also {
                it.putObjectExtra(BookmarkPostActivity.EXTRA_ENTRY, bookmarksViewModel.entry.value)
                it.putObjectExtra(BookmarkPostActivity.EXTRA_EDIT_DATA, bookmarksViewModel.editData)
            }
            postBookmarkLauncher.launch(intent)
        }

        // スクロールメニュー
        binding.bookmarksScrollMenuButton.setOnClickListener {
            binding.bookmarksScrollTopButton.isShown.let {
                if (it) {
                    scrollFABs.forEach { fab -> fab.hide() }
                }
                else {
                    binding.bookmarksScrollTopButton.show()
                    binding.bookmarksScrollBottomButton.show()
                    if (bookmarksViewModel.userBookmark != null) {
                        binding.bookmarksOpenMyBookmarkButton.show()
                    }
                }
                onBackPressedCallbackForScroll?.isEnabled = !it
            }
        }

        // キーワード抽出モードON/OFF切り替えボタン
        binding.searchButton.setOnClickListener {
            binding.bookmarksSearchText.let { bookmarksSearchText ->
                (bookmarksSearchText.visibility != View.VISIBLE).let { isOn ->
                    bookmarksSearchText.visibility = isOn.toVisibility(View.GONE)

                    // IMEのON/OFF + 非表示にしたらフィルタリングを解除する
                    val activity = requireActivity()
                    if (isOn) {
                        activity.showSoftInputMethod(bookmarksSearchText)
                    }
                    else {
                        activity.hideSoftInputMethod()
                        bookmarksViewModel.filteringText.value = null
                    }
                    onBackPressedCallbackForKeyword?.isEnabled = isOn
                }
            }
        }

        // 検索キーワード入力ボックス
        binding.bookmarksSearchText.setOnEditorActionListener { _, actionId, _ ->
            when (actionId) {
                EditorInfo.IME_ACTION_DONE -> {
                    requireActivity().hideSoftInputMethod()
                    true
                }

                else -> false
            }
        }

        // カスタムタブ設定ボタン
        binding.customSettingsButton.setOnClickListener {
            bookmarksViewModel.openCustomTabSettingsDialog(childFragmentManager)
        }
    }
}
