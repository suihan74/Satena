package com.suihan74.satena.scenes.bookmarks2

import android.text.SpannableString
import android.text.style.ImageSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.suihan74.HatenaLib.Bookmark
import com.suihan74.HatenaLib.BookmarksEntry
import com.suihan74.HatenaLib.StarColor
import com.suihan74.satena.R
import com.suihan74.satena.TappedActionLauncher
import com.suihan74.satena.models.PreferenceKey
import com.suihan74.satena.models.TapEntryAction
import com.suihan74.satena.models.userTag.Tag
import com.suihan74.satena.models.userTag.UserAndTags
import com.suihan74.utilities.*
import kotlinx.android.synthetic.main.listview_item_bookmarks.view.*
import org.threeten.bp.format.DateTimeFormatter

fun <T> List<T>?.contentsEquals(other: List<T>?) =
    if (this == null && other == null)
        true
    else if (other == null)
        false
    else
        this!!.size == other.size && this.mapIndexed { index, _ -> this[index] == other[index] }.all { it }

open class BookmarksAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    data class Entity (
        val bookmark: Bookmark,
        val analyzedComment: AnalyzedBookmarkComment,
        val isIgnored: Boolean,
        val mentions: List<Bookmark>,
        val userTags: List<Tag>
    ) {
        override fun equals(other: Any?): Boolean {
            if (other !is Entity) return false

            return bookmark.user == other.bookmark.user &&
                    bookmark.comment == other.bookmark.comment &&
                    bookmark.starCount.contentsEquals(other.bookmark.starCount) &&
                    isIgnored == other.isIgnored &&
                    mentions.contentsEquals(other.mentions) &&
                    userTags.contentsEquals(other.userTags)
        }
    }

    /** 表示項目リスト */
    private var states = emptyList<RecyclerState<Entity>>()
    private var loadableFooter: LoadableFooterViewHolder? = null

    open fun onItemClicked(bookmark: Bookmark) {}
    open fun onItemLongClicked(bookmark: Bookmark) = false

    /** コメント中のリンクをタップしたときの処理 */
    open fun onLinkClicked(url: String) {}
    /** コメント中のリンクをロングタップしたときの処理 */
    open fun onLinkLongClicked(url: String) {}

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
                        inflater.inflate(R.layout.listview_item_bookmarks, parent, false),
                        this
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
                    ).also {
                        loadableFooter = it
                    }

                else -> throw RuntimeException("an invalid list item")
            }
        }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) =
        when (RecyclerType.fromInt(holder.itemViewType)) {
            RecyclerType.BODY -> {
                (holder as ViewHolder).run {
                    val entity = states[position].body!!
                    bookmark = entity
                }
            }

            else -> Unit
        }

    /** フッタのローディングアニメを表示する */
    fun startLoading() {
        loadableFooter?.showProgressBar()
    }

    /** フッタのローディングアニメを隠す */
    fun stopLoading() {
        loadableFooter?.hideProgressBar()
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
                bookmark = it,
                analyzedComment = analyzedComment,
                isIgnored = ignoredUsers.contains(it.user),
                mentions = analyzedComment.ids.mapNotNull { called ->
                    bookmarksEntry.bookmarks.firstOrNull { b -> b.user == called }
                },
                userTags = taggedUsers.firstOrNull { t -> t.user.name == it.user }?.tags ?: emptyList()
            )
        })

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
                return old.type == new.type && old.body?.equals(new.body) == true
            }
        })
        states = newStates

        diff.dispatchUpdatesTo(this)
    }

    /** ブクマリストアイテム */
    class ViewHolder(
        private val view: View,
        private val bookmarksAdapter: BookmarksAdapter
    ) : RecyclerView.ViewHolder(view) {
        var bookmark: Entity? = null
            set(value) {
                field = value
                value?.let {
                    init(value)
                }
            }

        private fun init(entity: Entity) {
            val bookmark = entity.bookmark
            val userTags = entity.userTags

            Glide.with(view.context).run {
                clear(view.bookmark_user_icon)
                load(bookmark.userIconUrl)
                    .into(view.bookmark_user_icon)
            }
            view.bookmark_user_name.text = bookmark.user

            view.bookmark_comment.apply {
                text = entity.analyzedComment.comment
                visibility = (text.isNotEmpty()).toVisibility(View.GONE)

                val linkMovementMethod = object : MutableLinkMovementMethod() {
                    override fun onSinglePressed(link: String) {
                        if (link.startsWith("http")) {
                            bookmarksAdapter.onLinkClicked(link)
                        }
                        else {
                            val eid = entity.analyzedComment.entryIds.firstOrNull { link.contains(it.toString()) }
                            if (eid != null) {
                                // TODO: EntryIdをクリックしたらブクマActivityを追加する
                            }
                        }
                    }

                    override fun onLongPressed(link: String) {
                        if (link.startsWith("http")) {
                            bookmarksAdapter.onLinkLongClicked(link)
                        }
                    }
                }

                setOnTouchListener { view, event ->
                    val textView = view as TextView
                    return@setOnTouchListener linkMovementMethod.onTouchEvent(
                        textView,
                        SpannableString(textView.text),
                        event)
                }
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

            // ユーザータグ
            if (userTags.isNullOrEmpty()) {
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

                    text = userTags.joinToString(", ") { it.name }
                    visibility = View.VISIBLE
                }
            }

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
                        override fun onItemClicked(item: Bookmark) =
                            bookmarksAdapter.onItemClicked(item)

                        override fun onItemLongClicked(item: Bookmark) =
                            bookmarksAdapter.onItemLongClicked(item)
                    }

                    repeat(itemDecorationCount) {
                        removeItemDecorationAt(0)
                    }

                    val dividerItemDecoration = DividerItemDecorator(
                        ContextCompat.getDrawable(context, R.drawable.recycler_view_item_divider)!!
                    )
                    addItemDecoration(dividerItemDecoration)
                }
            }
        }
    }
}
