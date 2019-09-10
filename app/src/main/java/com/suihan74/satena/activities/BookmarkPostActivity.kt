package com.suihan74.satena.activities

import android.os.Bundle
import android.util.Log
import com.suihan74.HatenaLib.BookmarksEntry
import com.suihan74.HatenaLib.Entry
import com.suihan74.HatenaLib.HatenaClient
import com.suihan74.satena.R
import com.suihan74.satena.fragments.EntryInformationFragment
import com.suihan74.satena.models.PreferenceKey
import com.suihan74.utilities.SafeSharedPreferences
import com.suihan74.utilities.showToast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.lang.ref.WeakReference

class BookmarkPostActivity : ActivityBase() {
    companion object {
        const val EXTRA_ENTRY = "entry"

        private var bookmarksEntryCache = WeakReference<BookmarksEntry>(null)
    }

    override val containerId: Int = R.id.content_layout
    override val progressBarId = R.id.detail_progress_bar
    override val progressBackgroundId = R.id.click_guard

    private lateinit var mEntry: Entry

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 設定ロード
        val prefs = SafeSharedPreferences.create<PreferenceKey>(this)

        // テーマの設定
        val isThemeDark = prefs.getBoolean(PreferenceKey.DARK_THEME)
        if (isThemeDark) {
            setTheme(R.style.AppTheme_Dark)
        }
        else {
            setTheme(R.style.AppTheme_Light)
        }
        setContentView(R.layout.activity_bookmark_post)

        showProgressBar()

        mEntry = intent.getSerializableExtra(EXTRA_ENTRY) as Entry
        launch(Dispatchers.Main) {
            try {
                val cache = bookmarksEntryCache.get()
                val bookmarksEntry = if (cache?.id != mEntry.id) {
                    HatenaClient.getBookmarksEntryAsync(mEntry.id).await()
                }
                else {
                    cache
                }

                val fragment = EntryInformationFragment.createInstance(mEntry, bookmarksEntry, true)
                showFragment(fragment)
            }
            catch (e: Exception) {
                Log.e("BookmarkPostActivity", e.message)
                showToast("エントリ情報の取得に失敗しました")
            }
            finally {
                hideProgressBar()
            }
        }
    }
}
