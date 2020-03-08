package com.suihan74.satena.scenes.bookmarks2

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import androidx.activity.OnBackPressedCallback
import androidx.activity.addCallback
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.suihan74.satena.R
import com.suihan74.satena.scenes.bookmarks2.dialog.CustomTabSettingsDialog
import com.suihan74.satena.scenes.bookmarks2.tab.CustomTabViewModel
import com.suihan74.satena.scenes.post2.BookmarkPostActivity
import com.suihan74.utilities.toVisibility
import kotlinx.android.synthetic.main.fragment_bookmarks_fabs.view.*

class FloatingActionButtonsFragment :
    Fragment(),
    CustomTabSettingsDialog.Listener
{
    /** BookmarksActivityのViewModel */
    private val activityViewModel: BookmarksViewModel by lazy {
        ViewModelProvider(activity as BookmarksActivity)[BookmarksViewModel::class.java]
    }

    /** BookmarksFragmentの状態管理用ViewModel */
    private val fragmentViewModel: BookmarksFragmentViewModel by lazy {
        ViewModelProvider((activity as BookmarksActivity).bookmarksFragment)[BookmarksFragmentViewModel::class.java]
    }

    /** 現在表示中のタブのViewModel */
    private val tabViewModel
        get() = fragmentViewModel.selectedTabViewModel.value

    /** 戻るボタンの監視用コールバック */
    private lateinit var onBackPressedCallbackForKeyword: OnBackPressedCallback
    private lateinit var onBackPressedCallbackForScroll: OnBackPressedCallback

    companion object {
        fun createInstance() = FloatingActionButtonsFragment()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_bookmarks_fabs, container, false)

        // FAB初期化
        initFABs(view)

        // 「カスタム」タブでは設定ボタンを表示する
        fragmentViewModel.selectedTab.observe(this, Observer {
            if (it == BookmarksTabType.CUSTOM.ordinal) {
                view.custom_settings_button.show()
            }
            else {
                view.custom_settings_button.hide()
            }
        })

        // タブのブクマリストにサインインしているユーザーのブクマが存在するかを監視する
        fragmentViewModel.selectedTabViewModel.observe(this, Observer { vm ->
            vm.signedUserBookmark.observe(this, Observer { bookmark ->
                if (view.bookmarks_scroll_top_button.isShown) {
                    if (bookmark == null) {
                        view.bookmarks_scroll_my_bookmark_button.hide()
                    }
                    else {
                        view.bookmarks_scroll_my_bookmark_button.show()
                    }
                }
            })
        })

        activityViewModel.signedIn.observe(this, Observer {
            if (it) {
                view.bookmark_button.show()
            }
            else {
                view.bookmark_button.hide()
            }
        })

        return view
    }

    override fun onResume() {
        super.onResume()
        // 戻るボタンを監視
        onBackPressedCallbackForKeyword = requireActivity().onBackPressedDispatcher.addCallback(this, false) {
            val view = view!!
            if (view.bookmarks_search_text.visibility == View.VISIBLE) {
                view.bookmarks_search_text.visibility = View.GONE
                activityViewModel.filteringWord.postValue(null)
            }
            isEnabled = false
        }
        onBackPressedCallbackForScroll = requireActivity().onBackPressedDispatcher.addCallback(this, false) {
            val view = view!!
            view.bookmarks_scroll_top_button.hide()
            view.bookmarks_scroll_bottom_button.hide()
            view.bookmarks_scroll_my_bookmark_button.hide()
            isEnabled = false
        }
    }

    /** 画面下部メニューを初期化 */
    private fun initFABs(view: View) {
        val scrollFABs = arrayOf(
            view.bookmarks_scroll_top_button.apply {
                setOnClickListener {
                    tabViewModel?.scrollToTop()
                }
            },

            view.bookmarks_scroll_my_bookmark_button.apply {
                setOnClickListener {
                    // TODO: (暫定的な動作)詳細画面に遷移
                    tabViewModel?.signedUserBookmark?.value?.let { target ->
                        (activity as? BookmarksActivity)?.showBookmarkDetail(target)
                    }
                }
            },

            view.bookmarks_scroll_bottom_button.apply {
                setOnClickListener {
                    tabViewModel?.scrollToBottom()
                }
            }
        )

        // 初期状態を設定
        scrollFABs.forEach { it.hide() }
        if (fragmentViewModel.selectedTab.value != BookmarksTabType.CUSTOM.ordinal) {
            view.custom_settings_button.hide()
        }
        view.bookmarks_search_text.visibility = (!activityViewModel.filteringWord.value.isNullOrBlank()).toVisibility(View.GONE)

        // ブクマ投稿ボタン
        view.bookmark_button.hide()
        view.bookmark_button.setOnClickListener {
            val intent = Intent(context, BookmarkPostActivity::class.java).apply {
                putExtra(BookmarkPostActivity.EXTRA_ENTRY, activityViewModel.entry)
                putExtra(BookmarkPostActivity.EXTRA_EDITING_COMMENT, activityViewModel.editingComment)
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
                    if (tabViewModel?.signedUserBookmark?.value != null) {
                        view.bookmarks_scroll_my_bookmark_button.show()
                    }
                }
                onBackPressedCallbackForScroll.isEnabled = !it
            }
        }

        // キーワード抽出モードON/OFF切り替えボタン
        view.search_button.setOnClickListener {
            (view.bookmarks_search_text.visibility != View.VISIBLE).let { isOn ->
                view.bookmarks_search_text.visibility = isOn.toVisibility(View.GONE)

                // IMEのON/OFF
                (requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager)?.run {
                    if (isOn) {
                        view.bookmarks_search_text.requestFocus()
                        showSoftInput(view.bookmarks_search_text, 0)
                    }
                    else {
                        hideSoftInputFromWindow(requireActivity().currentFocus?.windowToken, InputMethodManager.HIDE_NOT_ALWAYS)
                    }
                }

                // 非表示にしたらフィルタリングを解除する
                if (isOn) {
                    activityViewModel.filteringWord.postValue(view.bookmarks_search_text.text.toString())
                }
                else {
                    activityViewModel.filteringWord.postValue(null)
                }
                onBackPressedCallbackForKeyword.isEnabled = isOn
            }
        }

        // 検索キーワード入力ボックス
        view.bookmarks_search_text.apply {
            setText(activityViewModel.filteringWord.value)
            addTextChangedListener {
                activityViewModel.filteringWord.postValue(it.toString())
            }

            setOnEditorActionListener { _, actionId, _ ->
                when (actionId) {
                    EditorInfo.IME_ACTION_DONE -> {
                        (requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager)?.run {
                            hideSoftInputFromWindow(requireActivity().currentFocus?.windowToken, InputMethodManager.HIDE_NOT_ALWAYS)
                        }
                        true
                    }

                    else -> false
                }
            }
        }

        // カスタムタブ設定ボタン
        view.custom_settings_button.setOnClickListener {
            val dialog = CustomTabSettingsDialog.createInstance()
            dialog.show(childFragmentManager, "custom_tab_settings_dialog")
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
