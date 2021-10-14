package com.suihan74.satena.scenes.bookmarks.viewModel

import android.content.Context
import android.util.Log
import android.view.View
import androidx.appcompat.widget.Toolbar
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.view.updateLayoutParams
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.behavior.HideBottomViewOnScrollBehavior
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.suihan74.hatenaLib.Bookmark
import com.suihan74.hatenaLib.Entry
import com.suihan74.satena.R
import com.suihan74.satena.models.ExtraScrollingAlignment
import com.suihan74.satena.models.PreferenceKey
import com.suihan74.satena.models.Theme
import com.suihan74.satena.scenes.bookmarks.BookmarkDetailOpenable
import com.suihan74.satena.scenes.bookmarks.BookmarksTabAdapter
import com.suihan74.satena.scenes.bookmarks.BookmarksTabType
import com.suihan74.satena.scenes.bookmarks.detail.BookmarkDetailFragment
import com.suihan74.utilities.PreferenceLiveData
import com.suihan74.utilities.SafeSharedPreferences
import com.suihan74.utilities.ScrollableToBottom
import com.suihan74.utilities.ScrollableToTop
import com.suihan74.utilities.extensions.ContextExtensions.showToast
import com.suihan74.utilities.extensions.alsoAs
import com.suihan74.utilities.extensions.setOnTabLongClickListener
import com.suihan74.utilities.extensions.touchSlop
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * タブ制御など画面の状態管理用のViewModel
 */
class ContentsViewModel(
    private val prefs : SafeSharedPreferences<PreferenceKey>
) : ViewModel() {

    /** 画面テーマ */
    val themeId = Theme.themeId(prefs)

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

    /** タブページャのスワイプ感度 */
    private val pagerScrollSensitivity by lazy {
        prefs.getFloat(PreferenceKey.BOOKMARKS_PAGER_SCROLL_SENSITIVITY)
    }

    /** ドロワーの配置 */
    val drawerGravity by lazy {
        prefs.getInt(PreferenceKey.DRAWER_GRAVITY)
    }

    /** エクストラスクロール機能のツマミの配置 */
    val extraScrollingAlignment
        get() = ExtraScrollingAlignment.fromId(prefs.getInt(PreferenceKey.BOOKMARKS_EXTRA_SCROLL_ALIGNMENT))

    /** エクストラスクロール機能のツマミの表示状態 */
    val extraScrollBarVisibility =
        MutableLiveData(extraScrollingAlignment != ExtraScrollingAlignment.NONE)

    val extraScrollProgress = MutableLiveData(0f)

    suspend fun updateExtraScrollBarVisibility(visibility: Boolean) = withContext(Dispatchers.Main) {
        extraScrollBarVisibility.value = extraScrollingAlignment != ExtraScrollingAlignment.NONE && visibility
    }

    // ------ //

    /** 現在アクティブなタブFragmentを取得する処理 */
    private var currentTabFragmentSelector : (()->Fragment?)? = null

    /** FAB部分を強制的に表示する処理 */
    private var showFloatingActionButtons : (()->Unit)? = null

    /** タブ制御を初期化 */
    fun initializeTabPager(
        activity: FragmentActivity,
        viewPager: ViewPager2,
        tabLayout: TabLayout
    ) {
        val adapter = BookmarksTabAdapter(activity)
        viewPager.adapter = adapter

        tabLayout.also { layout ->
            TabLayoutMediator(tabLayout, viewPager) { tab, position ->
                tab.text = activity.getText(adapter.getTitleId(position))
            }.attach()
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
                    adapter.currentFragment(viewPager).alsoAs<ScrollableToTop> { fragment ->
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

        currentTabFragmentSelector = { adapter.currentFragment(viewPager) }

        // 初期タブを設定
        viewPager.setCurrentItem(selectedTab.value?.ordinal ?: 0 , false)

        // スクロール感度を設定
        resetPagerScrollSensitivity(viewPager)
    }

    private var defaultPagerTouchSlop : Int? = null
    private fun resetPagerScrollSensitivity(pager: ViewPager2) {
        runCatching {
            if (defaultPagerTouchSlop == null) defaultPagerTouchSlop = pager.touchSlop
            val scale = 1 / pagerScrollSensitivity
            pager.touchSlop = (defaultPagerTouchSlop!! * scale).toInt()
        }.onFailure {
            Log.e("viewPager2", Log.getStackTraceString(it))
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

    /** 選択したブクマの詳細情報画面を開く */
    fun openBookmarkDetail(container: BookmarkDetailOpenable, entry: Entry, bookmark: Bookmark) {
        val fragmentManager = container.fragmentManager

        val backStackName = "detail: ${bookmark.user}"
        val currentTop = fragmentManager.findFragmentById(container.bookmarkDetailFrameLayoutId)
        val currentTopTag = currentTop?.tag
        if (currentTopTag != null && currentTopTag == backStackName) {
            return
        }

        val bookmarkDetailFragment = BookmarkDetailFragment.createInstance(entry, bookmark)
        fragmentManager.beginTransaction()
            .replace(container.bookmarkDetailFrameLayoutId, bookmarkDetailFragment, backStackName)
            .addToBackStack(backStackName)
            .commitAllowingStateLoss()
    }

    /** ブクマをつけていないユーザーの詳細情報画面を開く */
    fun openEmptyBookmarkDetail(container: BookmarkDetailOpenable, entry: Entry, user: String) {
        val dummyBookmark = Bookmark(
            user = user,
            comment = ""
        )
        openBookmarkDetail(container, entry, dummyBookmark)
    }
}
