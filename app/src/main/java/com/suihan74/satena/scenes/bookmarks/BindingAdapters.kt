package com.suihan74.satena.scenes.bookmarks

import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.view.View
import android.webkit.URLUtil
import android.widget.TextView
import androidx.core.text.buildSpannedString
import androidx.databinding.BindingAdapter
import com.suihan74.hatenaLib.Bookmark
import com.suihan74.satena.R
import com.suihan74.utilities.BookmarkCommentDecorator
import com.suihan74.utilities.Listener
import com.suihan74.utilities.MutableLinkMovementMethod2
import com.suihan74.utilities.extensions.append
import com.suihan74.utilities.extensions.appendDrawable

/**
 * ブクマ関係の汎用的なBindingAdapter
 */
object BindingAdapters {
    /**
     * コメント中のリンクをクリック可能文字列としてバインドする
     */
    @JvmStatic
    @BindingAdapter("comment", "onLinkClick", "onLinkLongClick", "onEntryIdClick", "onEntryIdLongClick")
    fun setBookmarkComment(
        textView: TextView,
        bookmark: Bookmark?,
        onLinkClick: Listener<String>?,
        onLinkLongClick : Listener<String>?,
        onEntryIdClick: Listener<Long>?,
        onEntryIdLongClick: Listener<Long>?
    ) {
        val comment = bookmark?.comment
        if (comment.isNullOrBlank()) {
            textView.text = ""
            return
        }
        val analyzed = BookmarkCommentDecorator.convert(comment)
        textView.text = analyzed.comment
        textView.movementMethod = object : MutableLinkMovementMethod2() {
            private fun detectEntryId(link: String) : Long? =
                analyzed.entryIds
                    .firstOrNull { eid -> link.contains(eid.toString()) }

            override fun onSinglePressed(link: String) {
                if (URLUtil.isNetworkUrl(link)) {
                    onLinkClick?.invoke(link)
                }
                else detectEntryId(link)?.let { eid ->
                    onEntryIdClick?.invoke(eid)
                }
            }

            override fun onLongPressed(link: String) {
                if (URLUtil.isNetworkUrl(link)) {
                    onLinkLongClick?.invoke(link)
                }
                else detectEntryId(link)?.let { eid ->
                    onEntryIdLongClick?.invoke(eid)
                }
            }
        }
    }

    /**
     * ブクマに含まれるタグをクリック可能文字列としてバインドする
     */
    @JvmStatic
    @BindingAdapter("tagLinks", "onTagClick")
    fun setBookmarkTagLinks(textView: TextView, bookmark: Bookmark?, onClick : Listener<String>?) {
        val tags = bookmark?.tags
        if (tags.isNullOrEmpty()) {
            textView.text = ""
            textView.visibility = View.GONE
            return
        }

        textView.text = buildSpannedString {
            appendDrawable(textView, R.drawable.ic_tag)
            val lastIndex = tags.lastIndex
            tags.forEachIndexed { index, tag ->
                append(
                    tag,
                    object : ClickableSpan() {
                        override fun onClick(widget: View) {
                            onClick?.invoke(tag)
                        }
                    }
                )
                if (index < lastIndex) {
                    append(", ")
                }
            }
        }
        textView.movementMethod = LinkMovementMethod.getInstance()
        textView.visibility = View.VISIBLE
    }
}
