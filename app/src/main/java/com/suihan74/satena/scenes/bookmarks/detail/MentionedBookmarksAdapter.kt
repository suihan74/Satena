package com.suihan74.satena.scenes.bookmarks.detail

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.suihan74.HatenaLib.Bookmark
import com.suihan74.HatenaLib.StarsEntry
import com.suihan74.satena.R
import com.suihan74.utilities.BookmarkCommentDecorator
import com.suihan74.utilities.FooterViewHolder
import com.suihan74.utilities.RecyclerState
import com.suihan74.utilities.RecyclerType

open class MentionedBookmarksAdapter(
    bookmarks: List<Bookmark>,
    private val starsMap : Map<String, StarsEntry>
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val states = RecyclerState.makeStatesWithFooter(bookmarks)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        when (RecyclerType.fromInt(viewType)) {
            RecyclerType.BODY -> {
                val inflate = LayoutInflater.from(parent.context).inflate(R.layout.listview_item_stars, parent, false)
                val holder =
                    ViewHolder(
                        inflate
                    )

                holder.itemView.setOnClickListener {
                    onItemClicked(holder.user ?: "")
                }

                holder.itemView.setOnLongClickListener {
                    onItemLongClicked(holder.user ?: "")
                }

                return holder
            }

            RecyclerType.FOOTER -> {
                val inflate = LayoutInflater.from(parent.context).inflate(R.layout.footer_recycler_view, parent, false)
                return FooterViewHolder(inflate)
            }

            else -> throw RuntimeException("invalid RecyclerType")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (RecyclerType.fromInt(holder.itemViewType)) {
            RecyclerType.BODY -> {
                holder as ViewHolder
                val bookmark = states[position].body as Bookmark
                holder.setBookmark(bookmark, starsMap)
            }

            else -> {}
        }
    }

    override fun getItemCount() = states.size

    override fun getItemViewType(position: Int): Int = states[position].type.int

    open fun onItemClicked(user: String) {}
    open fun onItemLongClicked(user: String) : Boolean = true

    class ViewHolder(private val view : View) : RecyclerView.ViewHolder(view) {
        private val userName    = view.findViewById<TextView>(R.id.star_user_name)!!
        private val userIcon    = view.findViewById<ImageView>(R.id.star_user_icon)!!
        private val comment     = view.findViewById<TextView>(R.id.star_comment)!!
        private val starsCount  = view.findViewById<TextView>(R.id.star_stars_count)!!

        var user : String? = null
            private set

        fun setBookmark(bookmark: Bookmark, starsMap: Map<String, StarsEntry>) {
            user = bookmark.user
            userName.text = user

            val analyzed = BookmarkCommentDecorator.convert(bookmark.comment)
            comment.text = analyzed.comment

            val starsEntry = starsMap[bookmark.user]
            if (starsEntry != null) {
                starsCount.text = view.context.getString(R.string.star_with_count, starsEntry.totalStarsCount)
            }
            else {
                starsCount.text = ""
            }

            Glide.with(view)
                .load(bookmark.userIconUrl)
                .into(userIcon)
        }
    }
}
