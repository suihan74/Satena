package com.suihan74.utilities

import android.net.Uri
import android.text.Html
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
    private val urlRegex = Regex("""http(s)?://([\w-]+\.)+[\w-]+(/[a-zA-Z0-9_\-+./!?%&=|^~#@*;:,<>()\[\]{}]*)?""")
    private val entryIdRegex = Regex("""(b:)?id:entry:([0-9]+)""")
    private val idRegex = Regex("""(b:)?id:(?!entry:)([a-zA-Z0-9_\-]+)""")
    private val tagRegex = Regex("""<.+>""")

    fun convert(str: String) : AnalyzedBookmarkComment {
        val ids = ArrayList<String>()
        val entryIds = ArrayList<Long>()
        val urls = ArrayList<String>()

        val idColor = "#2196F3"

        // 特定文字列のリンク化
        val html = str
            .replace(tagRegex) {
                // タグは無効化する
                Html.escapeHtml(it.value)
            }
            .replace(entryIdRegex) {
                // エントリID
                val eid = it.groups[2]!!.value
                if (eid.isNotEmpty()) {
                    entryIds.add(eid.toLong())
                }
                "<a href=\"$eid\"><font color=\"$idColor\">${it.value}</font></a>"
            }
            .replace(idRegex) {
                // ユーザーID
                val id = it.groups[2]!!.value
                if (id.isNotEmpty()) {
                    ids.add(id)
                }
                "<font color=\"$idColor\">${it.value}</font>"
            }
            .replace(urlRegex) {
                // URL
                val url = Uri.decode(it.value)
                urls.add(url)
                "<a href=\"$url\">$url</a>"
            }

        return AnalyzedBookmarkComment(
            makeSpannedFromHtml(html),
            ids.distinct(),
            entryIds.distinct(),
            urls.distinct())
    }

    fun makeClickableTagsText(tags: List<String>, onItemClicked: (String)->Unit) : Spanned {
        val sb = SpannableStringBuilder()
        val sep = ", "
        var spanStart = 0
        val lastIdx = tags.lastIndex
        tags.forEachIndexed { idx, tag ->
            sb.append(tag)
            if (idx < lastIdx) {
                sb.append(sep)
            }
            sb.setSpan(object : ClickableSpan() {
                override fun onClick(widget: View) = onItemClicked(tag)
            }, spanStart, spanStart + tag.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            spanStart += tag.length + sep.length
        }
        return sb
    }
}
