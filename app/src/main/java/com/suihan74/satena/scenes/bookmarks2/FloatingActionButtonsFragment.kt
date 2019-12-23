package com.suihan74.satena.scenes.bookmarks2

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.activity.addCallback
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.lifecycle.viewModelScope
import com.suihan74.satena.R
import com.suihan74.satena.models.BookmarksTabType
import com.suihan74.satena.scenes.post.BookmarkPostActivity
import com.suihan74.utilities.toVisibility
import kotlinx.android.synthetic.main.fragment_bookmarks_fabs.view.*
import kotlinx.android.synthetic.main.fragment_bookmarks_fabs.view.search_button
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class FloatingActionButtonsFragment : Fragment() {
    /** BookmarksActivityのViewModel */
    private val activityViewModel: BookmarksViewModel by lazy {
        ViewModelProviders.of(activity as BookmarksActivity)[BookmarksViewModel::class.java]
    }

    /** BookmarksFragmentの状態管理用ViewModel */
    private val fragmentViewModel: BookmarksFragmentViewModel by lazy {
        ViewModelProviders.of((activity as BookmarksActivity).bookmarksFragment)[BookmarksFragmentViewModel::class.java]
    }

    /** 現在表示中のタブのViewModel */
    private val tabViewModel
        get() = fragmentViewModel.selectedTabViewModel.value

    /** 戻るボタンの監視用コールバック */
    private lateinit var onBackPressedCallback: OnBackPressedCallback

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

        return view
    }

    override fun onResume() {
        super.onResume()
        // 戻るボタンを監視
        onBackPressedCallback = requireActivity().onBackPressedDispatcher.addCallback(this, false) {
            val view = view!!
            if (view.bookmarks_search_text.visibility == View.VISIBLE) {
                view.bookmarks_search_text.visibility = View.GONE
                activityViewModel.filteringWord.postValue(null)
                isEnabled = false
            }
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
                    tabViewModel?.signedUserBookmark?.value?.let { target ->
                        // TODO: 未ロードの場合どうするかを考える
                        activityViewModel.loadNextRecentToUser(target.user).invokeOnCompletion { e ->
                            if (e == null) {
                                // TODO: postValueされるまえに呼ばれてしまうのでロードが挟まるとスクロールしない問題
                                tabViewModel?.scrollTo(target)
                            }
                        }
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
        view.bookmarks_search_text.visibility = (activityViewModel.filteringWord.value != null).toVisibility(View.GONE)

        // ブクマ投稿ボタン
        view.bookmark_button.setOnClickListener {
            val intent = Intent(context, BookmarkPostActivity::class.java).apply {
                putExtra(BookmarkPostActivity.EXTRA_ENTRY, activityViewModel.entry)
            }
            startActivity(intent)
        }

        // スクロールメニュー
        view.bookmarks_scroll_menu_button.setOnClickListener {
            if (view.bookmarks_scroll_top_button.isShown) {
                scrollFABs.forEach { it.hide() }
            }
            else {
                view.bookmarks_scroll_top_button.show()
                view.bookmarks_scroll_bottom_button.show()
                if (tabViewModel?.signedUserBookmark?.value != null) {
                    view.bookmarks_scroll_my_bookmark_button.show()
                }
            }
        }

        // キーワード抽出モードON/OFF切り替えボタン
        view.search_button.setOnClickListener {
            (view.bookmarks_search_text.visibility != View.VISIBLE).let {
                view.bookmarks_search_text.visibility = it.toVisibility(View.GONE)
                // 非表示にしたらフィルタリングを解除する
                if (it) {
                    activityViewModel.filteringWord.postValue(view.bookmarks_search_text.text.toString())
                }
                else {
                    activityViewModel.filteringWord.postValue(null)
                }
                onBackPressedCallback.isEnabled = it
            }
        }

        // 検索キーワード入力ボックス
        view.bookmarks_search_text.apply {
            // TODO: 無効化した後で画面回転すると何故か有効な値が再代入される
            setText(activityViewModel.filteringWord.value)
            addTextChangedListener {
                activityViewModel.filteringWord.postValue(it.toString())
            }
        }
    }
}
