package com.suihan74.satena.scenes.bookmarks

import android.content.Context
import android.content.Intent
import androidx.activity.result.contract.ActivityResultContract
import com.suihan74.hatenaLib.Entry
import com.suihan74.utilities.extensions.putObjectExtra

class BookmarksActivityContract : ActivityResultContract<BookmarksActivityContract.Args, Unit>() {
    data class Args(
        val entry: Entry? = null,
        val url: String? = null,
        val eid: Long? = null,
        val targetUser: String? = null
    )

    override fun createIntent(context: Context, input: Args?) =
        Intent(context, BookmarksActivity::class.java).apply {
            input?.let { args ->
                args.entry?.let { entry -> putObjectExtra(BookmarksActivity.EXTRA_ENTRY, entry) }
                args.url?.let { url -> putExtra(BookmarksActivity.EXTRA_ENTRY_URL, url) }
                args.eid?.let { eid -> putExtra(BookmarksActivity.EXTRA_ENTRY_ID, eid) }
                args.targetUser?.let { user -> putExtra(BookmarksActivity.EXTRA_TARGET_USER, user) }
            }
        }

    override fun parseResult(resultCode: Int, intent: Intent?) {}
}
