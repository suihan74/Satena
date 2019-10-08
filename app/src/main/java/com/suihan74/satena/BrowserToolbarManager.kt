package com.suihan74.satena

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.widget.RemoteViews
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.content.ContextCompat
import com.suihan74.HatenaLib.Entry
import com.suihan74.HatenaLib.HatenaClient
import com.suihan74.HatenaLib.SearchType
import com.suihan74.satena.activities.BookmarkPostActivity
import com.suihan74.satena.activities.BookmarksActivity
import kotlinx.coroutines.*
import org.chromium.customtabsclient.shared.CustomTabsHelper

class BrowserToolbarManager : BroadcastReceiver() {
    private fun onReceiveImpl(context: Context, intent: Intent) {
        val url = intent.dataString ?: entry?.url ?: return

        val clickedId = intent.getIntExtra(CustomTabsIntent.EXTRA_REMOTEVIEWS_CLICKED_ID, -1)
        val bIntent = when (clickedId) {
            R.id.bookmark_button -> {
                Intent(context, BookmarkPostActivity::class.java).apply {
                    action = Intent.ACTION_SEND
                    putExtra(Intent.EXTRA_TEXT, url)
                    if (url == entry?.url || url == entry?.ampUrl) {
                        putExtra(BookmarkPostActivity.EXTRA_ENTRY, entry)
                    }
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
            }

            R.id.show_bookmarks_button -> {
                Intent(context, BookmarksActivity::class.java).apply {
                    action = Intent.ACTION_SEND
                    putExtra(Intent.EXTRA_TEXT, url)
                    if (url == entry?.url || url == entry?.ampUrl) {
                        putExtra(BookmarksActivity.EXTRA_ENTRY, entry)
                    }
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
            }

            else -> throw NotImplementedError()
        }
        context.startActivity(bIntent)
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (entryLoadingJob != null) {
            entryLoadingJob!!.invokeOnCompletion {
                entryLoadingJob = null
                onReceiveImpl(context, intent)
            }
        }
        else {
            onReceiveImpl(context, intent)
        }
    }

    companion object {
        private var entry: Entry? = null
        private var entryLoadingJob: Job? = null

        fun createRemoteViews(context: Context, entry: Entry?): RemoteViews {
            this.entry = entry
            if (entry != null) {
                BookmarksActivity.startPreLoading(entry)
            }
            return RemoteViews(context.packageName, R.layout.browser_toolbar)
        }

        fun createRemoteViews(context: Context, url: String, coroutineScope: CoroutineScope) : RemoteViews {
            entryLoadingJob = coroutineScope.launch(Dispatchers.IO) {
                try {
                    entry = HatenaClient.searchEntriesAsync(url, SearchType.Text).await()
                        .firstOrNull { it.url == url }
                }
                catch (e: Exception) {
                    Log.d("BrowserToolbarManager", Log.getStackTraceString(e))
                }

                if (entry == null) {
                    try {
                        val bookmarksEntry = HatenaClient.getEmptyBookmarksEntryAsync(url).await()
                        entry = Entry(0, bookmarksEntry.title, "", 0, url, url, "", "")
                    } catch (e: Exception) {
                        Log.d("BrowserToolbarManager", Log.getStackTraceString(e))
                    }
                }

                if (entry == null) {
                    Log.e("BrowserToolbarmanager", "failed to fetch the entry: $url")
                }
                else {
                    BookmarksActivity.startPreLoading(entry!!)
                }
            }

            return RemoteViews(context.packageName, R.layout.browser_toolbar)
        }

        fun getClickableIds() = intArrayOf(R.id.bookmark_button, R.id.show_bookmarks_button)

        fun getOnClickPendingIntent(context: Context): PendingIntent {
            val broadcastIntent = Intent(context, BrowserToolbarManager::class.java)
            return PendingIntent.getBroadcast(context, 0, broadcastIntent, 0)
        }
    }
}

fun Context.showCustomTabsIntent(entry: Entry) {
    val url = entry.ampUrl ?: entry.url

    val intent = CustomTabsIntent.Builder()
        .setShowTitle(true)
        .enableUrlBarHiding()
        .addDefaultShareMenuItem()
        .setToolbarColor(ContextCompat.getColor(this, R.color.colorPrimary))
        .setSecondaryToolbarColor(ContextCompat.getColor(this, R.color.colorPrimary))
        .setSecondaryToolbarViews(
            BrowserToolbarManager.createRemoteViews(this, entry),
            BrowserToolbarManager.getClickableIds(),
            BrowserToolbarManager.getOnClickPendingIntent(this)
        )
        .build()

    val packageName = CustomTabsHelper.getPackageNameToUse(this)
    intent.intent.setPackage(packageName)
    intent.launchUrl(this, Uri.parse(url))
}

fun Context.showCustomTabsIntent(url: String, coroutineScope: CoroutineScope = GlobalScope) {
    val intent = CustomTabsIntent.Builder()
        .setShowTitle(true)
        .enableUrlBarHiding()
        .addDefaultShareMenuItem()
        .setToolbarColor(ContextCompat.getColor(this, R.color.colorPrimary))
        .setSecondaryToolbarColor(ContextCompat.getColor(this, R.color.colorPrimary))
        .setSecondaryToolbarViews(
            BrowserToolbarManager.createRemoteViews(this, url, coroutineScope),
            BrowserToolbarManager.getClickableIds(),
            BrowserToolbarManager.getOnClickPendingIntent(this)
        )
        .build()

    val packageName = CustomTabsHelper.getPackageNameToUse(this)
    intent.intent.setPackage(packageName)
    intent.launchUrl(this, Uri.parse(url))
}
