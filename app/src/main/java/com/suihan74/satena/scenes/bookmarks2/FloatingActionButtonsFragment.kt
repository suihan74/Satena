package com.suihan74.satena.scenes.bookmarks2

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.suihan74.satena.R
import com.suihan74.satena.models.BookmarksTabType
import com.suihan74.satena.scenes.post.BookmarkPostActivity
import com.suihan74.utilities.toVisibility
import kotlinx.android.synthetic.main.fragment_bookmarks_fabs.view.*

class FloatingActionButtonsFragment : Fragment() {
    /** BookmarksActivityのViewModel */
    private val activityViewModel: BookmarksViewModel by lazy {
        ViewModelProviders.of(activity as BookmarksActivity)[BookmarksViewModel::class.java]
    }

    /** BookmarksFragmentの状態管理用ViewModel */
    private val viewModel: BookmarksFragmentViewModel by lazy {
        ViewModelProviders.of((activity as BookmarksActivity).bookmarksFragment)[BookmarksFragmentViewModel::class.java]
    }

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
        viewModel.selectedTab.observe(this, Observer {
            if (it == BookmarksTabType.CUSTOM.ordinal) {
                view.custom_settings_button.show()
            }
            else {
                view.custom_settings_button.hide()
            }
        })

        return view
    }

    /** 画面下部メニューを初期化 */
    private fun initFABs(view: View) {
        val scrollFABs = arrayOf(
            view.bookmarks_scroll_bottom_button,
            view.bookmarks_scroll_my_bookmark_button,
            view.bookmarks_scroll_top_button
        )

        // 初期状態では隠しておくものたち
        view.bookmarks_search_text.visibility = View.GONE
        scrollFABs.forEach { it.hide() }
        if (viewModel.selectedTab.value != BookmarksTabType.CUSTOM.ordinal) {
            view.custom_settings_button.hide()
        }

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
                scrollFABs.forEach { it.show() }
            }
        }

        // 検索ボックス
        view.search_button.setOnClickListener {
            view.bookmarks_search_text.visibility =
                (view.bookmarks_search_text.visibility != View.VISIBLE).toVisibility(View.GONE)
        }
    }
}
