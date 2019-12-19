package com.suihan74.satena.scenes.bookmarks2

import android.text.SpannableString
import android.text.style.ImageSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.suihan74.HatenaLib.Bookmark
import com.suihan74.HatenaLib.StarColor
import com.suihan74.satena.R
import com.suihan74.utilities.*
import kotlinx.android.synthetic.main.listview_item_bookmarks.view.*
import org.threeten.bp.format.DateTimeFormatter

fun <T> List<T>.contentsEquals(other: List<T>) =
    this.size == other.size && this.mapIndexed { index, _ -> this[index] == other[index] }.all { it }

open class BookmarksAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var states = emptyList<RecyclerState<Bookmark>>()

    open fun onItemClicked(bookmark: Bookmark) {}
    open fun onItemLongClicked(bookmark: Bookmark) = false

    override fun getItemCount() = states.size

    override fun getItemViewType(position: Int) =
        states[position].type.int

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        LayoutInflater.from(parent.context).let { inflater ->
            when (RecyclerType.fromInt(viewType)) {
                RecyclerType.BODY ->
                    ViewHolder(
                        inflater.inflate(R.layout.listview_item_bookmarks, parent, false)
                    ).apply {
                        itemView.setOnClickListener {
                            val bookmark = states[adapterPosition].body!!
                            onItemClicked(bookmark)
                        }
                        itemView.setOnLongClickListener {
                            val bookmark = states[adapterPosition].body!!
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
                    bookmark = states[position].body!!
                }
            }

            else -> Unit
        }

    fun setBookmarks(bookmarks: List<Bookmark>) {
        val newStates = RecyclerState.makeStatesWithFooter(bookmarks)

        val diff = DiffUtil.calculateDiff(object : DiffUtil.Callback() {
            override fun getOldListSize() = states.size
            override fun getNewListSize() = newStates.size
            override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                val old = states[oldItemPosition]
                val new = newStates[newItemPosition]
                return old.type == new.type && old.body?.user == new.body?.user
            }
            override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                val old = states[oldItemPosition]
                val new = newStates[newItemPosition]
                return old.type == new.type &&
                        old.body?.user == new.body?.user &&
                        old.body?.comment == new.body?.comment &&
                        old.body?.starCount?.contentsEquals(new.body?.starCount!!) == true
            }
        })

        states = newStates
        diff.dispatchUpdatesTo(this)
    }

    /** ブクマリストアイテム */
    class ViewHolder(private val view: View) : RecyclerView.ViewHolder(view) {
        var bookmark: Bookmark? = null
            set(value) {
                field = value
                if (value != null) {
                    initView(value)
                }
            }

        private fun initView(bookmark: Bookmark) {
            Glide.with(view.context)
                .load(bookmark.userIconUrl)
                .into(view.bookmark_user_icon)
            view.bookmark_user_name.text = bookmark.user

            val analyzed = BookmarkCommentDecorator.convert(bookmark.comment)
            view.bookmark_comment.apply {
                text = analyzed.comment
                visibility = (text.isNotEmpty()).toVisibility(View.GONE)
            }

            view.ignored_user_mark.visibility = View.GONE

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
//                if (mentions.isEmpty()) {
                    visibility = View.GONE
                    adapter = null
/*
                }
                else {
                    visibility = View.VISIBLE
                    layoutManager = LinearLayoutManager(context)
                    adapter = object : MentionsAdapter(mentions) {
                        override fun onItemClicked(item: Bookmark) {
                            this@ViewHolder.adapter.onItemClicked(item)
                        }

                        override fun onItemLongClicked(item: Bookmark): Boolean {
                            this@ViewHolder.adapter.onItemLongClicked(item)
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
 */
            }
        }
    }
}
