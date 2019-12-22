package com.suihan74.satena.scenes.post

import android.app.Activity
import android.content.Intent
import android.graphics.Point
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.webkit.URLUtil
import android.widget.FrameLayout
import com.suihan74.HatenaLib.*
import com.suihan74.satena.ActivityBase
import com.suihan74.satena.R
import com.suihan74.satena.SatenaApplication
import com.suihan74.satena.models.PreferenceKey
import com.suihan74.satena.modifySpecificUrls
import com.suihan74.utilities.AccountLoader
import com.suihan74.utilities.MastodonClientHolder
import com.suihan74.utilities.SafeSharedPreferences
import com.suihan74.utilities.showToast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BookmarkPostActivity : ActivityBase(), BookmarkPostFragment.ResultListener {
    companion object {
        private const val DIALOG_WIDTH_RATIO = 0.9

        const val EXTRA_ENTRY = "EXTRA_ENTRY"

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
                    AccountLoader(
                        SatenaApplication.instance,
                        HatenaClient,
                        MastodonClientHolder
                    ).run {
                        signInAccounts()
                    }
                }
                catch (e: AccountLoader.HatenaSignInException) {
                    showToast(R.string.msg_auth_failed)
                    finish()
                    return@launch
                }
                catch (e: AccountLoader.MastodonSignInException) {
                    showToast(R.string.msg_auth_mastodon_failed)
                }

                // エントリ情報を取得
                loadExtras(intent)

                try {
                    val fragment = BookmarkPostFragment.createInstance(entry, mBookmarksEntry)
                    showFragment(fragment)
                }
                catch (e: Exception) {
                    Log.e("BookmarkPostActivity", e.message)
                    showToast(R.string.msg_get_entry_information_failed)
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
                val url = modifySpecificUrls(intent.getStringExtra(Intent.EXTRA_TEXT))!!
                if (!URLUtil.isNetworkUrl(url)) throw RuntimeException("invalid url shared")

                var entry: Entry? = intent.getSerializableExtra(EXTRA_ENTRY) as? Entry

                if (entry == null) {
                    try {
                        entry = HatenaClient.searchEntriesAsync(url, SearchType.Text).await()
                            .firstOrNull { it.url == url }
                    }
                    catch (e: Exception) {
                        Log.e("FetchBookmarks", Log.getStackTraceString(e))
                    }
                }

                if (entry == null) {
                    mBookmarksEntry =
                        try {
                            HatenaClient.getEmptyBookmarksEntryAsync(url).await()
                        }
                        catch (e: Exception) {
                            Log.e("FetchBookmarks", Log.getStackTraceString(e))
                            null
                        }
                    entry = Entry(
                        id = 0,
                        title = mBookmarksEntry!!.title,
                        description = "",
                        count = 0,
                        url = mBookmarksEntry!!.url,
                        rootUrl = Uri.parse(mBookmarksEntry!!.url).let { it.scheme!! + "://" + it.host!! },
                        faviconUrl = null,
                        imageUrl = mBookmarksEntry!!.screenshot)
                }

                mEntry = entry
            }

            else -> {
                mEntry = intent.getSerializableExtra(EXTRA_ENTRY) as? Entry
            }
        }
    }

    override fun onPostBookmark(result: BookmarkResult) {
        onBackPressed() // 投稿完了でアクティビティを閉じる
    }
}
