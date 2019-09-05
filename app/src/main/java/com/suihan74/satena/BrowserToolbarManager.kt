package com.suihan74.satena

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.support.customtabs.CustomTabsIntent
import android.widget.RemoteViews
import com.suihan74.HatenaLib.Entry
import com.suihan74.satena.activities.BookmarkPostActivity
import com.suihan74.satena.activities.BookmarksActivity

class BrowserToolbarManager : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val clickedId = intent.getIntExtra(CustomTabsIntent.EXTRA_REMOTEVIEWS_CLICKED_ID, -1)
        val bIntent = when (clickedId) {
            R.id.bookmark_button -> {
                Intent(context, BookmarkPostActivity::class.java).apply {
                    putExtra(BookmarkPostActivity.EXTRA_ENTRY, entry)
                }
            }

            R.id.show_bookmarks_button -> {
                Intent(context, BookmarksActivity::class.java).apply {
                    action = Intent.ACTION_SEND
                    putExtra(Intent.EXTRA_TEXT, entry?.url ?: intent.dataString)
                    putExtra(BookmarksActivity.EXTRA_ENTRY, entry)
                }
            }

            else -> throw NotImplementedError()
        }
        context.startActivity(bIntent)
    }

    companion object {
        private var entry: Entry? = null

        fun createRemoteViews(context: Context, entry: Entry?): RemoteViews {
            this.entry = entry
            if (entry != null) {
                BookmarksActivity.startPreLoading(entry)
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
