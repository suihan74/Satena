package com.suihan74.satena.scenes.bookmarks2

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.activity.addCallback
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.suihan74.HatenaLib.*
import com.suihan74.satena.R
import com.suihan74.satena.SatenaApplication
import com.suihan74.satena.models.PreferenceKey
import com.suihan74.satena.models.userTag.Tag
import com.suihan74.satena.scenes.bookmarks2.detail.BookmarkDetailFragment
import com.suihan74.satena.scenes.bookmarks2.dialog.BookmarkMenuDialog
import com.suihan74.satena.scenes.bookmarks2.dialog.ReportDialog
import com.suihan74.satena.scenes.bookmarks2.dialog.UserTagSelectionDialog
import com.suihan74.satena.scenes.bookmarks2.information.EntryInformationFragment
import com.suihan74.satena.scenes.entries.EntriesActivity
import com.suihan74.satena.scenes.preferences.ignored.IgnoredEntryRepository
import com.suihan74.satena.scenes.preferences.userTag.UserTagRepository
import com.suihan74.utilities.AccountLoader
import com.suihan74.utilities.MastodonClientHolder
import com.suihan74.utilities.SafeSharedPreferences
import com.suihan74.utilities.showToast
import kotlinx.android.synthetic.main.activity_bookmarks2.*

class BookmarksActivity :
    AppCompatActivity(),
    BookmarkMenuDialog.Listener,
    UserTagSelectionDialog.Listener,
    ReportDialog.Listener
{
    /** ViewModel */
    private lateinit var viewModel: BookmarksViewModel

    val bookmarksFragment
        get() = supportFragmentManager.findFragmentByTag("bookmarks") as BookmarksFragment

    lateinit var onBackPressedCallback: OnBackPressedCallback

    companion object {
        // Intent EXTRA keys
        /** Entryを渡す */
        const val EXTRA_ENTRY = "BookmarksActivity.EXTRA_ENTRY"
        const val EXTRA_TARGET_USER = "BookmarksActivity.EXTRA_TARGET_USER"
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

        val entry = intent.getSerializableExtra(EXTRA_ENTRY) as Entry
        val targetUser = intent.getStringExtra(EXTRA_TARGET_USER)

        val factory = BookmarksViewModel.Factory(
            BookmarksRepository(
                entry = entry,
                client = HatenaClient,
                accountLoader = AccountLoader(applicationContext, HatenaClient, MastodonClientHolder)
            ),
            UserTagRepository(
                SatenaApplication.instance.userTagDao
            ),
            IgnoredEntryRepository(
                SatenaApplication.instance.ignoredEntryDao
            )
        )
        viewModel = ViewModelProviders.of(this, factory)[BookmarksViewModel::class.java]

        viewModel.init(loading = savedInstanceState == null) { e ->
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
        val drawerToggle = object : ActionBarDrawerToggle(this, drawer_layout, toolbar,
            R.string.drawer_open,
            R.string.drawer_close
        ) {
            override fun onDrawerOpened(drawerView: View) {
                super.onDrawerOpened(drawerView)
                val informationFragment = supportFragmentManager.findFragmentByTag("information") as? EntryInformationFragment
                informationFragment?.onShown()
            }
        }
        drawer_layout.addDrawerListener(drawerToggle)
        drawerToggle.isDrawerIndicatorEnabled = false
        drawerToggle.syncState()

        // Observers
        viewModel.bookmarksEntry.observe(this, Observer {
            toolbar.subtitle = String.format("%d users (%d comments)",
                it.bookmarks.size,
                it.bookmarks.count { b -> b.comment.isNotBlank() })
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
        if (savedInstanceState == null) {
            val bookmarksFragment = BookmarksFragment.createInstance()
            val entryInformationFragment = EntryInformationFragment.createInstance()
            val buttonsFragment = FloatingActionButtonsFragment.createInstance()

            supportFragmentManager.beginTransaction()
                .replace(R.id.content_layout, bookmarksFragment, "bookmarks")
                .replace(R.id.buttons_layout, buttonsFragment, "buttons")
                .replace(R.id.entry_information_layout, entryInformationFragment, "information")
                .commitAllowingStateLoss()

            // ユーザーが指定されている場合そのユーザーのブクマ詳細画面に直接遷移する
            if (!targetUser.isNullOrBlank()) {
                showBookmarkDetail(targetUser)
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
        startActivity(
            Intent(this, EntriesActivity::class.java).apply {
                putExtra(EntriesActivity.EXTRA_DISPLAY_USER, user)
            }
        )
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
        dialog.show(supportFragmentManager, "report_dialog")
    }

    override fun onSetUserTag(user: String) {
        val dialog = UserTagSelectionDialog.createInstance(user)
        dialog.show(supportFragmentManager, "user_tag_selection_dialog")
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
}
