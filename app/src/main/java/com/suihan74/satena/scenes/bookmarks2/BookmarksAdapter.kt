package com.suihan74.satena.scenes.bookmarks2

import android.text.SpannableString
import android.text.style.ImageSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.suihan74.HatenaLib.Bookmark
import com.suihan74.HatenaLib.BookmarksEntry
import com.suihan74.HatenaLib.StarColor
import com.suihan74.satena.R
import com.suihan74.satena.models.userTag.Tag
import com.suihan74.satena.models.userTag.UserAndTags
import com.suihan74.utilities.*
import kotlinx.android.synthetic.main.listview_item_bookmarks.view.*
import org.threeten.bp.format.DateTimeFormatter

fun <T> List<T>.contentsEquals(other: List<T>) =
    this.size == other.size && this.mapIndexed { index, _ -> this[index] == other[index] }.all { it }

open class BookmarksAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    data class Entity (
        val bookmark: Bookmark,
        val analyzedComment: AnalyzedBookmarkComment,
        val isIgnored: Boolean,
        val mentions: List<Bookmark>
    )

    private var states = emptyList<RecyclerState<Entity>>()
    private var tags = emptyList<List<Tag>?>()

    open fun onItemClicked(bookmark: Bookmark) {}
    open fun onItemLongClicked(bookmark: Bookmark) = false

    override fun getItemCount() = states.size

    override fun getItemViewType(position: Int) =
        states[position].type.int

    /**
     * 指定ブクマの位置を取得する
     * @return 対象が存在するとき ---> そのインデックス
     *
     * 対象が存在しないとき ---> -1
     */
    fun getPosition(bookmark: Bookmark) =
        states.indexOfFirst { it.body?.bookmark?.user == bookmark.user }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        LayoutInflater.from(parent.context).let { inflater ->
            when (RecyclerType.fromInt(viewType)) {
                RecyclerType.BODY ->
                    ViewHolder(
                        inflater.inflate(R.layout.listview_item_bookmarks, parent, false)
                    ).apply {
                        itemView.setOnClickListener {
                            val bookmark = states[adapterPosition].body!!.bookmark
                            onItemClicked(bookmark)
                        }
                        itemView.setOnLongClickListener {
                            val bookmark = states[adapterPosition].body!!.bookmark
                            onItemLongClicked(bookmark)
                        }
                    }

                RecyclerType.FOOTER ->
                    LoadableFooterViewHolder(
                        inflater.inflate(R.layout.footer_recycler_view_loadable, parent, false)
                    )

                else -> throw RuntimeException("an invalid list item")
            }
        }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) =
        when (RecyclerType.fromInt(holder.itemViewType)) {
            RecyclerType.BODY -> {
                (holder as ViewHolder).run {
                    val entity = states[position].body!!
                    bookmark = entity
                    userTags = tags[position]
                }
            }

            else -> Unit
        }

    fun setBookmarks(
        bookmarks: List<Bookmark>,
        bookmarksEntry: BookmarksEntry,
        taggedUsers: List<UserAndTags>,
        ignoredUsers: List<String>
    ) {
        val newStates = RecyclerState.makeStatesWithFooter(bookmarks.map {
            val analyzedComment = BookmarkCommentDecorator.convert(it.comment)
            Entity(
                it,
                analyzedComment,
                ignoredUsers.contains(it.user),
                analyzedComment.ids.mapNotNull { called -> bookmarksEntry.bookmarks.firstOrNull { it.user == called } })
        })
        val newTags = newStates.map { taggedUsers.firstOrNull { t -> t.user.name == it.body?.bookmark?.user }?.tags }

        val diff = DiffUtil.calculateDiff(object : DiffUtil.Callback() {
            override fun getOldListSize() = states.size
            override fun getNewListSize() = newStates.size
            override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                val old = states[oldItemPosition]
                val new = newStates[newItemPosition]
                return old.type == new.type && old.body?.bookmark?.user == new.body?.bookmark?.user
            }
            override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                val old = states[oldItemPosition]
                val new = newStates[newItemPosition]

                val oldTag = tags[oldItemPosition]
                val newTag = newTags[newItemPosition]

                val oldMentions = old.body?.mentions
                val newMentions = new.body?.mentions

                return old.type == new.type &&
                        old.body?.bookmark?.user == new.body?.bookmark?.user &&
                        old.body?.bookmark?.comment == new.body?.bookmark?.comment &&
                        old.body?.bookmark?.starCount?.contentsEquals(new.body?.bookmark?.starCount!!) == true &&
                        old.body?.isIgnored == new.body?.isIgnored &&
                        (oldMentions == null && newMentions == null || oldMentions != null && newMentions != null && oldMentions.contentsEquals(newMentions)) &&
                        (oldTag == null && newTag == null || oldTag != null && newTag != null && oldTag.contentsEquals(newTag))
            }
        })
        states = newStates
        tags = newTags

        diff.dispatchUpdatesTo(this)
    }

    /** ブクマリストアイテム */
    class ViewHolder(private val view: View) : RecyclerView.ViewHolder(view) {
        var userTags: List<Tag>? = null
            set(value) {
                field = value
                if (value.isNullOrEmpty()) {
                    view.user_tags.visibility = View.GONE
                }
                else {
                    view.user_tags.apply {
                        val icon = resources.getDrawable(R.drawable.ic_user_tag, null).apply {
                            val size = textSize.toInt()
                            setBounds(0, 0, size, size)
                            setTint(resources.getColor(R.color.tagColor, null))
                        }
                        setCompoundDrawablesRelative(icon, null, null, null)

                        text = value.joinToString(", ") { it.name }
                        visibility = View.VISIBLE
                    }
                }
            }

        var bookmark: Entity? = null
            set(value) {
                field = value
                value?.let {
                    init(value)
                }
            }

        private fun init(entity: Entity) {
            val bookmark = entity.bookmark
            Glide.with(view.context).run {
                clear(view.bookmark_user_icon)
                load(bookmark.userIconUrl)
                    .into(view.bookmark_user_icon)
            }
            view.bookmark_user_name.text = bookmark.user

            view.bookmark_comment.apply {
                text = entity.analyzedComment.comment
                visibility = (text.isNotEmpty()).toVisibility(View.GONE)
            }

            view.ignored_user_mark.visibility = entity.isIgnored.toVisibility(View.GONE)

            // タグ
            view.bookmark_tags.apply {
                if (bookmark.tags.isEmpty()) {
                    visibility = View.GONE
                }
                else {
                    visibility = View.VISIBLE
                    val tagsText = bookmark.tags.joinToString(", ")
                    text = SpannableString("_$tagsText").apply {
                        val icon = resources.getDrawable(R.drawable.ic_tag, null).apply {
                            setBounds(0, 0, lineHeight, lineHeight)
                            setTint(resources.getColor(R.color.tagColor, null))
                        }
                        setSpan(ImageSpan(icon), 0, 1, SpannableString.SPAN_EXCLUSIVE_EXCLUSIVE)
                    }
                }
            }

            // タイムスタンプ & スター
            val builder = StringBuilder()
            val formatter = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm")
            builder.append(bookmark.timestamp.format(formatter))
            builder.append("　")

            if (!bookmark.starCount.isNullOrEmpty()) {
                bookmark.starCount.let { stars ->
                    val yellowStarCount = stars.firstOrNull { it.color == StarColor.Yellow }?.count ?: 0
                    val redStarCount = stars.firstOrNull { it.color == StarColor.Red }?.count ?: 0
                    val greenStarCount = stars.firstOrNull { it.color == StarColor.Green }?.count ?: 0
                    val blueStarCount = stars.firstOrNull { it.color == StarColor.Blue }?.count ?: 0
                    val purpleStarCount = stars.firstOrNull { it.color == StarColor.Purple }?.count ?: 0

                    appendStarText(builder, purpleStarCount, view.context, R.color.starPurple)
                    appendStarText(builder, blueStarCount, view.context, R.color.starBlue)
                    appendStarText(builder, redStarCount, view.context, R.color.starRed)
                    appendStarText(builder, greenStarCount, view.context, R.color.starGreen)
                    appendStarText(builder, yellowStarCount, view.context, R.color.starYellow)
                }
            }
            view.bookmark_timestamp.setHtml(builder.toString())

            // 言及先リスト
            view.bookmark_mentions.apply {
                val mentions = entity.mentions
                if (mentions.isEmpty()) {
                    visibility = View.GONE
                    adapter = null
                }
                else {
                    visibility = View.VISIBLE
                    layoutManager = LinearLayoutManager(context)
                    adapter = object : MentionsAdapter(mentions) {
                        override fun onItemClicked(item: Bookmark) {
//                            this@ViewHolder.adapter.onItemClicked(item)
                        }

                        override fun onItemLongClicked(item: Bookmark): Boolean {
//                            this@ViewHolder.adapter.onItemLongClicked(item)
                            return true
                        }
                    }

                    repeat(itemDecorationCount) {
                        removeItemDecorationAt(0)
                    }

                    val dividerItemDecoration = DividerItemDecorator(
                        ContextCompat.getDrawable(context, R.drawable.recycler_view_item_divider)!!)
                    addItemDecoration(dividerItemDecoration)
                }
            }
        }
    }
}
