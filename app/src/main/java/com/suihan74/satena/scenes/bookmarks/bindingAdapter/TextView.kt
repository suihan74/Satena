package com.suihan74.satena.scenes.bookmarks.bindingAdapter

import android.content.res.ColorStateList
import android.net.Uri
import android.text.SpannableStringBuilder
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.text.style.TextAppearanceSpan
import android.view.View
import android.webkit.URLUtil
import android.widget.TextView
import androidx.core.text.buildSpannedString
import androidx.databinding.BindingAdapter
import com.suihan74.hatenaLib.Bookmark
import com.suihan74.hatenaLib.StarColor
import com.suihan74.hatenaLib.StarsEntry
import com.suihan74.satena.R
import com.suihan74.satena.models.userTag.UserAndTags
import com.suihan74.satena.scenes.bookmarks.detail.tabs.StarRelationsAdapter
import com.suihan74.utilities.BookmarkCommentDecorator
import com.suihan74.utilities.Listener
import com.suihan74.utilities.MutableLinkMovementMethod2
import com.suihan74.utilities.extensions.*
import org.threeten.bp.LocalDateTime
import org.threeten.bp.format.DateTimeFormatter

object TextViewBindingAdapters {
    /**
     * コメント中のリンクをクリック可能文字列としてバインドする
     */
    @JvmStatic
    @BindingAdapter("comment", "onLinkClick", "onLinkLongClick", "onEntryIdClick", "onEntryIdLongClick")
    fun bindBookmarkComment(
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
    fun bindBookmarkTagLinks(textView: TextView, bookmark: Bookmark?, onClick : Listener<String>?) {
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

    /**
     * スターリストのコメント+引用文表示
     */
    @JvmStatic
    @BindingAdapter("starRelationComment")
    fun bindStarRelationText(textView: TextView, item: StarRelationsAdapter.Item?) {
        textView.text = buildString {
            item?.star?.quote?.let {
                if (it.isNotBlank()) append("\"${Uri.decode(it)}\"\n")
            }
            item?.comment?.let {
                append(Uri.decode(it))
            }
        }
    }

    /**
     * スター数の表示
     *
     * @param starsEntry スター情報
     * @param timestamp (ブコメリスト項目では使用)投稿時刻表示
     */
    @JvmStatic
    @BindingAdapter(value = ["starsEntry", "timestamp"], requireAll = false)
    fun bindStarsEntry(textView: TextView, starsEntry: StarsEntry?, timestamp: LocalDateTime?) {
        val builder = SpannableStringBuilder()
        val stars = starsEntry?.allStars

        timestamp?.let {
            val formatter = DateTimeFormatter.ofPattern("uuuu/MM/dd HH:mm")
            builder.append(
                it.toSystemZonedDateTime("Asia/Tokyo").format(formatter)
            )
            if (!stars.isNullOrEmpty()) {
                builder.append("\u2002\u2002") // margin
            }
        }

        if (!stars.isNullOrEmpty()) {
            stars.let { star ->
                val yellowStarCount = star.filter { it.color == StarColor.Yellow }.sumBy { it.count }
                val redStarCount = star.filter { it.color == StarColor.Red }.sumBy { it.count }
                val greenStarCount = star.filter { it.color == StarColor.Green }.sumBy { it.count }
                val blueStarCount = star.filter { it.color == StarColor.Blue }.sumBy { it.count }
                val purpleStarCount = star.filter { it.color == StarColor.Purple }.sumBy { it.count }

                val context = textView.context
                builder.appendStarSpan(purpleStarCount, context, R.style.StarSpan_Purple)
                builder.appendStarSpan(blueStarCount, context, R.style.StarSpan_Blue)
                builder.appendStarSpan(redStarCount, context, R.style.StarSpan_Red)
                builder.appendStarSpan(greenStarCount, context, R.style.StarSpan_Green)
                builder.appendStarSpan(yellowStarCount, context, R.style.StarSpan_Yellow)
            }
        }

        textView.text = builder
    }

    /**
     * ユーザータグの列挙
     */
    @JvmStatic
    @BindingAdapter(value = ["userName", "userTags", "tagsSize"], requireAll = false)
    fun bindUserTagsText(textView: TextView, user: String?, userTags: UserAndTags?, tagsSizePx: Int?) {
        textView.text = buildSpannedString {
            append(user)

            val tagsText = userTags?.tags?.joinToString(",") { it.name }
            if (!tagsText.isNullOrBlank()) {
                val color = textView.context.getThemeColor(R.attr.tagTextColor)
                val sizePx = tagsSizePx ?: (textView.lineHeight * .8).toInt()
                append("\u2002")
                appendDrawable(
                    textView = textView,
                    resId = R.drawable.ic_user_tag,
                    color = color,
                    sizePx = sizePx
                )
                append(tagsText, TextAppearanceSpan(null, 0, sizePx, ColorStateList.valueOf(color), null))
            }
        }
    }
}
