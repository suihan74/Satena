package com.suihan74.utilities

import android.net.Uri
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.ClickableSpan
import android.view.View

data class AnalyzedBookmarkComment(
    val comment : Spanned,
    val ids : List<String>,
    val entryIds: List<Long>,
    val urls : List<String>
)

object BookmarkCommentDecorator {
    private val mUrlRegex = Regex("""http(s)?://([\w-]+\.)+[\w-]+(/[\w-./?%&=#~@:]*)?""")
    private val mEntryIdRegex = Regex("""(b:)?id:entry:([0-9]+)""")
    private val mIdRegex = Regex("""(b:)?id:(?!entry:)([a-zA-Z0-9_\-]+)""")

    fun convert(str: String) : AnalyzedBookmarkComment {
        val ids = ArrayList<String>()
        val entryIds = ArrayList<Long>()
        val urls = ArrayList<String>()

        val idColor = "#2196F3"

        // 特定文字列のリンク化
        val html = str
            .replace(mEntryIdRegex) {
                // エントリID
                val eid = it.groups[2]!!.value
                if (eid.isNotEmpty()) {
                    entryIds.add(eid.toLong())
                }
                "<a href=\"$eid\"><font color=\"$idColor\">${it.value}</font></a>"
            }
            .replace(mIdRegex) {
                // ユーザーID
                val id = it.groups[2]!!.value
                if (id.isNotEmpty()) {
                    ids.add(id)
                }
                "<font color=\"$idColor\">${it.value}</font>"
            }
            .replace(mUrlRegex) {
                // URL
                val url = Uri.decode(it.value)
                urls.add(url)
                "<a href=\"$url\">$url</a>"
            }

        return AnalyzedBookmarkComment(makeSpannedfromHtml(html), ids, entryIds, urls)
    }

    fun makeClickableTagsText(tags: List<String>, onItemClicked: (String)->Unit) : Spanned {
        val sb = SpannableStringBuilder()
        val sep = ", "
        var spanStart = 0
        for (tag in tags) {
            sb.append(tag)
            sb.append(sep)
            sb.setSpan(object : ClickableSpan() {
                override fun onClick(widget: View) = onItemClicked(tag)
            }, spanStart, spanStart + tag.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            spanStart += tag.length + sep.length
        }
        return sb
    }
}
