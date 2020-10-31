package com.suihan74.satena.scenes.bookmarks

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import androidx.activity.OnBackPressedCallback
import androidx.activity.addCallback
import androidx.fragment.app.Fragment
import com.suihan74.satena.R
import com.suihan74.satena.scenes.bookmarks.viewModel.BookmarksViewModel
import com.suihan74.satena.scenes.bookmarks.viewModel.ContentsViewModel
import com.suihan74.satena.scenes.bookmarks2.dialog.CustomTabSettingsDialog
import com.suihan74.satena.scenes.post.BookmarkPostActivity
import com.suihan74.utilities.bindings.setVisibility
import com.suihan74.utilities.extensions.*
import com.suihan74.utilities.showAllowingStateLoss
import kotlinx.android.synthetic.main.fragment_bookmarks_fabs.view.*

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
    private lateinit var onBackPressedCallbackForKeyword: OnBackPressedCallback
    private lateinit var onBackPressedCallbackForScroll: OnBackPressedCallback

    private val DIALOG_CUSTOM_TAB_SETTINGS by lazy { "DIALOG_CUSTOM_TAB_SETTINGS" }

    // ------ //

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_bookmarks_fabs, container, false)

        // FAB初期化
        initFABs(view)

        // 「カスタム」タブでは設定ボタンを表示する
        contentsViewModel.selectedTab.observe(viewLifecycleOwner) {
            if (it == BookmarksTabType.CUSTOM) {
                view.custom_settings_button.show()
            }
            else {
                view.custom_settings_button.hide()
            }
        }

        bookmarksViewModel.signedIn.observe(viewLifecycleOwner) {
            if (it) {
                view.bookmark_button.show()
            }
            else {
                view.bookmark_button.hide()
            }
        }

        return view
    }

    override fun onResume() {
        super.onResume()
        bookmarkButtonClicked = false
        // 戻るボタンを監視
        onBackPressedCallbackForKeyword = requireActivity().onBackPressedDispatcher.addCallback(this, false) {
            requireView().let { view ->
                if (view.bookmarks_search_text.visibility == View.VISIBLE) {
                    view.bookmarks_search_text.visibility = View.GONE
                    bookmarksViewModel.filteringText.value = null
                }
            }
            isEnabled = false
        }
        onBackPressedCallbackForScroll = requireActivity().onBackPressedDispatcher.addCallback(this, false) {
            requireView().let { view ->
                view.bookmarks_scroll_top_button.hide()
                view.bookmarks_scroll_bottom_button.hide()
                view.bookmarks_open_my_bookmark_button.hide()
            }
            isEnabled = false
        }
    }

    /** 画面下部メニューを初期化 */
    private fun initFABs(view: View) {
        val scrollFABs = arrayOf(
            view.bookmarks_scroll_top_button,
            view.bookmarks_open_my_bookmark_button,
            view.bookmarks_scroll_bottom_button
        )

        view.bookmarks_scroll_top_button.setOnClickListener {
            contentsViewModel.scrollCurrentTabToTop()
        }

        view.bookmarks_open_my_bookmark_button.setOnClickListener {
            // 自分のブコメの詳細画面に遷移
            bookmarksViewModel.userBookmark?.let { bookmark ->
                activity.alsoAs<BookmarkDetailOpenable> { container ->
                    contentsViewModel.openBookmarkDetail(container, bookmark)
                }
            }

            scrollFABs.forEach { fab -> fab.hide() }
            onBackPressedCallbackForScroll.isEnabled = false
        }

        view.bookmarks_scroll_bottom_button.setOnClickListener {
            contentsViewModel.scrollCurrentTabToBottom()
        }

        // 初期状態を設定
        scrollFABs.forEach { it.hide() }
        if (contentsViewModel.selectedTab.value != BookmarksTabType.CUSTOM) {
            view.custom_settings_button.hide()
        }
        view.bookmarks_search_text.setVisibility(!bookmarksViewModel.filteringText.value.isNullOrBlank())

        // ブクマ投稿ボタン
        view.bookmark_button.hide()
        view.bookmark_button.setOnClickListener {
            if (bookmarkButtonClicked) return@setOnClickListener
            bookmarkButtonClicked = true

            val intent = Intent(context, BookmarkPostActivity::class.java).also {
                it.putObjectExtra(BookmarkPostActivity.EXTRA_ENTRY, bookmarksViewModel.entry.value)
                it.putObjectExtra(BookmarkPostActivity.EXTRA_EDIT_DATA, bookmarksViewModel.editData)
            }
            activity?.startActivityForResult(intent, BookmarkPostActivity.REQUEST_CODE)
        }

        // スクロールメニュー
        view.bookmarks_scroll_menu_button.setOnClickListener {
            view.bookmarks_scroll_top_button.isShown.let {
                if (it) {
                    scrollFABs.forEach { fab -> fab.hide() }
                }
                else {
                    view.bookmarks_scroll_top_button.show()
                    view.bookmarks_scroll_bottom_button.show()
                    if (bookmarksViewModel.userBookmark != null) {
                        view.bookmarks_open_my_bookmark_button.show()
                    }
                }
                onBackPressedCallbackForScroll.isEnabled = !it
            }
        }

        // キーワード抽出モードON/OFF切り替えボタン
        view.search_button.setOnClickListener {
            (view.bookmarks_search_text.visibility != View.VISIBLE).let { isOn ->
                view.bookmarks_search_text.visibility = isOn.toVisibility(View.GONE)

                // IMEのON/OFF + 非表示にしたらフィルタリングを解除する
                val activity = requireActivity()
                if (isOn) {
                    activity.showSoftInputMethod(view.bookmarks_search_text)
                    bookmarksViewModel.filteringText.value = view.bookmarks_search_text.text.toString()
                }
                else {
                    activity.hideSoftInputMethod()
                    bookmarksViewModel.filteringText.value = null
                }
                onBackPressedCallbackForKeyword.isEnabled = isOn
            }
        }

        // 検索キーワード入力ボックス
        view.bookmarks_search_text.setOnEditorActionListener { _, actionId, _ ->
            when (actionId) {
                EditorInfo.IME_ACTION_DONE -> {
                    requireActivity().hideSoftInputMethod()
                    true
                }

                else -> false
            }
        }

        // カスタムタブ設定ボタン
        view.custom_settings_button.setOnClickListener {
            val dialog = CustomTabSettingsDialog.createInstance()
            dialog.showAllowingStateLoss(childFragmentManager, DIALOG_CUSTOM_TAB_SETTINGS)
        }
    }


    // --- CustomTabSettingsDialog --- //

    /*
    fun getCustomTabSettings() =
        (tabViewModel as CustomTabViewModel).settings!!

    fun getUserTags() =
        bookmarksViewModel.userTags.value ?: emptyList()

    fun onPositiveButtonClicked(set: CustomTabViewModel.Settings) {
        (tabViewModel as? CustomTabViewModel)?.saveSettings(set)
    }
    */
}