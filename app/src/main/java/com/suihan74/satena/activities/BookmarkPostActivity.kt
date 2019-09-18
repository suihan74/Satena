package com.suihan74.satena.activities

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.webkit.URLUtil
import com.suihan74.HatenaLib.BookmarksEntry
import com.suihan74.HatenaLib.Entry
import com.suihan74.HatenaLib.HatenaClient
import com.suihan74.HatenaLib.SearchType
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

            launch(Dispatchers.Main) {
                loadExtras(intent)

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

    private suspend fun loadExtras(intent: Intent) {
        when (intent.action) {
            // ブラウザから「共有」を使って遷移してきたときの処理
            Intent.ACTION_SEND -> {
                val url = intent.getStringExtra(Intent.EXTRA_TEXT)
                if (!URLUtil.isNetworkUrl(url)) throw RuntimeException("invalid url shared")

                var entry: Entry? = intent.getSerializableExtra(EXTRA_ENTRY) as? Entry

                if (entry == null) {
                    try {
                        entry = HatenaClient.searchEntriesAsync(url, SearchType.Text).await()
                            .firstOrNull { it.url == url }
                    }
                    catch (e: Exception) {
                        Log.d("failedToFetchBookmarks", Log.getStackTraceString(e))
                    }
                }

                if (entry == null) {
                    val bookmarksEntry: BookmarksEntry? =
                        try {
                            HatenaClient.getEmptyBookmarksEntryAsync(url).await()
                        }
                        catch (e: Exception) {
                            Log.d("failedToFetchBookmarks", Log.getStackTraceString(e))
                            null
                        }
                    entry = Entry(0, bookmarksEntry?.title ?: "", "", 0, url, url, "", "")
                }

                mEntry = entry
            }

            else -> {
                mEntry = intent.getSerializableExtra(EXTRA_ENTRY) as? Entry
            }
        }
    }
}
