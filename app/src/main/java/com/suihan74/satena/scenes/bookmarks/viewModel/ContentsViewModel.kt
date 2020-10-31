package com.suihan74.satena.scenes.bookmarks.viewModel

import android.content.Context
import android.view.View
import androidx.appcompat.widget.Toolbar
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.view.updateLayoutParams
import androidx.fragment.app.Fragment
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.viewpager.widget.ViewPager
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.behavior.HideBottomViewOnScrollBehavior
import com.google.android.material.tabs.TabLayout
import com.suihan74.hatenaLib.Bookmark
import com.suihan74.satena.R
import com.suihan74.satena.models.PreferenceKey
import com.suihan74.satena.scenes.bookmarks.BookmarkDetailOpenable
import com.suihan74.satena.scenes.bookmarks.BookmarksActivity
import com.suihan74.satena.scenes.bookmarks.BookmarksTabAdapter
import com.suihan74.satena.scenes.bookmarks.BookmarksTabType
import com.suihan74.satena.scenes.bookmarks.detail.BookmarkDetailFragment
import com.suihan74.utilities.PreferenceLiveData
import com.suihan74.utilities.SafeSharedPreferences
import com.suihan74.utilities.ScrollableToBottom
import com.suihan74.utilities.ScrollableToTop
import com.suihan74.utilities.extensions.alsoAs
import com.suihan74.utilities.extensions.setOnTabLongClickListener
import com.suihan74.utilities.extensions.showToast

/**
 * タブ制御など画面の状態管理用のViewModel
 */
class ContentsViewModel(
    private val prefs : SafeSharedPreferences<PreferenceKey>
) : ViewModel() {

    /** 画面テーマ */
    val themeId =
        if (prefs.getBoolean(PreferenceKey.DARK_THEME)) R.style.AppTheme_Dark
        else R.style.AppTheme_Light

    /** 現在表示中のタブ */
    val selectedTab by lazy {
        MutableLiveData(
            BookmarksTabType.fromOrdinal(initialTabOrdinal.value ?: 0)
        )
    }

    /**
     *  最初に表示するタブ
     *
     *  タブ長押しで変更される可能性があるので一応状態を通知させる
     */
    private val initialTabOrdinal =
        PreferenceLiveData(prefs, PreferenceKey.BOOKMARKS_INITIAL_TAB) { p, key ->
            p.getInt(key)
        }

    /** タブ長押しで初期表示タブを変更する */
    private val changeHomeTabByLongClick by lazy {
        prefs.getBoolean(PreferenceKey.BOOKMARKS_CHANGE_HOME_BY_LONG_TAPPING_TAB)
    }

    /** スクロールでツールバーを隠す */
    private val hideToolbarByScrolling by lazy {
        prefs.getBoolean(PreferenceKey.BOOKMARKS_HIDING_TOOLBAR_BY_SCROLLING)
    }

    /** スクロールで画面下部ボタンを隠す */
    private val hideButtonsByScrolling by lazy {
        prefs.getBoolean(PreferenceKey.BOOKMARKS_HIDING_BUTTONS_BY_SCROLLING)
    }

    /** ドロワーの配置 */
    val drawerGravity by lazy {
        prefs.getInt(PreferenceKey.DRAWER_GRAVITY)
    }

    // ------ //

    /** 現在アクティブなタブFragmentを取得する処理 */
    private var currentTabFragmentSelector : (()->Fragment?)? = null

    /** FAB部分を強制的に表示する処理 */
    private var showFloatingActionButtons : (()->Unit)? = null

    /** タブ制御を初期化 */
    fun initializeTabPager(
        activity: BookmarksActivity,
        viewPager: ViewPager,
        tabLayout: TabLayout
    ) {
        val adapter = BookmarksTabAdapter(activity, activity.supportFragmentManager)
        viewPager.adapter = adapter

        tabLayout.also { layout ->
            layout.setupWithViewPager(viewPager)
            layout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {

                /** タブを切替え */
                override fun onTabSelected(tab: TabLayout.Tab?) {
                    tab?.position?.let {
                        selectedTab.value = BookmarksTabType.fromOrdinal(it)
                    }
                    showFloatingActionButtons?.invoke()
                }

                override fun onTabUnselected(p0: TabLayout.Tab?) {}

                /** タブ再選択で最新までスクロール */
                override fun onTabReselected(tab: TabLayout.Tab?) {
                    val idx = tab?.position ?: return
                    adapter.instantiateItem(viewPager, idx).alsoAs<ScrollableToTop> { fragment ->
                        fragment.scrollToTop()
                    }
                    showFloatingActionButtons?.invoke()
                }
            })

            // タブを長押しで最初に表示するタブを変更
            layout.setOnTabLongClickListener { idx ->
                if (!changeHomeTabByLongClick) return@setOnTabLongClickListener false
                if (initialTabOrdinal.value == idx) return@setOnTabLongClickListener false

                initialTabOrdinal.value = idx
                activity.showToast(
                    R.string.msg_bookmarks_initial_tab_changed,
                    activity.getString(BookmarksTabType.fromOrdinal(idx).textId)
                )
                return@setOnTabLongClickListener true
            }
        }

        currentTabFragmentSelector = {
            val idx = tabLayout.selectedTabPosition
            adapter.instantiateItem(viewPager, idx) as? Fragment
        }
    }

    /** ツールバーやボタンをスクロールで隠す設定を反映する */
    fun setScrollingBehavior(context: Context, toolbar: Toolbar, buttonsArea: View) {
        // スクロールでツールバーを隠す
        toolbar.updateLayoutParams<AppBarLayout.LayoutParams> {
            scrollFlags =
                if (hideToolbarByScrolling)
                    AppBarLayout.LayoutParams.SCROLL_FLAG_SCROLL or AppBarLayout.LayoutParams.SCROLL_FLAG_ENTER_ALWAYS
                else
                    0
        }

        // スクロールでボタンを隠す
        buttonsArea.updateLayoutParams<CoordinatorLayout.LayoutParams> {
            behavior =
                if (hideButtonsByScrolling)
                    HideBottomViewOnScrollBehavior<View>(context, null)
                else
                    null
        }

        showFloatingActionButtons = {
            buttonsArea.layoutParams.alsoAs<CoordinatorLayout.LayoutParams> { layoutParams ->
                layoutParams.behavior.alsoAs<HideBottomViewOnScrollBehavior<View>> { behavior ->
                    behavior.slideUp(buttonsArea)
                }
            }
        }
    }

    // ------ //

    /** 現在アクティブなタブのブクマリストを一番上までスクロールする */
    fun scrollCurrentTabToTop() {
        currentTabFragmentSelector?.invoke().alsoAs<ScrollableToTop> {
            it.scrollToTop()
        }
    }

    /** 現在アクティブなタブのブクマリストを一番下までスクロールする */
    fun scrollCurrentTabToBottom() {
        currentTabFragmentSelector?.invoke().alsoAs<ScrollableToBottom> {
            it.scrollToBottom()
        }
    }

    // ------ //

    /** 選択したブクマの詳細情報表示画面を開く */
    fun openBookmarkDetail(container: BookmarkDetailOpenable, bookmark: Bookmark) {
        val fragmentManager = container.fragmentManager

        val backStackName = "detail: ${bookmark.user}"
        val currentTop = fragmentManager.findFragmentById(container.bookmarkDetailFrameLayoutId)
        val currentTopTag = currentTop?.tag
        if (currentTopTag != null && currentTopTag == backStackName) {
            return
        }

        val bookmarkDetailFragment = BookmarkDetailFragment.createInstance(bookmark)
        fragmentManager.beginTransaction()
            .add(container.bookmarkDetailFrameLayoutId, bookmarkDetailFragment, backStackName)
            .addToBackStack(backStackName)
            .commitAllowingStateLoss()
    }
}
