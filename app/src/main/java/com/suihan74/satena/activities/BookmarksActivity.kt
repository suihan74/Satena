package com.suihan74.satena.activities

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.webkit.URLUtil
import androidx.fragment.app.Fragment
import com.suihan74.HatenaLib.*
import com.suihan74.satena.R
import com.suihan74.satena.fragments.BookmarkPostFragment
import com.suihan74.satena.fragments.BookmarksFragment
import com.suihan74.satena.models.PreferenceKey
import com.suihan74.utilities.AccountLoader
import com.suihan74.utilities.SafeSharedPreferences
import com.suihan74.utilities.lock
import kotlinx.coroutines.*

class BookmarksActivity : ActivityBase() {
    // プリロード中のブクマ・スター
    data class PreLoadingTasks (
        val bookmarksTask : Deferred<BookmarksEntry>,
        val bookmarksDigestTask : Deferred<BookmarksDigest>,
        val bookmarksRecentTask : Deferred<List<BookmarkWithStarCount>>,
        val starsEntriesTask : Deferred<List<StarsEntry>>
    ) {
        private val monitorTask = GlobalScope.launch(SupervisorJob() + Dispatchers.Default) {
            try {
                listOf(bookmarksTask, bookmarksDigestTask, bookmarksRecentTask, starsEntriesTask)
                    .awaitAll()
                Log.d("preloading", "completed")
            }
            catch (e: Exception) {
                Log.d("preloading", "canceled")
            }
        }

        fun cancel() {
            bookmarksTask.cancel()
            bookmarksDigestTask.cancel()
            bookmarksRecentTask.cancel()
            starsEntriesTask.cancel()
        }
    }

    private var mEntry : Entry? = null
    private var mBookmarksEntry : BookmarksEntry? = null

    private var mPostFragment : BookmarkPostFragment? = null

    private var mIsDialogOpened = false

    // TODO: 一番上にあるBookmarksFragmentを返す（暫定）
    val bookmarksFragment
        get() = getStackedFragment<BookmarksFragment>(FRAGMENT_TAG_MAIN_CONTENT)

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
        private var mPreloadingStopped = false
        private var preloadingStopped : Boolean
            get() {
                lock(mPreloadingStopped) {
                    return mPreloadingStopped
                }
            }
            private set(value) {
                lock(mPreloadingStopped) {
                    mPreloadingStopped = value
                }
            }

        fun stopPreLoading() {
            preloadingStopped = true
        }

        fun startPreLoading(entry: Entry) {
            preLoadingTasks?.cancel()
            preLoadingTasks = null
            preloadingStopped = false

            val bookmarksEntryTask = HatenaClient.getBookmarksEntryAsync(entry.url)
            val digestTask = HatenaClient.getDigestBookmarksAsync(entry.url)

            val recentTask = GlobalScope.async {
                val list = ArrayList<BookmarkWithStarCount>()
                while (!preloadingStopped) {
                    try {
                        val cur = HatenaClient.getRecentBookmarksAsync(entry.url, of = list.size.toLong()).await()

                        if (cur.isEmpty()) {
                            break
                        }
                        else {
                            list.addAll(cur.filterNot { c -> list.any { it.user == c.user } })
                            list.sortByDescending { it.timestamp }
                        }
                    }
                    catch (e: Exception) {
                        Log.d("preloading", e.message)
                        break
                    }
                }
                return@async list
            }

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

                    var result : List<StarsEntry>? = null
                    for (times in 1..5) {
                        try {
                            result = HatenaClient.getStarsEntryAsync(urls).await()
                        }
                        catch (e: Exception) {
                            if (times == 5) {
                                Log.e("preLoading", "failed to fetch stars")
                            }
                            else {
                                Log.d("preLoading", "failed to fetch stars. retrying...")
                            }
                        }
                        break
                    }
                    result ?: emptyList()
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
        var preLoadingTasks : PreLoadingTasks? = null
            private set

        private const val EXTRA_BASE = "com.suihan74.satena.activities.BookmarksActivity."
        const val EXTRA_ENTRY = EXTRA_BASE + "EXTRA_ENTRY"

        private const val BUNDLE_ENTRY = "mEntry"
        private const val BUNDLE_POST_DIALOG_OPENED = "mIsDialogOpened"

        const val FRAGMENT_TAG_MAIN_CONTENT = "bookmarks_main_content"
        const val FRAGMENT_TAG_POST = "bookmark_post_fragment"
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.run {
            putSerializable(BUNDLE_ENTRY, mEntry)
            putBoolean(BUNDLE_POST_DIALOG_OPENED, mIsDialogOpened)
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
        setContentView(R.layout.activity_bookmarks)

        setSupportActionBar(toolbar)
        supportActionBar?.hide()

        savedInstanceState?.let {
            mEntry = it.getSerializable(BUNDLE_ENTRY) as Entry
            mBookmarksEntry = null
            mPostFragment = getStackedFragment<BookmarkPostFragment>(FRAGMENT_TAG_POST)
            mIsDialogOpened = it.getBoolean(BUNDLE_POST_DIALOG_OPENED)
        } ?: showProgressBar()

        findViewById<View>(R.id.post_bookmark_background).setOnClickListener {
            closeBookmarkDialog()
        }

        if (savedInstanceState == null) {
            startInitialize()
        }
    }

    override fun onResume() {
        super.onResume()

        if (mIsDialogOpened) {
            openBookmarkDialog()
        }
    }

    override fun onAttachFragment(fragment: Fragment) {
        super.onAttachFragment(fragment)
        updateToolbar(fragment)
    }

    private fun startInitialize() = launch(Dispatchers.Default) {
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

        // フラグメント表示
        withContext(Dispatchers.Main) {
            showProgressBar()
            val targetUser = intent.extras?.getString("target_user")
            val targetEntry = this@BookmarksActivity.mEntry!!

            val bookmarksFragment =
                if (targetUser == null) {
                    BookmarksFragment.createInstance(targetEntry, preLoadingTasks)
                } else {
                    BookmarksFragment.createInstance(targetUser, targetEntry, preLoadingTasks)
                }

            showFragment(bookmarksFragment, FRAGMENT_TAG_MAIN_CONTENT)
        }
    }

    private fun initializeBookmarkPostFragment(entry: Entry, bookmarksEntry: BookmarksEntry?) {
        if (HatenaClient.signedIn()) {
            mPostFragment =
                BookmarkPostFragment.createInstance(entry, bookmarksEntry).apply {
                    setOnPostedListener { bookmarkResult ->
                        closeBookmarkDialog()
                        launch(Dispatchers.Main) {
                            val bookmarksFragment = bookmarksFragment!!
                            bookmarksFragment.addBookmark(bookmarkResult)
                            bookmarksFragment.updateUI()
                        }
                    }

                    arguments = Bundle().apply {
                        putInt(BookmarkPostFragment.INITIAL_VISIBILITY, View.GONE)
                    }
                }

            supportFragmentManager.beginTransaction()
                .replace(R.id.bookmark_post_layout, mPostFragment!!)
                .commitNow()
        }
    }

    fun openBookmarkDialog() {
        val fragment = bookmarksFragment ?: return
        initializeBookmarkPostFragment(fragment.entry, fragment.bookmarksEntry)

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
                supportFragmentManager.beginTransaction()
                    .remove(mPostFragment!!)
                    .commit()
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
