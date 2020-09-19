package com.suihan74.satena.scenes.bookmarks2.detail

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.suihan74.hatenaLib.StarColor
import com.suihan74.satena.R
import com.suihan74.utilities.FooterViewHolder
import com.suihan74.utilities.RecyclerState
import com.suihan74.utilities.RecyclerType
import com.suihan74.utilities.extensions.setHtml
import com.suihan74.utilities.extensions.toVisibility
import kotlinx.android.synthetic.main.listview_item_stars.view.*

open class StarsAdapter : ListAdapter<RecyclerState<StarWithBookmark>, RecyclerView.ViewHolder>(DiffCallback()) {
    private class DiffCallback : DiffUtil.ItemCallback<RecyclerState<StarWithBookmark>>() {
        override fun areItemsTheSame(
            oldItem: RecyclerState<StarWithBookmark>,
            newItem: RecyclerState<StarWithBookmark>
        ) = oldItem.type == newItem.type && oldItem.body?.star?.user == newItem.body?.star?.user

        override fun areContentsTheSame(
            oldItem: RecyclerState<StarWithBookmark>,
            newItem: RecyclerState<StarWithBookmark>
        ) = oldItem.type == newItem.type && oldItem.body == newItem.body
    }

    open fun onItemClicked(item: StarWithBookmark) {}
    open fun onItemLongClicked(item: StarWithBookmark) = false

    override fun getItemCount() = currentList.size
    override fun getItemViewType(position: Int) = currentList[position].type.int

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        LayoutInflater.from(parent.context).let { inflater ->
            when (RecyclerType.fromInt(viewType)) {
                RecyclerType.BODY ->
                    ViewHolder(inflater.inflate(R.layout.listview_item_stars, parent, false)).apply {
                        itemView.setOnClickListener {
                            onItemClicked(
                                currentList[adapterPosition].body!!
                            )
                        }
                        itemView.setOnLongClickListener {
                            onItemLongClicked(
                                currentList[adapterPosition].body!!
                            )
                        }
                    }

                RecyclerType.FOOTER ->
                    FooterViewHolder(inflater.inflate(R.layout.footer_recycler_view, parent, false))

                else -> throw RuntimeException("an invalid view type")
            }
        }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) =
        when (RecyclerType.fromInt(holder.itemViewType)) {
            RecyclerType.BODY ->
                (holder as ViewHolder).body = currentList[position].body!!

            else -> {}
        }

    fun setStars(stars: List<StarWithBookmark>) {
        if (currentList.isEmpty() && stars.isEmpty()) return
        submitList(RecyclerState.makeStatesWithFooter(stars))
    }

    class ViewHolder(private val view: View) : RecyclerView.ViewHolder(view) {
        var body: StarWithBookmark? = null
            set(value) {
                field = value

                if (value != null) {
                    val star = value.star
                    val bookmark = value.bookmark

                    // IDとアイコン
                    view.star_user_name.text = bookmark.user
                    Glide.with(view.context).run {
                        clear(view.star_user_icon)
                        load(bookmark.userIconUrl)
                            .into(view.star_user_icon)
                    }

                    // ユーザーのブコメと引用文
                    val quote = star?.quote
                    val comment =
                        when {
                            quote.isNullOrBlank() -> bookmark.comment
                            bookmark.comment.isBlank() -> quote
                            else -> "\"$quote\"\n${bookmark.comment}"
                        }
                    view.star_comment.text = comment
                    view.star_comment.visibility = (!comment.isBlank()).toVisibility()

                    // 非表示ブクマかどうかのマークを表示
                    view.muted_mark.visibility = (value.state == StarWithBookmark.DisplayState.COVER).toVisibility()

                    if (star == null) {
                        return
                    }

                    // ユーザーが付けたスター
                    val colorId = when (star.color) {
                        StarColor.Yellow -> R.color.starYellow
                        StarColor.Red -> R.color.starRed
                        StarColor.Green -> R.color.starGreen
                        StarColor.Blue -> R.color.starBlue
                        StarColor.Purple -> R.color.starPurple
                    }
                    val starColor = view.context.getColor(colorId)
                    val starText =
                        if (star.count > 9)
                            view.context.getString(R.string.star_with_count, star.count)
                        else
                            buildString {
                                repeat (star.count) { append(view.context.getString(R.string.star)) }
                            }

                    view.star_stars_count.setHtml("<font color=\"$starColor\">$starText</font>")
                }
            }
    }
}
