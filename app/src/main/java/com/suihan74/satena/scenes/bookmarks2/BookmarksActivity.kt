package com.suihan74.satena.scenes.bookmarks2

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.activity.addCallback
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.view.GravityCompat
import androidx.core.view.updateLayoutParams
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.appbar.AppBarLayout
import com.suihan74.hatenaLib.*
import com.suihan74.satena.NetworkReceiver
import com.suihan74.satena.R
import com.suihan74.satena.SatenaApplication
import com.suihan74.satena.dialogs.UserTagDialogFragment
import com.suihan74.satena.models.PreferenceKey
import com.suihan74.satena.models.TapEntryAction
import com.suihan74.satena.models.saveHistory
import com.suihan74.satena.models.userTag.Tag
import com.suihan74.satena.scenes.bookmarks2.detail.BookmarkDetailFragment
import com.suihan74.satena.scenes.bookmarks2.dialog.BookmarkMenuDialog
import com.suihan74.satena.scenes.bookmarks2.dialog.ReportDialog
import com.suihan74.satena.scenes.bookmarks2.dialog.UserTagSelectionDialog
import com.suihan74.satena.scenes.bookmarks2.information.EntryInformationFragment
import com.suihan74.satena.scenes.entries2.EntriesActivity
import com.suihan74.satena.scenes.entries2.dialog.EntryMenuDialog
import com.suihan74.satena.scenes.post2.BookmarkPostActivity
import com.suihan74.satena.scenes.preferences.ignored.IgnoredEntryRepository
import com.suihan74.satena.scenes.preferences.userTag.UserTagRepository
import com.suihan74.utilities.*
import com.suihan74.utilities.exceptions.InvalidUrlException
import kotlinx.android.synthetic.main.activity_bookmarks2.*
import kotlinx.coroutines.CompletionHandler

class BookmarksActivity :
    AppCompatActivity(),
    BookmarkMenuDialog.Listener,
    UserTagSelectionDialog.Listener,
    ReportDialog.Listener,
    UserTagDialogFragment.Listener
{
    /** ViewModel */
    val viewModel: BookmarksViewModel by lazy {
        val repository = BookmarksRepository(
            client = HatenaClient,
            accountLoader = AccountLoader(
                applicationContext,
                HatenaClient,
                MastodonClientHolder
            )
        )

        val factory = BookmarksViewModel.Factory(
            repository,
            UserTagRepository(
                SatenaApplication.instance.userTagDao
            ),
            IgnoredEntryRepository(
                SatenaApplication.instance.ignoredEntryDao
            )
        )

        ViewModelProvider(this, factory)[BookmarksViewModel::class.java]
    }

    val bookmarksFragment
        get() = findFragmentByTag<BookmarksFragment>(FRAGMENT_BOOKMARKS)!!

    lateinit var onBackPressedCallback: OnBackPressedCallback

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
        private const val DIALOG_REPORT = "DIALOG_REPORT"
        private const val DIALOG_SELECT_USER_TAG = "DIALOG_SELECT_USER_TAG"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val prefs = SafeSharedPreferences.create<PreferenceKey>(this)
        setTheme(
            prefs.getBoolean(PreferenceKey.DARK_THEME).let {
                if (it) R.style.AppTheme_Dark
                else R.style.AppTheme_Light
            }
        )
        setContentView(R.layout.activity_bookmarks2)

        val targetUser = intent.getStringExtra(EXTRA_TARGET_USER)

        val firstLaunching = !viewModel.repository.isInitialized //savedInstanceState == null
        val entry =
            if (firstLaunching) intent.getSerializableExtra(EXTRA_ENTRY) as? Entry
            else viewModel.repository.entry

        if (entry == null) {
            // Entryのロードが必要な場合
            progress_bar.visibility = View.VISIBLE

            val onSuccess : (Entry)->Unit = { init(firstLaunching, it, targetUser) }

            val onError : CompletionHandler = { e ->
                when(e) {
                    is InvalidUrlException -> {
                        showToast(R.string.invalid_url_error)
                        finish()
                    }

                    is FetchIgnoredUsersFailureException -> {
                        showToast(R.string.msg_fetch_ignored_users_failed)
                    }

                    else -> showToast(R.string.msg_update_bookmarks_failed)
                }
                Log.e("BookmarksActivity", Log.getStackTraceString(e))
                progress_bar.visibility = View.INVISIBLE
            }

            val eid = intent.getLongExtra(EXTRA_ENTRY_ID, 0L)
            if (eid > 0L) {
                toolbar.title = "eid=$eid"
                viewModel.loadEntry(eid, onSuccess, onError)
            }
            else {
                val url = getUrlFromIntent(intent)
                toolbar.title = url
                viewModel.loadEntry(url, onSuccess, onError)
            }
        }
        else {
            // Entryは既に取得済みの場合
            viewModel.repository.setEntry(entry)
            init(firstLaunching, entry, targetUser)
        }

        // スクロールでツールバーを隠す
        toolbar.updateLayoutParams<AppBarLayout.LayoutParams> {
            scrollFlags =
                if (prefs.getBoolean(PreferenceKey.BOOKMARKS_HIDING_TOOLBAR_BY_SCROLLING))
                    AppBarLayout.LayoutParams.SCROLL_FLAG_SCROLL or AppBarLayout.LayoutParams.SCROLL_FLAG_ENTER_ALWAYS
                else
                    0
        }

        // スクロールでボタンを隠す
        buttons_layout.updateLayoutParams<CoordinatorLayout.LayoutParams> {
            behavior =
                if (prefs.getBoolean(PreferenceKey.BOOKMARKS_HIDING_BUTTONS_BY_SCROLLING))
                    HideBottomViewOnScrollBehavior<View>(this@BookmarksActivity, null)
                else
                    null
        }

        // 接続状態を監視する
        var isNetworkReceiverInitialized = false
        val networkReceiver = SatenaApplication.instance.networkReceiver
        networkReceiver.state.observe(this, Observer { state ->
            if (!isNetworkReceiverInitialized) {
                isNetworkReceiverInitialized = true
                return@Observer
            }

            if (state == NetworkReceiver.State.CONNECTED) {
                viewModel.init(true)
            }
        })
    }

    fun showButtons() {
        val behavior = (buttons_layout.layoutParams as? CoordinatorLayout.LayoutParams)
            ?.behavior as? HideBottomViewOnScrollBehavior
            ?: return
        behavior.slideUp(buttons_layout)
    }

    /** Intentから適切なエントリーURLを受け取る */
    private fun getUrlFromIntent(intent: Intent) : String {
        return intent.getStringExtra(EXTRA_ENTRY_URL)
            ?: when (intent.action) {
                // 閲覧中のURLが送られてくる場合
                Intent.ACTION_SEND ->
                    intent.getStringExtra(Intent.EXTRA_TEXT)!!

                // ブコメページのURLが送られてくる場合
                Intent.ACTION_VIEW ->
                    HatenaClient.getEntryUrlFromCommentPageUrl(intent.dataString!!)

                else -> throw InvalidUrlException()
            }
    }

    /** entryロード完了後に画面を初期化 */
    private fun init(firstLaunching: Boolean, entry: Entry, targetUser: String?) {
        viewModel.init(loading = firstLaunching) { e ->
            when (e) {
                is AccountLoader.HatenaSignInException ->
                    showToast(R.string.msg_auth_failed)

                is AccountLoader.MastodonSignInException ->
                    showToast(R.string.msg_auth_mastodon_failed)

                else ->
                    showToast(R.string.msg_update_bookmarks_failed)
            }
        }

        // Toolbar
        toolbar.apply {
            title = entry.title
        }

        // Drawerの開閉を監視する
        val drawerToggle = object : ActionBarDrawerToggle(
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
        drawerToggle.isDrawerIndicatorEnabled = false
        drawerToggle.syncState()

        // Observers
        viewModel.bookmarksEntry.observe(this, Observer {
            toolbar.subtitle = getString(
                R.string.toolbar_subtitle_bookmarks,
                it.bookmarks.size,
                it.bookmarks.count { b -> b.comment.isNotBlank() }
            )
        })

        // 戻るボタンを監視
        onBackPressedCallback = onBackPressedDispatcher.addCallback(this) {
            if (drawer_layout.isDrawerOpen(GravityCompat.END)) {
                drawer_layout.closeDrawer(GravityCompat.END)
            }
            else {
                val backStackEntryCount = supportFragmentManager.backStackEntryCount
                if (backStackEntryCount > 0) {
                    supportFragmentManager.popBackStack()
                }
                else {
                    finish()
                }
            }
        }

        // コンテンツの初期化
        if (firstLaunching) {
            // 表示履歴に追加
            entry.saveHistory(this@BookmarksActivity)

            val bookmarksFragment = BookmarksFragment.createInstance()
            val entryInformationFragment = EntryInformationFragment.createInstance()
            val buttonsFragment = FloatingActionButtonsFragment.createInstance()

            supportFragmentManager.beginTransaction()
                .replace(R.id.content_layout, bookmarksFragment, FRAGMENT_BOOKMARKS)
                .replace(R.id.buttons_layout, buttonsFragment, FRAGMENT_BUTTONS)
                .replace(R.id.entry_information_layout, entryInformationFragment, FRAGMENT_INFORMATION)
                .commitAllowingStateLoss()

            // ユーザーが指定されている場合そのユーザーのブクマ詳細画面に直接遷移する
            if (!targetUser.isNullOrBlank()) {
                showBookmarkDetail(targetUser)
            }
        }

        progress_bar.visibility = View.INVISIBLE
    }

    /** BookmarkPostActivityからの結果を受け取る */
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        // ブクマ投稿結果をentryに反映して、次回以降の編集時に投稿内容を最初から入力した状態でダイアログを表示する
        when (requestCode) {
            BookmarkPostActivity.REQUEST_CODE -> {
                when (resultCode) {
                    Activity.RESULT_OK -> {
                        val result = data?.getSerializableExtra(BookmarkPostActivity.RESULT_BOOKMARK) as? BookmarkResult
                            ?: return
                        viewModel.resetEntry(
                            viewModel.entry.copy(bookmarkedData = result)
                        )
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

    /** ブクマ詳細画面を開く */
    fun showBookmarkDetail(bookmark: Bookmark) {
        val bookmarkDetailFragment = BookmarkDetailFragment.createInstance(bookmark)
        supportFragmentManager.beginTransaction()
            .add(R.id.detail_content_layout, bookmarkDetailFragment)
            .addToBackStack("detail: ${bookmark.user}")
            .commitAllowingStateLoss()
    }

    /** ブクマ詳細画面を開く */
    fun showBookmarkDetail(user: String) {
        var observer: Observer<BookmarksEntry>? = null
        observer = Observer { bEntry: BookmarksEntry ->
            val bookmark = bEntry.bookmarks.firstOrNull { it.user == user } ?: return@Observer
            val bookmarkDetailFragment = BookmarkDetailFragment.createInstance(bookmark)
            supportFragmentManager.beginTransaction()
                .add(R.id.detail_content_layout, bookmarkDetailFragment)
                .addToBackStack("detail: $user}")
                .commitAllowingStateLoss()

            viewModel.bookmarksEntry.removeObserver(observer!!)
        }
        viewModel.bookmarksEntry.observe(this, observer)
    }

    // --- BookmarkMenuDialogの処理 --- //

    override fun isIgnored(user: String) =
        viewModel.ignoredUsers.value.contains(user)

    override fun onShowEntries(user: String) {
        val intent = Intent(this, EntriesActivity::class.java).apply {
            putExtra(EntriesActivity.EXTRA_USER, user)
        }
        startActivity(intent)
    }

    override fun onIgnoreUser(user: String, ignore: Boolean) {
        viewModel.setUserIgnoreState(
            user,
            ignore,
            onSuccess = {
                val msgId =
                    if (ignore) R.string.msg_ignore_user_succeeded
                    else R.string.msg_unignore_user_succeeded
                showToast(msgId, user)
            },
            onError = { e ->
                val msgId =
                    if (ignore) R.string.msg_ignore_user_failed
                    else R.string.msg_unignore_user_failed
                showToast(msgId, user)
                Log.e("ignoreUser", "failed: user = $user")
                e.printStackTrace()
            }
        )
    }

    override fun onReportBookmark(bookmark: Bookmark) {
        val dialog = ReportDialog.createInstance(viewModel.entry, bookmark)
        dialog.show(supportFragmentManager, DIALOG_REPORT)
    }

    override fun onSetUserTag(user: String) {
        val dialog = UserTagSelectionDialog.createInstance(user)
        dialog.show(supportFragmentManager, DIALOG_SELECT_USER_TAG)
    }

    // --- UserTagDialogの処理 --- //

    override suspend fun onCompletedEditTagName(
        tagName: String,
        dialog: UserTagDialogFragment
    ): Boolean {
        return try {
            viewModel.createTag(tagName)
            viewModel.loadUserTags()
            true
        }
        catch (e: Throwable) {
            Log.e("BookmarksActivity", Log.getStackTraceString(e))
            false
        }
    }

    override suspend fun onAddUserToCreatedTag(
        tagName: String,
        user: String,
        dialog: UserTagDialogFragment
    ) {
        val tag = viewModel.userTags.value?.firstOrNull { it.userTag.name == tagName } ?: throw RuntimeException("")
        viewModel.tagUser(user, tag.userTag)
        viewModel.loadUserTags()

        showToast(R.string.msg_user_tag_created_and_added_user, tagName, user)
    }

    // --- UserTagSelectionDialogの処理 --- //

    override fun getUserTags() =
        viewModel.userTags.value ?: emptyList()

    override suspend fun activateTags(user: String, activeTags: List<Tag>) {
        activeTags.forEach { tag ->
            viewModel.tagUser(user, tag)
        }
    }

    override suspend fun inactivateTags(user: String, inactiveTags: List<Tag>) {
        inactiveTags.forEach { tag ->
            viewModel.unTagUser(user, tag)
        }
    }

    override suspend fun reloadUserTags() {
        viewModel.loadUserTags()
    }

    // --- ReportDialogの処理 --- //

    override fun onReportBookmark(model: ReportDialog.Model) {
        val user = model.report.bookmark.user
        viewModel.reportBookmark(
            model.report,
            onSuccess = {
                if (model.ignoreAfterReporting && !viewModel.ignoredUsers.value.contains(user)) {
                    viewModel.setUserIgnoreState(
                        user,
                        true,
                        onSuccess = {
                            showToast(R.string.msg_report_and_ignore_succeeded, user)
                        },
                        onError = { e ->
                            showToast(R.string.msg_ignore_user_failed, user)
                            Log.e("ignoreUser", "failed: user = $user")
                            e.printStackTrace()
                        }
                    )
                }
                else {
                    showToast(R.string.msg_report_succeeded, user)
                }
            },
            onError = { e ->
                showToast(R.string.msg_report_failed)
                Log.e("onReportBookmark", "error user: $user")
                e.printStackTrace()
            }
        )
    }

    // --- ブックマーク中のリンクの処理 --- //

    fun onBookmarkClicked(bookmark: Bookmark) {
        showBookmarkDetail(bookmark)
    }

    fun onBookmarkLongClicked(bookmark: Bookmark): Boolean {
        val dialog = BookmarkMenuDialog.createInstance(bookmark)
        dialog.show(supportFragmentManager, DIALOG_BOOKMARK_MENU)
        return true
    }

    fun onLinkClicked(url: String) {
        val prefs = SafeSharedPreferences.create<PreferenceKey>(this)
        val act = TapEntryAction.fromInt(prefs.getInt(PreferenceKey.BOOKMARK_LINK_SINGLE_TAP_ACTION))
        EntryMenuDialog.act(this, url, act, supportFragmentManager)
    }

    fun onLinkLongClicked(url: String) {
        val prefs = SafeSharedPreferences.create<PreferenceKey>(this)
        val act = TapEntryAction.fromInt(prefs.getInt(PreferenceKey.BOOKMARK_LINK_LONG_TAP_ACTION))
        EntryMenuDialog.act(this, url, act, supportFragmentManager)
    }

    fun onEntryIdClicked(eid: Long) {
        val intent = Intent(this, BookmarksActivity::class.java).apply {
            putExtra(EXTRA_ENTRY_ID, eid)
        }
        startActivity(intent)
    }
}
