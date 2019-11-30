package com.suihan74.satena

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.suihan74.HatenaLib.Entry
import com.suihan74.HatenaLib.HatenaClient
import com.suihan74.satena.dialogs.EntryMenuDialog
import com.suihan74.satena.models.TapEntryAction
import com.suihan74.satena.scenes.bookmarks.BookmarksActivity
import com.suihan74.satena.scenes.entries.EntriesTabFragmentBase
import com.suihan74.utilities.CoroutineScopeFragment

object TappedActionLauncher {
    fun launch(context: Context, act: TapEntryAction, url: String, fragment: CoroutineScopeFragment? = null) = when (act) {
        TapEntryAction.SHOW_COMMENTS -> launchBookmarksActivity(context, url)
        TapEntryAction.SHOW_PAGE -> launchTabs(context, url)
        TapEntryAction.SHOW_PAGE_IN_BROWSER -> launchBrowser(context, url)
        TapEntryAction.SHOW_MENU -> showMenu(context, url, fragment!!)
    }

    fun launch(context: Context, act: TapEntryAction, entry: Entry, fragment: EntriesTabFragmentBase? = null) = when (act) {
        TapEntryAction.SHOW_COMMENTS -> launchBookmarksActivity(context, entry)
        TapEntryAction.SHOW_PAGE -> launchTabs(context, entry)
        TapEntryAction.SHOW_PAGE_IN_BROWSER -> launchBrowser(context, entry)
        TapEntryAction.SHOW_MENU -> showMenu(context, entry, fragment)
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
        intent.putExtra(BookmarksActivity.EXTRA_ENTRY, entry)
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

    private fun showMenu(context: Context, url: String, fragment: CoroutineScopeFragment) {
        val items = arrayListOf(
            TapEntryAction.SHOW_COMMENTS.titleId,
            TapEntryAction.SHOW_PAGE.titleId,
            TapEntryAction.SHOW_PAGE_IN_BROWSER.titleId
        )

        EntryMenuDialog.Builder(url, R.style.AlertDialogStyle)
            .setItems(items.map { context.getString(it) })
            .show(fragment.childFragmentManager, "entry_menu_dialog")
    }

    private fun showMenu(context: Context, entry: Entry, fragment: EntriesTabFragmentBase?) {
        val items = arrayListOf(
            TapEntryAction.SHOW_COMMENTS.titleId,
            TapEntryAction.SHOW_PAGE.titleId,
            TapEntryAction.SHOW_PAGE_IN_BROWSER.titleId
        )
        val additionalItems = fragment?.makeAdditionalMenuItems(entry)

        if (additionalItems != null) {
            items.addAll(additionalItems)
        }

        EntryMenuDialog.Builder(entry, R.style.AlertDialogStyle)
            .setItems(items.map { context.getString(it) })
            .show(fragment!!.childFragmentManager, "entry_menu_dialog")
    }
}
