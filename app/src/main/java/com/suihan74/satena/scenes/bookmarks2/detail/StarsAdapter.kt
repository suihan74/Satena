package com.suihan74.satena.scenes.bookmarks2.detail

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.suihan74.HatenaLib.StarColor
import com.suihan74.satena.R
import com.suihan74.utilities.*
import kotlinx.android.synthetic.main.listview_item_stars.view.*


open class StarsAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var states = emptyList<RecyclerState<StarWithBookmark>>()

    open fun onItemClicked(item: StarWithBookmark) {}
    open fun onItemLongClicked(item: StarWithBookmark) = false

    override fun getItemCount() = states.size
    override fun getItemViewType(position: Int) = states[position].type.int

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        LayoutInflater.from(parent.context).let { inflater ->
            when (RecyclerType.fromInt(viewType)) {
                RecyclerType.BODY ->
                    ViewHolder(inflater.inflate(R.layout.listview_item_stars, parent, false)).apply {
                        itemView.setOnClickListener {
                            onItemClicked(
                                states[adapterPosition].body!!
                            )
                        }
                        itemView.setOnLongClickListener {
                            onItemLongClicked(
                                states[adapterPosition].body!!
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
                (holder as ViewHolder).body = states[position].body!!

            else -> {}
        }

    fun setStars(stars: List<StarWithBookmark>) {
        val newStates = RecyclerState.makeStatesWithFooter(stars)

        val diff = DiffUtil.calculateDiff(object : DiffUtil.Callback() {
            override fun getOldListSize() = states.size
            override fun getNewListSize() = newStates.size
            override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                val old = states[oldItemPosition]
                val new = newStates[newItemPosition]
                return old.type == new.type && old.body?.star?.user == new.body?.star?.user
            }
            override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                val old = states[oldItemPosition]
                val new = newStates[newItemPosition]
                return old.type == new.type &&
                        old.body?.star?.user == new.body?.star?.user &&
                        old.body?.star?.color == new.body?.star?.color &&
                        old.body?.star?.count == new.body?.star?.count &&
                        old.body?.star?.quote == new.body?.star?.quote &&
                        old.body?.bookmark?.comment == new.body?.bookmark?.comment
            }
        })
        states = newStates
        diff.dispatchUpdatesTo(this)
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

                    // ユーザーのブコメ
                    view.star_comment.text = bookmark.comment
                    view.star_comment.visibility = (!bookmark.comment.isBlank()).toVisibility()

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
                                for (i in 1..star.count) append(view.context.getString(R.string.star))
                            }

                    view.star_stars_count.setHtml("<font color=\"$starColor\">$starText</font>")
                }
            }
    }
}
