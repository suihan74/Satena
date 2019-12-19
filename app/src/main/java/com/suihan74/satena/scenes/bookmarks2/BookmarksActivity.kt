package com.suihan74.satena.scenes.bookmarks2

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.suihan74.HatenaLib.Bookmark
import com.suihan74.HatenaLib.Entry
import com.suihan74.HatenaLib.HatenaClient
import com.suihan74.satena.R
import com.suihan74.satena.models.PreferenceKey
import com.suihan74.satena.scenes.bookmarks2.detail.BookmarkDetailFragment
import com.suihan74.satena.scenes.bookmarks2.information.EntryInformationFragment
import com.suihan74.utilities.SafeSharedPreferences
import kotlinx.android.synthetic.main.activity_bookmarks2.*

class BookmarksActivity : AppCompatActivity() {
    /** ViewModel */
    private lateinit var viewModel: BookmarksViewModel

    val bookmarksFragment
        get() = supportFragmentManager.findFragmentByTag("bookmarks") as BookmarksFragment

    companion object {
        // Intent EXTRA keys
        /** Entryを渡す */
        const val EXTRA_ENTRY = "EXTRA_ENTRY"
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
        if (savedInstanceState == null) {
            val factory = BookmarksViewModel.Factory(
                BookmarksRepository(
                    entry = entry,
                    client = HatenaClient
                )
            )
            viewModel = ViewModelProviders.of(this, factory)[BookmarksViewModel::class.java]
            viewModel.load()
        }
        else {
            viewModel = ViewModelProviders.of(this)[BookmarksViewModel::class.java]
        }

        // Toolbar
        toolbar.title = entry.title

        setSupportActionBar(toolbar)

        // Observers
        viewModel.bookmarksEntry.observe(this, Observer {
            toolbar.subtitle = String.format("%d users (%d comments)",
                it.bookmarks.size,
                it.bookmarks.count { b -> b.comment.isNotBlank() })
        })

        // コンテンツの初期化
        if (savedInstanceState == null) {
            val bookmarksFragment = BookmarksFragment.createInstance()
            val entryInformationFragment = EntryInformationFragment.createInstance()
            val buttonsFragment = FloatingActionButtonsFragment.createInstance()

            supportFragmentManager.beginTransaction()
                .replace(R.id.content_layout, bookmarksFragment, "bookmarks")
                .replace(R.id.entry_information_layout, entryInformationFragment, "information")
                .replace(R.id.buttons_layout, buttonsFragment, "buttons")
                .commitAllowingStateLoss()
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
        val bookmark = viewModel.bookmarksEntry.value?.bookmarks?.firstOrNull { it.user == user }
        if (bookmark != null) {
            showBookmarkDetail(bookmark)
        }
    }

    override fun onBackPressed() {
        val backStackEntryCount = supportFragmentManager.backStackEntryCount
        if (backStackEntryCount > 0) {
            supportFragmentManager.popBackStack()
        }
        else {
            super.onBackPressed()
        }
    }
}
