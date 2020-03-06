package com.suihan74.satena.scenes.entries

import com.suihan74.hatenaLib.Entry
import com.suihan74.hatenaLib.HatenaClient
import com.suihan74.satena.R
import com.suihan74.satena.TappedActionLauncher
import com.suihan74.satena.dialogs.EntryMenuDialog
import com.suihan74.satena.models.Category
import com.suihan74.satena.models.TapEntryAction
import com.suihan74.satena.scenes.entries.pages.SiteEntriesFragment
import com.suihan74.utilities.CoroutineScopeFragment

abstract class EntriesTabFragmentBase : CoroutineScopeFragment(), EntryMenuDialog.Listener {
    abstract val entriesAdapter: EntriesAdapter?

    fun makeAdditionalMenuItems(entry: Entry) : List<Int> {
        val items = ArrayList<Int>()

        if (this !is SiteEntriesFragment) {
            items.add(R.string.entry_action_show_entries)
        }
        if (HatenaClient.signedIn()) {
            if (entry.bookmarkedData == null) {
                items.add(R.string.entry_action_read_later)
            }
            else {
                if (entry.bookmarkedData.tags.contains("あとで読む")) {
                    items.add(R.string.entry_action_read)
                }
                else {
                    items.add(R.string.entry_action_read_later)
                }

                items.add(R.string.entry_action_delete_bookmark)
            }
        }
        items.add(R.string.entry_action_ignore)

        return items
    }

    private fun showEntries(entry: Entry) {
        val activity = activity as? EntriesActivity ?: throw RuntimeException("activity error")
        activity.refreshEntriesFragment(Category.Site, entry.rootUrl)
    }

    override fun onItemSelected(item: String, dialog: EntryMenuDialog) {
        val entry = dialog.entry

        when (item) {
            getString(R.string.entry_action_show_comments) ->
                TappedActionLauncher.launch(context!!, TapEntryAction.SHOW_COMMENTS, entry)

            getString(R.string.entry_action_show_page) ->
                TappedActionLauncher.launch(context!!, TapEntryAction.SHOW_PAGE, entry)

            getString(R.string.entry_action_show_page_in_browser) ->
                TappedActionLauncher.launch(context!!, TapEntryAction.SHOW_PAGE_IN_BROWSER, entry)

            getString(R.string.entry_action_show_entries) ->
                showEntries(entry)

            getString(R.string.entry_action_delete_bookmark) ->
                entriesAdapter?.deleteBookmark(entry)

            getString(R.string.entry_action_read) ->
                entriesAdapter?.bookmarkReadLaterEntry(entry)

            getString(R.string.entry_action_read_later) ->
                entriesAdapter?.addToReadLaterEntries(entry)

            getString(R.string.entry_action_ignore) ->
                entriesAdapter?.addEntryToIgnores(entry)
        }
    }
}
