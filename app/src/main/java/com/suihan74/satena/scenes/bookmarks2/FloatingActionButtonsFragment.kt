package com.suihan74.satena.scenes.bookmarks2

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import androidx.activity.OnBackPressedCallback
import androidx.activity.addCallback
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.lifecycle.observe
import com.suihan74.satena.databinding.FragmentBookmarksFabsBinding
import com.suihan74.satena.scenes.bookmarks2.dialog.CustomTabSettingsDialog
import com.suihan74.satena.scenes.bookmarks2.tab.CustomTabViewModel
import com.suihan74.satena.scenes.post.BookmarkPostActivity
import com.suihan74.utilities.extensions.*
import com.suihan74.utilities.showAllowingStateLoss

class FloatingActionButtonsFragment :
    Fragment(),
    CustomTabSettingsDialog.Listener
{
    companion object {
        fun createInstance() = FloatingActionButtonsFragment()
    }

    // ------ //

    /** BookmarksActivityのViewModel */
    private val activityViewModel: BookmarksViewModel
        get() = (requireActivity() as BookmarksActivity).viewModel

    /** BookmarksFragmentの状態管理用ViewModel */
    private val fragmentViewModel: BookmarksFragment.BookmarksFragmentViewModel
        get() = (requireActivity() as BookmarksActivity).bookmarksFragment.viewModel

    /** 現在表示中のタブのViewModel */
    private val tabViewModel
        get() = fragmentViewModel.selectedTabViewModel.value

    // ------ //

    private var _binding : FragmentBookmarksFabsBinding? = null
    private val binding
        get() = _binding!!

    // ------ //

    /** ブクマ投稿ダイアログを開くボタンを複数回押されてもダイアログが複数出ないようにする */
    private var bookmarkButtonClicked = false

    /** 戻るボタンの監視用コールバック */
    private lateinit var onBackPressedCallbackForKeyword: OnBackPressedCallback
    private lateinit var onBackPressedCallbackForScroll: OnBackPressedCallback

    private val DIALOG_CUSTOM_TAB_SETTINGS by lazy { "DIALOG_CUSTOM_TAB_SETTINGS" }

    // ------ //

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val binding = FragmentBookmarksFabsBinding.inflate(
            inflater,
            container,
            false
        )
        this._binding = binding

        // FAB初期化
        initFABs(binding)

        // 「カスタム」タブでは設定ボタンを表示する
        fragmentViewModel.selectedTab.observe(viewLifecycleOwner) {
            if (it == BookmarksTabType.CUSTOM.ordinal) {
                binding.customSettingsButton.show()
            }
            else {
                binding.customSettingsButton.hide()
            }
        }

        // タブのブクマリストにサインインしているユーザーのブクマが存在するかを監視する
        fragmentViewModel.selectedTabViewModel.observe(viewLifecycleOwner) { vm ->
            vm.signedUserBookmark.observe(viewLifecycleOwner) { bookmark ->
                if (binding.bookmarksScrollTopButton.isShown) {
                    if (bookmark == null) {
                        binding.bookmarksOpenMyBookmarkButton.hide()
                    }
                    else {
                        binding.bookmarksOpenMyBookmarkButton.show()
                    }
                }
            }
        }

        activityViewModel.signedIn.observe(viewLifecycleOwner) {
            if (it) {
                binding.bookmarkButton.show()
            }
            else {
                binding.bookmarkButton.hide()
            }
        }

        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onResume() {
        super.onResume()
        bookmarkButtonClicked = false
        // 戻るボタンを監視
        onBackPressedCallbackForKeyword = requireActivity().onBackPressedDispatcher.addCallback(this, false) {
            binding.bookmarksSearchText.let {
                if (it.visibility == View.VISIBLE) {
                    it.visibility = View.GONE
                    activityViewModel.filteringWord.postValue(null)
                }
            }
            isEnabled = false
        }
        onBackPressedCallbackForScroll = requireActivity().onBackPressedDispatcher.addCallback(this, false) {
            binding.bookmarksScrollTopButton.hide()
            binding.bookmarksScrollBottomButton.hide()
            binding.bookmarksOpenMyBookmarkButton.hide()
            isEnabled = false
        }
    }

    /** 画面下部メニューを初期化 */
    private fun initFABs(binding: FragmentBookmarksFabsBinding) {
        val scrollFABs = arrayOf(
            binding.bookmarksScrollTopButton,
            binding.bookmarksOpenMyBookmarkButton,
            binding.bookmarksScrollBottomButton
        )

        binding.bookmarksScrollTopButton.setOnClickListener {
            tabViewModel?.scrollToTop()
        }

        binding.bookmarksOpenMyBookmarkButton.setOnClickListener {
            // 自分のブコメの詳細画面に遷移
            tabViewModel?.signedUserBookmark?.value?.let { target ->
                activity.alsoAs<BookmarksActivity> { activity ->
                    activityViewModel.showBookmarkDetail(activity, target)
                }
            }
            scrollFABs.forEach { fab -> fab.hide() }
            onBackPressedCallbackForScroll.isEnabled = false
        }

        binding.bookmarksScrollBottomButton.setOnClickListener {
            tabViewModel?.scrollToBottom()
        }

        // 初期状態を設定
        scrollFABs.forEach { it.hide() }
        if (fragmentViewModel.selectedTab.value != BookmarksTabType.CUSTOM.ordinal) {
            binding.customSettingsButton.hide()
        }
        binding.bookmarksSearchText.visibility = (!activityViewModel.filteringWord.value.isNullOrBlank()).toVisibility(View.GONE)

        // ブクマ投稿ボタン
        binding.bookmarkButton.hide()
        binding.bookmarkButton.setOnClickListener {
            if (bookmarkButtonClicked) return@setOnClickListener
            bookmarkButtonClicked = true

            val intent = Intent(context, BookmarkPostActivity::class.java).also {
                it.putObjectExtra(BookmarkPostActivity.EXTRA_ENTRY, activityViewModel.entry.value)
                it.putObjectExtra(BookmarkPostActivity.EXTRA_EDIT_DATA, activityViewModel.editData)
            }
            activity?.startActivityForResult(intent, BookmarkPostActivity.REQUEST_CODE)
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
                    if (tabViewModel?.signedUserBookmark?.value != null) {
                        binding.bookmarksOpenMyBookmarkButton.show()
                    }
                }
                onBackPressedCallbackForScroll.isEnabled = !it
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
                        activityViewModel.filteringWord.postValue(bookmarksSearchText.text.toString())
                    }
                    else {
                        activity.hideSoftInputMethod()
                        activityViewModel.filteringWord.postValue(null)
                    }
                    onBackPressedCallbackForKeyword.isEnabled = isOn
                }
            }
        }

        // 検索キーワード入力ボックス
        binding.bookmarksSearchText.apply {
            setText(activityViewModel.filteringWord.value)
            addTextChangedListener {
                activityViewModel.filteringWord.postValue(it.toString())
            }

            setOnEditorActionListener { _, actionId, _ ->
                when (actionId) {
                    EditorInfo.IME_ACTION_DONE -> {
                        requireActivity().hideSoftInputMethod()
                        true
                    }

                    else -> false
                }
            }
        }

        // カスタムタブ設定ボタン
        binding.customSettingsButton.setOnClickListener {
            val dialog = CustomTabSettingsDialog.createInstance()
            dialog.showAllowingStateLoss(childFragmentManager, DIALOG_CUSTOM_TAB_SETTINGS)
        }
    }


    // --- CustomTabSettingsDialog --- //

    override fun getCustomTabSettings() =
        (tabViewModel as CustomTabViewModel).settings!!

    override fun getUserTags() =
        activityViewModel.userTags.value ?: emptyList()

    override fun onPositiveButtonClicked(set: CustomTabViewModel.Settings) {
        (tabViewModel as? CustomTabViewModel)?.saveSettings(set)
    }
}
