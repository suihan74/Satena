package com.suihan74.satena.scenes.bookmarks2

import android.app.Activity
import android.content.Intent
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.activity.addCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.view.updateLayoutParams
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.Observer
import androidx.lifecycle.observe
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.behavior.HideBottomViewOnScrollBehavior
import com.suihan74.hatenaLib.*
import com.suihan74.satena.NetworkReceiver
import com.suihan74.satena.R
import com.suihan74.satena.SatenaApplication
import com.suihan74.satena.models.saveHistory
import com.suihan74.satena.scenes.bookmarks2.information.EntryInformationFragment
import com.suihan74.satena.scenes.post2.BookmarkPostActivity
import com.suihan74.satena.scenes.preferences.ignored.IgnoredEntriesRepository
import com.suihan74.satena.scenes.preferences.userTag.UserTagRepository
import com.suihan74.utilities.*
import com.suihan74.utilities.exceptions.InvalidUrlException
import com.suihan74.utilities.extensions.findFragmentByTag
import com.suihan74.utilities.extensions.getObjectExtra
import com.suihan74.utilities.extensions.hideSoftInputMethod
import com.suihan74.utilities.extensions.showToast
import kotlinx.android.synthetic.main.activity_bookmarks2.*

class BookmarksActivity : AppCompatActivity() {
    companion object {
        // Intent EXTRA keys
        /** Entryを直接渡す場合 */
        const val EXTRA_ENTRY = "BookmarksActivity.EXTRA_ENTRY"
        /** EntryのURLを渡す場合 */
        const val EXTRA_ENTRY_URL = "BookmarksActivity.EXTRA_ENTRY_URL"
        /** EntryのIDを渡す場合 */
        const val EXTRA_ENTRY_ID = "BookmarksActivity.EXTRA_ENTRY_ID"
        /** 画面表示後直接特定のユーザーのブクマを表示する場合その対象 */
        const val EXTRA_TARGET_USER = "BookmarksActivity.EXTRA_TARGET_USER"

        // Fragment tags
        private const val FRAGMENT_BOOKMARKS = "FRAGMENT_BOOKMARKS"
        private const val FRAGMENT_BUTTONS = "FRAGMENT_BUTTONS"
        private const val FRAGMENT_INFORMATION = "FRAGMENT_INFORMATION"

        // Dialog tags
        private const val DIALOG_BOOKMARK_MENU = "DIALOG_BOOKMARK_MENU"

        // Fragment ViewModel tags
        const val VIEW_MODEL_ACTIVITY = "VIEW_MODEL_ACTIVITY"
        const val VIEW_MODEL_CONTENT_FRAGMENT = "VIEW_MODEL_CONTENT_FRAGMENT"
        const val VIEW_MODEL_POPULAR_TAB = "VIEW_MODEL_POPULAR_TAB"
        const val VIEW_MODEL_RECENT_TAB = "VIEW_MODEL_RECENT_TAB"
        const val VIEW_MODEL_ALL_TAB = "VIEW_MODEL_ALL_TAB"
        const val VIEW_MODEL_CUSTOM_TAB = "VIEW_MODEL_CUSTOM_TAB"

        fun getTabViewModelKey(tabType: BookmarksTabType) : String = when (tabType) {
            BookmarksTabType.POPULAR -> VIEW_MODEL_POPULAR_TAB
            BookmarksTabType.RECENT -> VIEW_MODEL_RECENT_TAB
            BookmarksTabType.ALL -> VIEW_MODEL_ALL_TAB
            BookmarksTabType.CUSTOM -> VIEW_MODEL_CUSTOM_TAB
        }
    }

    /** ViewModel */
    val viewModel: BookmarksViewModel by lazy {
        provideViewModel(this, VIEW_MODEL_ACTIVITY) {
            val bookmarksRepository = BookmarksRepository(
                client = HatenaClient,
                accountLoader = AccountLoader(
                    applicationContext,
                    HatenaClient,
                    MastodonClientHolder
                ),
                prefs = SafeSharedPreferences.create(this)
            )

            val userTagRepository = UserTagRepository(
                SatenaApplication.instance.userTagDao
            )

            val ignoredEntriesRepository = IgnoredEntriesRepository(
                    SatenaApplication.instance.ignoredEntryDao
                )

            BookmarksViewModel(bookmarksRepository, userTagRepository, ignoredEntriesRepository)
        }
    }

    val bookmarksFragment
        get() = findFragmentByTag<BookmarksFragment>(FRAGMENT_BOOKMARKS)!!

    lateinit var onBackPressedCallback: OnBackPressedCallback

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTheme(viewModel.themeId)
        setContentView(R.layout.activity_bookmarks2)

        val firstLaunching = !viewModel.repository.isInitialized

        progress_bar.visibility = View.VISIBLE

        viewModel.toolbarTitle.observe(this, Observer {
            toolbar.title = it
        })

        val onSuccess: OnSuccess<Entry> = {
            val targetUser = intent.getStringExtra(EXTRA_TARGET_USER)
            init(firstLaunching, it, targetUser)

            val entryInformationFragment = EntryInformationFragment.createInstance()
            supportFragmentManager.beginTransaction()
                .replace(R.id.entry_information_layout, entryInformationFragment, FRAGMENT_INFORMATION)
                .commitAllowingStateLoss()
        }

        val onError: OnError = { e ->
            when(e) {
                is InvalidUrlException -> {
                    showToast(R.string.invalid_url_error)
                    finish()
                }

                is FetchIgnoredUsersFailureException -> {
                    showToast(R.string.msg_fetch_ignored_users_failed)
                }

                is NotFoundException -> {}

                else -> showToast(R.string.msg_update_bookmarks_failed)
            }
            Log.e("BookmarksActivity", Log.getStackTraceString(e))
            progress_bar.visibility = View.INVISIBLE
        }

        viewModel.onCreate(intent, onSuccess, onError)

        // スクロールでツールバーを隠す
        toolbar.updateLayoutParams<AppBarLayout.LayoutParams> {
            scrollFlags =
                if (viewModel.hideToolbarByScrolling)
                    AppBarLayout.LayoutParams.SCROLL_FLAG_SCROLL or AppBarLayout.LayoutParams.SCROLL_FLAG_ENTER_ALWAYS
                else
                    0
        }

        // スクロールでボタンを隠す
        buttons_layout.updateLayoutParams<CoordinatorLayout.LayoutParams> {
            behavior =
                if (viewModel.hideButtonsByScrolling)
                    HideBottomViewOnScrollBehavior<View>(this@BookmarksActivity, null)
                else
                    null
        }

        // 接続状態を監視する
        var isNetworkReceiverInitialized = false
        val networkReceiver = SatenaApplication.instance.networkReceiver
        networkReceiver.state.observe(this) { state ->
            if (!isNetworkReceiverInitialized) {
                isNetworkReceiverInitialized = true
                return@observe
            }

            if (state == NetworkReceiver.State.CONNECTED) {
                viewModel.init(supportFragmentManager, true)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        appbar_layout.setExpanded(true, false)

        // ドロワを配置
        entry_information_layout.updateLayoutParams<DrawerLayout.LayoutParams> {
            gravity = viewModel.drawerGravity
        }
    }

    fun showButtons() {
        val behavior = (buttons_layout.layoutParams as? CoordinatorLayout.LayoutParams)
            ?.behavior as? HideBottomViewOnScrollBehavior
            ?: return
        behavior.slideUp(buttons_layout)
    }

    /** entryロード完了後に画面を初期化 */
    private fun init(firstLaunching: Boolean, entry: Entry, targetUser: String?) {
        // ロード中の画面回転による初期化処理重複を抑制する
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LOCKED

        val onError: OnError = { e ->
            when (e) {
                is AccountLoader.HatenaSignInException ->
                    showToast(R.string.msg_auth_failed)

                is AccountLoader.MastodonSignInException ->
                    showToast(R.string.msg_auth_mastodon_failed)

                is NotFoundException -> {
                    showToast(R.string.msg_no_bookmarks)
                    toolbar.subtitle = getString(R.string.toolbar_subtitle_bookmarks, 0, 0)
                }

                is FetchIgnoredUsersFailureException -> {
                    showToast(R.string.msg_fetch_ignored_users_failed)
                }

                else ->
                    showToast(R.string.msg_update_bookmarks_failed)
            }
            Log.e("BookmarksActivity", Log.getStackTraceString(e))
        }

        val onFinally: OnFinally = {
            // コンテンツの初期化
            if (firstLaunching) {
                // 表示履歴に追加
                entry.saveHistory(this@BookmarksActivity)

                val bookmarksFragment = BookmarksFragment.createInstance()
                val buttonsFragment = FloatingActionButtonsFragment.createInstance()

                supportFragmentManager.beginTransaction()
                    .replace(R.id.content_layout, bookmarksFragment, FRAGMENT_BOOKMARKS)
                    .replace(R.id.buttons_layout, buttonsFragment, FRAGMENT_BUTTONS)
                    .commitAllowingStateLoss()

                // ユーザーが指定されている場合そのユーザーのブクマ詳細画面に直接遷移する
                if (!targetUser.isNullOrBlank()) {
                    viewModel.showBookmarkDetail(this, targetUser)
                }
            }

            progress_bar.visibility = View.INVISIBLE

            // 画面回転を解放する
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }

        viewModel.init(
            fragmentManager = supportFragmentManager,
            loading = firstLaunching,
            onError = onError,
            onFinally = onFinally
        )

        // Toolbar
        toolbar.apply {
            if (!firstLaunching) {
                val bookmarksEntry = viewModel.bookmarksEntry.value
                val entireBookmarksCount = bookmarksEntry?.bookmarks?.size ?: 0
                val commentsCount = bookmarksEntry?.bookmarks?.count { it.comment.isNotBlank() } ?: 0

                toolbar.subtitle = getString(
                    R.string.toolbar_subtitle_bookmarks,
                    entireBookmarksCount,
                    commentsCount
                )
            }
        }

        // Drawerの開閉を監視する

        drawer_layout.addDrawerListener(object : DrawerLayout.DrawerListener {
            override fun onDrawerOpened(drawerView: View) {
                hideSoftInputMethod()
                findFragmentByTag<EntryInformationFragment>(FRAGMENT_INFORMATION)
                    ?.onShown()
            }
            override fun onDrawerClosed(drawerView: View) {}
            override fun onDrawerSlide(drawerView: View, slideOffset: Float) {}
            override fun onDrawerStateChanged(newState: Int) {}
        })
        /*val drawerToggle = object : ActionBarDrawerToggle(
            this,
            drawer_layout,
            toolbar,
            R.string.drawer_open,
            R.string.drawer_close
        ) {
            override fun onDrawerOpened(drawerView: View) {
                super.onDrawerOpened(drawerView)
                hideSoftInputMethod()

                findFragmentByTag<EntryInformationFragment>(FRAGMENT_INFORMATION)
                    ?.onShown()
            }
        }
        drawer_layout.addDrawerListener(drawerToggle)
        */

        // Observers
        viewModel.bookmarksEntry.observe(this) {
            if (it == null) return@observe
            toolbar.subtitle = getString(
                R.string.toolbar_subtitle_bookmarks,
                it.bookmarks.size,
                it.bookmarks.count { b -> b.comment.isNotBlank() }
            )
        }

        // 戻るボタンを監視
        onBackPressedCallback = onBackPressedDispatcher.addCallback(this) {
            if (!closeDrawer()) {
                val backStackEntryCount = supportFragmentManager.backStackEntryCount
                if (backStackEntryCount > 0) {
                    supportFragmentManager.popBackStack()
                }
                else {
                    finish()
                }
            }
        }
    }

    /** BookmarkPostActivityからの結果を受け取る */
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        // ブクマ投稿結果をentryに反映して、次回以降の編集時に投稿内容を最初から入力した状態でダイアログを表示する
        when (requestCode) {
            BookmarkPostActivity.REQUEST_CODE -> {
                when (resultCode) {
                    Activity.RESULT_OK -> {
                        val result = data?.getObjectExtra<BookmarkResult>(BookmarkPostActivity.RESULT_BOOKMARK)
                            ?: return
                        /*viewModel.resetEntry(
                            viewModel.entry.copy(bookmarkedData = result)
                        )*/
                        viewModel.updateUserBookmark(result)
                        viewModel.setEditingComment(null)
                    }

                    Activity.RESULT_CANCELED -> {
                        val comment = data?.getStringExtra(BookmarkPostActivity.RESULT_EDITING_COMMENT)
                        viewModel.setEditingComment(comment)
                    }
                }
            }
        }
    }

    /** エントリ情報ドロワを閉じる */
    fun closeDrawer() : Boolean =
        if (drawer_layout.isDrawerOpen(entry_information_layout)) {
            drawer_layout.closeDrawer(entry_information_layout)
            true
        }
        else false
}
