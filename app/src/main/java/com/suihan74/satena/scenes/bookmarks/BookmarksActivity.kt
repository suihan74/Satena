package com.suihan74.satena.scenes.bookmarks

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.lifecycleScope
import com.suihan74.hatenaLib.HatenaClient
import com.suihan74.satena.R
import com.suihan74.satena.SatenaApplication
import com.suihan74.satena.databinding.ActivityBookmarksBinding
import com.suihan74.satena.scenes.bookmarks.repository.BookmarksRepository
import com.suihan74.satena.scenes.bookmarks.viewModel.BookmarksViewModel
import com.suihan74.satena.scenes.bookmarks.viewModel.ContentsViewModel
import com.suihan74.utilities.AccountLoader
import com.suihan74.utilities.MastodonClientHolder
import com.suihan74.utilities.SafeSharedPreferences
import com.suihan74.utilities.provideViewModel
import kotlinx.coroutines.launch

/**
 * ブクマ一覧画面
 */
class BookmarksActivity :
    AppCompatActivity(),
    BookmarkDetailOpenable
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
                    repo.loadEntryFromIntent(intent)
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

        // 下部ボタンエリアを生成
        supportFragmentManager.beginTransaction()
            .replace(R.id.buttons_layout, FloatingActionButtonsFragment.createInstance())
            .commitAllowingStateLoss()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        bookmarksViewModel.onActivityResult(requestCode, resultCode, data)
    }

    // ------ //

    override val fragmentManager: FragmentManager
        get() = supportFragmentManager

    override val bookmarkDetailFrameLayoutId: Int
        get() = R.id.detail_content_layout
}
