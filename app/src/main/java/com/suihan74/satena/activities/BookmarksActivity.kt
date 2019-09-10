package com.suihan74.satena.activities

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.webkit.URLUtil
import com.suihan74.HatenaLib.*
import com.suihan74.satena.R
import com.suihan74.satena.fragments.BookmarkPostFragment
import com.suihan74.satena.fragments.BookmarksFragment
import com.suihan74.satena.models.PreferenceKey
import com.suihan74.utilities.AccountLoader
import com.suihan74.utilities.SafeSharedPreferences
import kotlinx.coroutines.*

class BookmarksActivity : ActivityBase() {
    // プリロード中のブクマ・スター
    data class PreLoadingTasks (
        val bookmarksTask : Deferred<BookmarksEntry>,
        val bookmarksDigestTask : Deferred<BookmarksDigest>,
        val bookmarksRecentTask : Deferred<List<BookmarkWithStarCount>>,
        val starsEntriesTask : Deferred<List<StarsEntry>>
    )

    private var mEntry : Entry = emptyEntry()
    private var mBookmarksEntry : BookmarksEntry? = null

    private var mPostFragment : BookmarkPostFragment? = null

    private var mIsDialogOpened = false

    var bookmarksFragment : BookmarksFragment? = null
        private set

    val entry : Entry
        get() = mEntry

    override val containerId = R.id.bookmarks_layout
    override val progressBarId: Int? = R.id.detail_progress_bar
    override val progressBackgroundId: Int? = R.id.click_guard

    init {
        setShowingProgressBarAction { progressBar, _ ->
            progressBar?.apply {
                scaleX = 1f
                scaleY = 1f
                alpha = 1f
            }
        }

        setHidingProgressBarAction { progressBar, background ->
            if (progressBar?.visibility == View.VISIBLE) {
                progressBar.apply {
                    animate()
                        .withEndAction {
                            visibility = View.INVISIBLE
                            background?.visibility = View.INVISIBLE
                        }
                        .alphaBy(1f)
                        .alpha(0f)
                        .scaleX(4f)
                        .scaleY(4f)
                        .duration = 250
                }
            }
        }
    }

    companion object {
        fun startPreLoading(entry: Entry) {
            preLoadingTasks = null

            val bookmarksEntryTask = HatenaClient.getBookmarksEntryAsync(entry.url)
            val digestTask = HatenaClient.getDigestBookmarksAsync(entry.url)
            val recentTask = HatenaClient.getRecentBookmarksAsync(entry.url)

            val starsEntriesTask = GlobalScope.async {
                while (true) {
                    if (bookmarksEntryTask.isCompleted || bookmarksEntryTask.isCancelled) break
                    yield()
                }

                if (bookmarksEntryTask.isCompleted) {
                    val bookmarksEntry = bookmarksEntryTask.await()
                    val urls = bookmarksEntry.bookmarks
                        .filter { it.comment.isNotBlank() }
                        .map { it.getBookmarkUrl(entry) }

                    val result = HatenaClient.getStarsEntryAsync(urls).await()
                    Log.d("preLoading", "completed!")

                    result
                }
                else {
                    emptyList()
                }
            }

            preLoadingTasks = PreLoadingTasks(
                bookmarksEntryTask,
                digestTask,
                recentTask,
                starsEntriesTask
            )
        }
        private var preLoadingTasks : PreLoadingTasks? = null
        const val EXTRA_ENTRY = "com.suihan74.satena.activities.BookmarksActivity.EXTRA_ENTRY"
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
        setContentView(R.layout.activity_bookmarks)


        if (savedInstanceState == null) {
            showProgressBar()
        }

        findViewById<View>(R.id.post_bookmark_background).setOnClickListener {
            closeBookmarkDialog()
        }

        launch(Dispatchers.IO) {
            val entry = when (intent.action) {
                // ブラウザから「共有」を使って遷移してきたときの処理
                Intent.ACTION_SEND -> {
                    val url = intent.extras?.getCharSequence(Intent.EXTRA_TEXT).toString()
                    if (!URLUtil.isNetworkUrl(url)) throw RuntimeException("invalid url shared")

                    var entry: Entry? = intent.extras?.getSerializable(EXTRA_ENTRY) as? Entry
                    AccountLoader.signInAccounts(applicationContext)

                    if (entry == null) {
                        preLoadingTasks = null

                        try {
                            entry = HatenaClient.searchEntriesAsync(url, SearchType.Text).await()
                                .firstOrNull { it.url == url }
                        }
                        catch (e: Exception) {
                            Log.d("failedToFetchBookmarks", Log.getStackTraceString(e))
                        }
                    }

                    if (entry == null) {
                        try {
                            mBookmarksEntry = HatenaClient.getEmptyBookmarksEntryAsync(url).await()
                        } catch (e: Exception) {
                            Log.d("failedToFetchBookmarks", Log.getStackTraceString(e))
                        }
                    }

                    entry ?: Entry(0, mBookmarksEntry?.title ?: "", "", 0, url, url, "", "")
                }

                // ブコメページのリンクを踏んだときの処理
                Intent.ACTION_VIEW -> {
                    val commentUrl = intent.dataString ?: ""
                    val url = HatenaClient.getEntryUrlFromCommentPageUrl(commentUrl)

                    var entry: Entry? = null
                    AccountLoader.signInAccounts(applicationContext)
                    try {
                        entry = HatenaClient.searchEntriesAsync(url, SearchType.Text).await()
                            .firstOrNull { it.url == url }
                    } catch (e: Exception) {
                        Log.d("failedToFetchBookmarks", Log.getStackTraceString(e))
                    }
                    entry
                }

                // MainActivityからの遷移
                else -> {
                    preLoadingTasks = null

                    val eid = intent.extras?.getLong("eid") ?: 0L
                    if (eid == 0L) {
                        intent.getSerializableExtra("entry")
                    } else {
                        HatenaClient.getBookmarksEntryAsync(eid).await()
                    }
                }
            }

            if (entry is Entry) {
                mEntry = entry
            }
            else if (entry is BookmarksEntry) {
                mEntry = Entry(
                    id = entry.id,
                    title = entry.title,
                    description = "",
                    count = entry.count,
                    url = entry.url,
                    rootUrl = entry.url,
                    faviconUrl = "",
                    imageUrl = entry.screenshot,
                    bookmarkedData = null
                )
                mBookmarksEntry = entry
            }

            // ブクマダイアログの設定
            if (HatenaClient.signedIn()) {
                withContext(Dispatchers.Main) {
                    mPostFragment =
                        BookmarkPostFragment.createInstance(mEntry, mBookmarksEntry).apply {
                            setOnPostedListener {
                                closeBookmarkDialog()
                            }

                            arguments = Bundle().apply {
                                putInt(BookmarkPostFragment.INITIAL_VISIBILITY, View.GONE)
                            }
                        }

                    supportFragmentManager.beginTransaction()
                        .replace(R.id.bookmark_post_layout, mPostFragment!!)
                        .commit()
                }
            }

            // フラグメント表示
            if (isFragmentShowed()) {
                val fragment = currentFragment
                if (fragment is BookmarksFragment) {
                    bookmarksFragment = fragment
                }
            }
            else {
                withContext(Dispatchers.Main) {
                    showProgressBar()
                    val targetUser = intent.extras?.getString("target_user")
                    bookmarksFragment =
                        if (targetUser == null) {
                            BookmarksFragment.createInstance(mEntry, preLoadingTasks)
                        } else {
                            BookmarksFragment.createInstance(targetUser, mEntry, preLoadingTasks)
                        }
                    showFragment(bookmarksFragment!!)
                }
            }
        }
    }


    fun openBookmarkDialog() {
        mIsDialogOpened = true

        val bg = findViewById<View>(R.id.post_bookmark_background)
        val view = mPostFragment!!.root
        val posY = resources.getDimension(R.dimen.dp_80)

        bg.visibility = View.VISIBLE
        bg.animate()
            .alphaBy(0f)
            .alpha(1f)
            .duration = 150

        view.alpha = 0f
        view.translationY = posY
        view.visibility = View.VISIBLE
        view.animate()
            .withEndAction {
                mPostFragment!!.focus()
            }
            .translationYBy(posY)
            .translationY(0f)
            .alphaBy(0f)
            .alpha(1f)
            .duration = 150
    }

    fun closeBookmarkDialog() {
        mIsDialogOpened = false

        val bg = findViewById<View>(R.id.post_bookmark_background)
        val view = mPostFragment!!.root
        val posY = resources.getDimension(R.dimen.dp_80)

        // キーボードを閉じる
        mPostFragment!!.unFocus()

        bg.animate()
            .withEndAction {
                bg.visibility = View.GONE
            }
            .alphaBy(1f)
            .alpha(0f)
            .duration = 150

        view.animate()
            .withEndAction {
                view.visibility = View.INVISIBLE
            }
            .translationYBy(0f)
            .translationY(posY)
            .alphaBy(1f)
            .alpha(0f)
            .duration = 150
    }

    override fun onBackPressed() {
        if (mIsDialogOpened) {
            closeBookmarkDialog()
        }
        else {
            super.onBackPressed()
        }
    }
}
