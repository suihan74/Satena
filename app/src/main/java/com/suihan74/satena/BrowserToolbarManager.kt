package com.suihan74.satena

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.webkit.URLUtil
import android.widget.RemoteViews
import androidx.browser.customtabs.CustomTabColorSchemeParams
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.content.ContextCompat
import com.suihan74.hatenaLib.Entry
import com.suihan74.satena.models.BrowserSettingsKey
import com.suihan74.satena.models.PreferenceKey
import com.suihan74.satena.scenes.bookmarks.BookmarksActivity
import com.suihan74.satena.scenes.browser.BrowserActivity
import com.suihan74.satena.scenes.browser.BrowserMode
import com.suihan74.satena.scenes.post.BookmarkPostActivity
import com.suihan74.utilities.SafeSharedPreferences
import org.chromium.customtabsclient.shared.CustomTabsHelper

class BrowserToolbarManager : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val url = intent.dataString ?: return

        val bIntent = when (intent.getIntExtra(CustomTabsIntent.EXTRA_REMOTEVIEWS_CLICKED_ID, -1)) {
            R.id.bookmark_button ->
                Intent(context, BookmarkPostActivity::class.java).apply {
                    action = Intent.ACTION_SEND
                    putExtra(Intent.EXTRA_TEXT, url)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }

            R.id.show_bookmarks_button ->
                Intent(context, BookmarksActivity::class.java).apply {
                    action = Intent.ACTION_SEND
                    putExtra(Intent.EXTRA_TEXT, url)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }

            else -> throw NotImplementedError()
        }
        context.startActivity(bIntent)
    }

    companion object {
        fun createRemoteViews(context: Context): RemoteViews {
            return RemoteViews(context.packageName, R.layout.browser_toolbar)
        }

        fun getClickableIds() = intArrayOf(R.id.bookmark_button, R.id.show_bookmarks_button)

        fun getOnClickPendingIntent(context: Context): PendingIntent {
            val broadcastIntent = Intent(context, BrowserToolbarManager::class.java)
            return PendingIntent.getBroadcast(context, 0, broadcastIntent, 0)
        }
    }
}

@SuppressLint("WrongConstant")
private fun Context.startInnerBrowser(remoteViews: RemoteViews, url: String) = CustomTabsIntent
    .Builder()
    .setShowTitle(true)
    .setUrlBarHidingEnabled(true)
    .setShareState(CustomTabsIntent.SHARE_STATE_ON)
    .setDefaultColorSchemeParams(CustomTabColorSchemeParams.Builder()
        .setToolbarColor(ContextCompat.getColor(this, R.color.colorPrimary))
        .setSecondaryToolbarColor(ContextCompat.getColor(this, R.color.colorPrimary))
        .build())
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
    when (BrowserMode.fromId(prefs.getInt(PreferenceKey.BROWSER_MODE))) {
        BrowserMode.CUSTOM_TABS_INTENT ->
            startInnerBrowser(
                BrowserToolbarManager.createRemoteViews(this),
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
    when (BrowserMode.fromId(prefs.getInt(PreferenceKey.BROWSER_MODE))) {
        BrowserMode.CUSTOM_TABS_INTENT -> {
            val startUrl = with(
                url ?: let {
                    val browserSettings = SafeSharedPreferences.create<BrowserSettingsKey>(this)
                    browserSettings.get(BrowserSettingsKey.START_PAGE_URL)
                }
            ) {
                if (URLUtil.isValidUrl(this)) this
                else "https://www.google.com/search?q=${Uri.encode(this)}"
            }

            startInnerBrowser(
                BrowserToolbarManager.createRemoteViews(this),
                startUrl
            )
        }

        BrowserMode.WEB_VIEW -> {
            if (this is BrowserActivity) {
                this.viewModel.goAddress(url)
            }
            else {
                val intent = Intent(this, BrowserActivity::class.java).apply {
                    putExtra(BrowserActivity.EXTRA_URL, url)
                }
                startActivity(intent)
            }
        }
    }
}
