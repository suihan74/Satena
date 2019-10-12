package com.suihan74.satena.activities

import android.app.Activity
import android.content.Intent
import android.graphics.Point
import android.os.Bundle
import android.util.Log
import android.webkit.URLUtil
import android.widget.FrameLayout
import com.suihan74.HatenaLib.BookmarksEntry
import com.suihan74.HatenaLib.Entry
import com.suihan74.HatenaLib.HatenaClient
import com.suihan74.HatenaLib.SearchType
import com.suihan74.satena.R
import com.suihan74.satena.fragments.BookmarkPostFragment
import com.suihan74.satena.models.PreferenceKey
import com.suihan74.utilities.AccountLoader
import com.suihan74.utilities.SafeSharedPreferences
import com.suihan74.utilities.showToast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BookmarkPostActivity : ActivityBase() {
    companion object {
        private const val DIALOG_WIDTH_RATIO = 0.9

        private const val EXTRA_BASE = "com.suihan74.satena.activities.BookmarkPostActivity."
        const val EXTRA_ENTRY = EXTRA_BASE + "entry"

        private const val BUNDLE_ENTRY = "mEntry"
    }
    override val containerId = R.id.content_layout
    override val progressBarId = R.id.detail_progress_bar
    override val progressBackgroundId: Int? = null

    private var mEntry : Entry? = null
    val entry
        get() = mEntry!!

    private var mBookmarksEntry : BookmarksEntry? = null

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.run {
            putSerializable(BUNDLE_ENTRY, mEntry)
        }
    }

    private fun getDisplaySize(activity: Activity) : Point {
        val display = activity.windowManager.defaultDisplay
        val point = Point()
        display.getSize(point)
        return point
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 設定ロード
        val prefs = SafeSharedPreferences.create<PreferenceKey>(this)

        // テーマの設定
        val isThemeDark = prefs.getBoolean(PreferenceKey.DARK_THEME)
        if (isThemeDark) {
            setTheme(R.style.AppDialogTheme_Dark)
        }
        else {
            setTheme(R.style.AppDialogTheme_Light)
        }
        setContentView(R.layout.activity_bookmark_post)

        val displaySize = getDisplaySize(this)
        val content = findViewById<FrameLayout>(R.id.content_layout)
        content.layoutParams = content.layoutParams.apply {
            width = (displaySize.x * DIALOG_WIDTH_RATIO).toInt()
        }

        if (savedInstanceState == null) {
            showProgressBar()

            launch(Dispatchers.Main) {
                // 投稿用アカウントの確認
                try {
                    if (!HatenaClient.signedIn()) {
                        throw RuntimeException()
                    }
                    AccountLoader.signInHatenaAsync(this@BookmarkPostActivity).await()
                    AccountLoader.signInMastodonAsync(this@BookmarkPostActivity).await()
                }
                catch (e: AccountLoader.HatenaSignInException) {
                    showToast("サインインに失敗しました")
                    finish()
                    return@launch
                }
                catch (e: AccountLoader.MastodonSignInException) {
                    showToast("Mastodonへのサインインに失敗しました")
                }

                // エントリ情報を取得
                loadExtras(intent)

                try {
                    val fragment = BookmarkPostFragment.createInstance(entry, mBookmarksEntry).apply {
                        setOnPostedListener {
                            onBackPressed() // 投稿完了でアクティビティを閉じる
                        }
                    }
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
                    mBookmarksEntry =
                        try {
                            HatenaClient.getEmptyBookmarksEntryAsync(url).await()
                        }
                        catch (e: Exception) {
                            Log.d("failedToFetchBookmarks", Log.getStackTraceString(e))
                            null
                        }
                    entry = Entry(0, mBookmarksEntry?.title ?: "", "", 0, url, url, "", "")
                }

                mEntry = entry
            }

            else -> {
                mEntry = intent.getSerializableExtra(EXTRA_ENTRY) as? Entry
            }
        }
    }
}
