package com.suihan74.satena.scenes.bookmarks

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.annotation.MainThread
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.updateLayoutParams
import androidx.databinding.DataBindingUtil
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.lifecycleScope
import com.suihan74.hatenaLib.HatenaClient
import com.suihan74.satena.R
import com.suihan74.satena.SatenaApplication
import com.suihan74.satena.databinding.ActivityBookmarksBinding
import com.suihan74.satena.scenes.bookmarks.information.EntryInformationFragment
import com.suihan74.satena.scenes.bookmarks.repository.BookmarksRepository
import com.suihan74.satena.scenes.bookmarks.viewModel.BookmarksViewModel
import com.suihan74.satena.scenes.bookmarks.viewModel.ContentsViewModel
import com.suihan74.utilities.*
import com.suihan74.utilities.extensions.hideSoftInputMethod
import com.suihan74.utilities.extensions.showToast
import kotlinx.android.synthetic.main.activity_bookmarks.*
import kotlinx.coroutines.launch

/**
 * ブクマ一覧画面
 */
class BookmarksActivity :
    AppCompatActivity(),
    BookmarkDetailOpenable,
    DrawerOwner
{
    companion object {
        // Intent EXTRA keys

        // ------ //
        // どれかひとつが必要

        /** Entryを直接渡す場合 */
        const val EXTRA_ENTRY = BookmarksRepository.EXTRA_ENTRY

        /** EntryのURLを渡す場合 */
        const val EXTRA_ENTRY_URL = BookmarksRepository.EXTRA_ENTRY_URL

        /** EntryのIDを渡す場合 */
        const val EXTRA_ENTRY_ID = BookmarksRepository.EXTRA_ENTRY_ID

        // ------ //

        /** 画面表示後直接特定のユーザーのブクマを表示する場合その対象 */
        const val EXTRA_TARGET_USER = "BookmarksActivity.EXTRA_TARGET_USER"
    }

    // ------ //

    /** ブクマ操作用のViewModel */
    val bookmarksViewModel : BookmarksViewModel by lazy {
        provideViewModel(this) {
            val app = SatenaApplication.instance

            val repository = BookmarksRepository(
                AccountLoader(this, HatenaClient, MastodonClientHolder),
                SafeSharedPreferences.create(this),
                app.ignoredEntryDao,
                app.userTagDao
            ).also { repo ->
                lifecycleScope.launch {
                    val result = runCatching {
                        repo.loadEntryFromIntent(intent)
                    }

                    if (result.exceptionOrNull() is IllegalArgumentException) {
                        showToast(R.string.invalid_url_error)
                        finish()
                    }
                }
            }

            BookmarksViewModel(repository)
        }
    }

    /** タブ制御用のViewModel */
    val contentsViewModel by lazy {
        provideViewModel(this) {
            ContentsViewModel(SafeSharedPreferences.create(this))
        }
    }

    // ------ //

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val binding = DataBindingUtil.setContentView<ActivityBookmarksBinding>(
            this.also { it.setTheme(contentsViewModel.themeId) },
            R.layout.activity_bookmarks
        ).also {
            it.bookmarksViewModel = bookmarksViewModel
            it.contentsViewModel = contentsViewModel
            it.lifecycleOwner = this
        }

        // タブ制御の初期化
        contentsViewModel.initializeTabPager(
            this,
            binding.tabPager,
            binding.tabLayout
        )

        // スクロールにあわせてビューを隠す設定を反映させる
        contentsViewModel.setScrollingBehavior(
            this,
            binding.toolbar,
            binding.buttonsLayout
        )

        // 下部ボタンエリアを生成
        supportFragmentManager.beginTransaction()
            .replace(R.id.buttons_layout, FloatingActionButtonsFragment.createInstance())
            .commitAllowingStateLoss()

        // ドロワエリアを生成
        supportFragmentManager.beginTransaction()
            .replace(R.id.entry_information_layout, EntryInformationFragment.createInstance())
            .commitAllowingStateLoss()

        drawer_layout.addDrawerListener(object : DrawerLayout.DrawerListener {
            override fun onDrawerOpened(drawerView: View) {}
            override fun onDrawerClosed(drawerView: View) {}
            override fun onDrawerSlide(drawerView: View, slideOffset: Float) {}
            override fun onDrawerStateChanged(newState: Int) {
                // ドロワ開閉でIMEを閉じる
                hideSoftInputMethod(main_area)
            }
        })

        // ドロワの位置を設定
        entry_information_layout.updateLayoutParams<DrawerLayout.LayoutParams> {
            gravity = contentsViewModel.drawerGravity
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        bookmarksViewModel.onActivityResult(requestCode, resultCode, data)
    }

    /** 戻るボタンの制御 */
    override fun onBackPressed() {
        if (onBackPressedDispatcher.hasEnabledCallbacks()) {
            onBackPressedDispatcher.onBackPressed()
        }
        else if (drawer_layout.isDrawerOpen(entry_information_layout)) {
            drawer_layout.closeDrawer(entry_information_layout)
        }
        else {
            super.onBackPressed()
        }
    }

    // ------ //
    // implement DrawerOwner

    @MainThread
    override fun openDrawer() {
        drawer_layout.openDrawer(entry_information_layout)
    }

    @MainThread
    override fun closeDrawer() {
        drawer_layout.closeDrawer(entry_information_layout)
    }

    // ------ //
    // implement BookmarkDetailOpenable

    override val fragmentManager: FragmentManager
        get() = supportFragmentManager

    override val bookmarkDetailFrameLayoutId: Int
        get() = R.id.detail_content_layout
}
