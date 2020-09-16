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
import com.suihan74.hatenaLib.Entry
import com.suihan74.hatenaLib.HatenaClient
import com.suihan74.satena.models.BrowserSettingsKey
import com.suihan74.satena.models.PreferenceKey
import com.suihan74.satena.scenes.bookmarks2.BookmarksActivity
import com.suihan74.satena.scenes.browser.BrowserActivity
import com.suihan74.satena.scenes.browser.BrowserMode
import com.suihan74.satena.scenes.post2.BookmarkPostActivity
import com.suihan74.utilities.SafeSharedPreferences
import com.suihan74.utilities.putObjectExtra
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.chromium.customtabsclient.shared.CustomTabsHelper

class BrowserToolbarManager : BroadcastReceiver() {
    private fun onReceiveImpl(context: Context, intent: Intent) {
        val url = intent.dataString
            ?: entry?.url
            ?: return

        val bIntent = when (intent.getIntExtra(CustomTabsIntent.EXTRA_REMOTEVIEWS_CLICKED_ID, -1)) {
            R.id.bookmark_button ->
                Intent(context, BookmarkPostActivity::class.java).apply {
                    action = Intent.ACTION_SEND
                    putExtra(Intent.EXTRA_TEXT, url)
                    if (url == entry?.url || url == entry?.ampUrl) {
                        putObjectExtra(BookmarkPostActivity.EXTRA_ENTRY, entry)
                    }
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }

            R.id.show_bookmarks_button ->
                Intent(context, BookmarksActivity::class.java).apply {
                    action = Intent.ACTION_SEND
                    putExtra(Intent.EXTRA_TEXT, url)
                    if (url == entry?.url || url == entry?.ampUrl) {
                        putObjectExtra(BookmarksActivity.EXTRA_ENTRY, entry)
                    }
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }

            else -> throw NotImplementedError()
        }
        context.startActivity(bIntent)
    }

    override fun onReceive(context: Context, intent: Intent) {
        entryLoadingJob?.invokeOnCompletion {
            entryLoadingJob = null
            onReceiveImpl(context, intent)
        } ?: onReceiveImpl(context, intent)
    }

    companion object {
        private var entry: Entry? = null
        private var entryLoadingJob: Job? = null

        fun createRemoteViews(context: Context, entry: Entry?): RemoteViews {
            this.entry = entry
            return RemoteViews(context.packageName, R.layout.browser_toolbar)
        }

        fun createRemoteViews(context: Context, url: String) : RemoteViews {
            entryLoadingJob = GlobalScope.launch(Dispatchers.IO) {
                try {
                    entry = HatenaClient.getEntryAsync(url).await()
                    /*HatenaClient.searchEntriesAsync(url, SearchType.Text).await()
                        .firstOrNull { it.url == url }*/
                }
                catch (e: Throwable) {
                    Log.d("BrowserToolbarManager", Log.getStackTraceString(e))
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

private fun Context.startInnerBrowser(remoteViews: RemoteViews, url: String) = CustomTabsIntent
    .Builder()
    .setShowTitle(true)
    .enableUrlBarHiding()
    .addDefaultShareMenuItem()
    .setToolbarColor(ContextCompat.getColor(this, R.color.colorPrimary))
    .setSecondaryToolbarColor(ContextCompat.getColor(this, R.color.colorPrimary))
    .setSecondaryToolbarViews(
        remoteViews,
        BrowserToolbarManager.getClickableIds(),
        BrowserToolbarManager.getOnClickPendingIntent(this)
    )
    .build()
    .let {
        val packageName = CustomTabsHelper.getPackageNameToUse(this)
        it.intent.setPackage(packageName)
        it.intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
        it.launchUrl(this, Uri.parse(url))
    }

fun Context.startInnerBrowser(entry: Entry) {
    val prefs = SafeSharedPreferences.create<PreferenceKey>(this)
    when (BrowserMode.fromInt(prefs.getInt(PreferenceKey.BROWSER_MODE))) {
        BrowserMode.CUSTOM_TABS_INTENT ->
            startInnerBrowser(
                BrowserToolbarManager.createRemoteViews(this, entry),
                entry.url
            )

        BrowserMode.WEB_VIEW -> {
            val intent = Intent(this, BrowserActivity::class.java).apply {
                putExtra(BrowserActivity.EXTRA_URL, entry.url)
            }
            startActivity(intent)
        }
    }
}

fun Context.startInnerBrowser(url: String? = null) {
    val prefs = SafeSharedPreferences.create<PreferenceKey>(this)
    when (BrowserMode.fromInt(prefs.getInt(PreferenceKey.BROWSER_MODE))) {
        BrowserMode.CUSTOM_TABS_INTENT -> {
            val startUrl = url ?: let {
                val browserSettings = SafeSharedPreferences.create<BrowserSettingsKey>(this)
                browserSettings.get(BrowserSettingsKey.START_PAGE_URL)
            }

            startInnerBrowser(
                BrowserToolbarManager.createRemoteViews(this, startUrl),
                startUrl
            )
        }

        BrowserMode.WEB_VIEW -> {
            val intent = Intent(this, BrowserActivity::class.java).apply {
                putExtra(BrowserActivity.EXTRA_URL, url)
            }
            startActivity(intent)
        }
    }
}
