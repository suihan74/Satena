package com.suihan74.satena.scenes.bookmarks

import android.annotation.SuppressLint
import android.graphics.Rect
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.View
import androidx.annotation.MainThread
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.doOnLayout
import androidx.core.view.updateLayoutParams
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.FragmentManager
import com.suihan74.satena.R
import com.suihan74.satena.SatenaApplication
import com.suihan74.satena.databinding.ActivityBookmarksBinding
import com.suihan74.satena.scenes.bookmarks.information.EntryInformationFragment
import com.suihan74.satena.scenes.bookmarks.repository.BookmarksRepository
import com.suihan74.satena.scenes.bookmarks.viewModel.BookmarksViewModel
import com.suihan74.satena.scenes.bookmarks.viewModel.ContentsViewModel
import com.suihan74.utilities.*
import com.suihan74.utilities.extensions.*

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
    val bookmarksViewModel by lazyProvideViewModel {
        val app = SatenaApplication.instance

        val repository = BookmarksRepository(
            app.accountLoader,
            SafeSharedPreferences.create(this),
            app.ignoredEntriesRepository,
            app.userTagDao
        )

        BookmarksViewModel(repository).also {
            it.loadEntryFromIntent(this, intent)
        }
    }

    /** タブ制御用のViewModel */
    val contentsViewModel by lazyProvideViewModel {
        ContentsViewModel(SafeSharedPreferences.create(this))
    }

    // ------ //

    private lateinit var binding : ActivityBookmarksBinding

    // ------ //

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTheme(contentsViewModel.themeId)

        binding = ActivityBookmarksBinding.inflate(layoutInflater).also {
            it.vm = bookmarksViewModel
            it.lifecycleOwner = this
        }
        setContentView(binding.root)

        bookmarksViewModel.onCreate(this)

        if (savedInstanceState == null) {
            // コンテンツフラグメントを生成
            supportFragmentManager.beginTransaction()
                .replace(R.id.main_layout, BookmarksContentFragment.createInstance())
                .commitAllowingStateLoss()

            // ドロワフラグメントを生成
            supportFragmentManager.beginTransaction()
                .replace(R.id.entry_information_layout, EntryInformationFragment.createInstance())
                .commitAllowingStateLoss()
        }

        @SuppressLint("RtlHardcoded")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            binding.drawerLayout.doOnLayout { v ->
                val drawerGravity = contentsViewModel.drawerGravity
                val layoutDirection = resources.configuration.layoutDirection
                val gestureExclusionRect =
                    when (Gravity.getAbsoluteGravity(drawerGravity,layoutDirection)) {
                        Gravity.LEFT ->
                            Rect(0, v.bottom - dp2px(200), dp2px(32), v.bottom)

                        Gravity.RIGHT ->
                            Rect(v.right - dp2px(32), v.bottom - dp2px(200), v.right, v.bottom)

                        else -> throw IllegalStateException()
                    }

                binding.drawerLayout.systemGestureExclusionRects = listOf(gestureExclusionRect)
            }
        }

        binding.drawerLayout.addDrawerListener(object : DrawerLayout.DrawerListener {
            private var systemGestureExclusionRects : List<Rect>? = null
            override fun onDrawerOpened(drawerView: View) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    systemGestureExclusionRects = binding.drawerLayout.systemGestureExclusionRects
                    binding.drawerLayout.systemGestureExclusionRects = emptyList()
                }
            }
            override fun onDrawerClosed(drawerView: View) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    binding.drawerLayout.systemGestureExclusionRects = systemGestureExclusionRects.orEmpty()
                }
            }
            override fun onDrawerSlide(drawerView: View, slideOffset: Float) {}
            override fun onDrawerStateChanged(newState: Int) {
                // ドロワ開閉でIMEを閉じる
                hideSoftInputMethod(binding.mainLayout)
            }
        })

        // ドロワの位置を設定
        binding.entryInformationLayout.updateLayoutParams<DrawerLayout.LayoutParams> {
            gravity = contentsViewModel.drawerGravity
        }

        // ユーザー名が与えられている場合，そのユーザーのブクマ詳細画面を開く
        if (savedInstanceState == null) {
            intent.getStringExtra(EXTRA_TARGET_USER).onNotNull { user ->
                bookmarksViewModel.bookmarksEntry.observe(this, scopedObserver { bEntry ->
                    bookmarksViewModel.bookmarksEntry.removeObserver(this)
                    val bookmark = bEntry?.bookmarks?.firstOrNull { it.user == user }
                    if (bookmark != null) {
                        contentsViewModel.openBookmarkDetail(this@BookmarksActivity, bookmark)
                    }
                })
            }
        }
    }

    /** 戻るボタンの制御 */
    override fun onBackPressed() {
        when {
            drawerOpened -> {
                closeDrawer()
            }

            else -> super.onBackPressed()
        }
    }

    // ------ //
    // implement DrawerOwner

    @MainThread
    override fun openDrawer() {
        binding.drawerLayout.openDrawer(binding.entryInformationLayout)
    }

    @MainThread
    override fun closeDrawer() {
        binding.drawerLayout.closeDrawer(binding.entryInformationLayout)
    }

    private val drawerOpened : Boolean
        get() = binding.drawerLayout.isDrawerOpen(binding.entryInformationLayout)

    // ------ //
    // implement BookmarkDetailOpenable

    override val fragmentManager: FragmentManager
        get() = supportFragmentManager

    override val bookmarkDetailFrameLayoutId: Int
        get() = R.id.main_layout
}
