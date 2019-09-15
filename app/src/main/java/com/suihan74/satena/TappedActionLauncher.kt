package com.suihan74.satena

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.support.v7.app.AlertDialog
import com.suihan74.HatenaLib.Entry
import com.suihan74.HatenaLib.HatenaClient
import com.suihan74.satena.activities.BookmarksActivity
import com.suihan74.satena.models.TapEntryAction

object TappedActionLauncher {
    fun launch(context: Context, act: TapEntryAction, url: String) = when (act) {
        TapEntryAction.SHOW_COMMENTS -> launchBookmarksActivity(context, url)
        TapEntryAction.SHOW_PAGE -> launchTabs(context, url)
        TapEntryAction.SHOW_PAGE_IN_BROWSER -> launchBrowser(context, url)
        TapEntryAction.SHOW_MENU -> showMenu(context, url, null)
    }

    fun launch(context: Context, act: TapEntryAction, url: String, additionalItemsAdder: (ArrayList<Pair<Int, () -> Unit>>)->Unit) = when (act) {
        TapEntryAction.SHOW_COMMENTS -> launchBookmarksActivity(context, url)
        TapEntryAction.SHOW_PAGE -> launchTabs(context, url)
        TapEntryAction.SHOW_PAGE_IN_BROWSER -> launchBrowser(context, url)
        TapEntryAction.SHOW_MENU -> showMenu(context, url, additionalItemsAdder)
    }


    fun launch(context: Context, act: TapEntryAction, entry: Entry) = when (act) {
        TapEntryAction.SHOW_COMMENTS -> launchBookmarksActivity(context, entry)
        TapEntryAction.SHOW_PAGE -> launchTabs(context, entry)
        TapEntryAction.SHOW_PAGE_IN_BROWSER -> launchBrowser(context, entry)
        TapEntryAction.SHOW_MENU -> showMenu(context, entry, null)
    }

    fun launch(context: Context, act: TapEntryAction, entry: Entry, additionalItemsAdder: (ArrayList<Pair<Int, () -> Unit>>)->Unit) = when (act) {
        TapEntryAction.SHOW_COMMENTS -> launchBookmarksActivity(context, entry)
        TapEntryAction.SHOW_PAGE -> launchTabs(context, entry)
        TapEntryAction.SHOW_PAGE_IN_BROWSER -> launchBrowser(context, entry)
        TapEntryAction.SHOW_MENU -> showMenu(context, entry, additionalItemsAdder)
    }

    private fun launchBookmarksActivity(context: Context, url: String) {
        val entryUrl = HatenaClient.getCommentPageUrlFromEntryUrl(url)
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(entryUrl)).apply {
            setPackage("com.suihan74.satena")
            extras?.putCharSequence(Intent.EXTRA_TEXT, entryUrl)
        }
        context.startActivity(intent)
    }

    private fun launchBookmarksActivity(context: Context, entry: Entry) {
        val intent = Intent(context, BookmarksActivity::class.java)
        intent.putExtra("entry", entry)
        context.startActivity(intent)
    }

    private fun launchTabs(context: Context, url: String) {
        context.showCustomTabsIntent(url)
    }

    private fun launchTabs(context: Context, entry: Entry) {
        context.showCustomTabsIntent(entry)
    }

    private fun launchBrowser(context: Context, url: String) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        context.startActivity(intent)
    }

    private fun launchBrowser(context: Context, entry: Entry) =
        launchBrowser(context, entry.ampUrl ?: entry.url)

    private fun showMenu(context: Context, url: String, additionalItemsAdder: ((ArrayList<Pair<Int, () -> Unit>>) -> Unit)?) {
        val items = arrayListOf(
            TapEntryAction.SHOW_COMMENTS.titleId to { launchBookmarksActivity(context, url) },
            TapEntryAction.SHOW_PAGE.titleId to { launchTabs(context, url) },
            TapEntryAction.SHOW_PAGE_IN_BROWSER.titleId to { launchBrowser(context, url) }
        )

        additionalItemsAdder?.invoke(items)

        AlertDialog.Builder(context, R.style.AlertDialogStyle)
            .setTitle(url)
            .setNegativeButton("Cancel", null)
            .setItems(items.map { context.getText(it.first) }.toTypedArray()) { _, which ->
                items[which].second()
            }
            .show()
    }

    private fun showMenu(context: Context, entry: Entry, additionalItemsAdder: ((ArrayList<Pair<Int, () -> Unit>>)->Unit)?) {
        val items = arrayListOf(
            TapEntryAction.SHOW_COMMENTS.titleId to { launchBookmarksActivity(context, entry) },
            TapEntryAction.SHOW_PAGE.titleId to { launchTabs(context, entry) },
            TapEntryAction.SHOW_PAGE_IN_BROWSER.titleId to { launchBrowser(context, entry) }
        )

        additionalItemsAdder?.invoke(items)

        AlertDialog.Builder(context, R.style.AlertDialogStyle)
            .setTitle(entry.title)
            .setNegativeButton("Cancel", null)
            .setItems(items.map { context.getText(it.first) }.toTypedArray()) { _, which ->
                items[which].second()
            }
            .show()
    }
}
