package com.suihan74.satena.activities

import android.os.Bundle
import android.util.Log
import com.suihan74.HatenaLib.Entry
import com.suihan74.HatenaLib.HatenaClient
import com.suihan74.satena.R
import com.suihan74.satena.fragments.EntryInformationFragment
import com.suihan74.satena.models.PreferenceKey
import com.suihan74.utilities.SafeSharedPreferences
import com.suihan74.utilities.showToast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BookmarkPostActivity : ActivityBase() {
    companion object {
        private const val EXTRA_BASE = "com.suihan74.satena.activities.BookmarkPostActivity."
        const val EXTRA_ENTRY = EXTRA_BASE + "entry"

        private const val BUNDLE_ENTRY = "mEntry"
    }

    override val containerId: Int = R.id.content_layout
    override val progressBarId = R.id.detail_progress_bar
    override val progressBackgroundId = R.id.click_guard

    private var mEntry: Entry? = null
    val entry
        get() = mEntry!!

    override fun onSaveInstanceState(outState: Bundle?) {
        super.onSaveInstanceState(outState)
        outState?.run {
            putSerializable(BUNDLE_ENTRY, mEntry)
        }
    }

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

        if (savedInstanceState == null) {
            showProgressBar()

            mEntry = intent.getSerializableExtra(EXTRA_ENTRY) as Entry

            launch(Dispatchers.Main) {
                val bookmarksEntry = try {
                    val task = BookmarksActivity.preLoadingTasks?.bookmarksTask ?: HatenaClient.getBookmarksEntryAsync(entry.id)
                    task.await()
                }
                catch (e: Exception) {
                    null
                }

                try {
                    val fragment = EntryInformationFragment.createInstance(entry, bookmarksEntry, true)
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
        else {
            savedInstanceState.let {
                mEntry = it.getSerializable(BUNDLE_ENTRY) as Entry
            }
        }
    }
}
