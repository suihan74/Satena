package com.suihan74.satena.scenes.bookmarks2.detail

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.suihan74.HatenaLib.Star
import com.suihan74.HatenaLib.StarColor
import com.suihan74.HatenaLib.StarsEntry
import com.suihan74.satena.R
import com.suihan74.utilities.FooterViewHolder
import com.suihan74.utilities.RecyclerState
import com.suihan74.utilities.RecyclerType
import com.suihan74.utilities.setHtml
import kotlinx.android.synthetic.main.listview_item_stars.view.*

open class StarsToUserAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var states = emptyList<RecyclerState<Star>>()

    open fun onItemClicked(star: Star) {}
    open fun onItemLongClicked(star: Star) = false

    override fun getItemCount() = states.size
    override fun getItemViewType(position: Int) = states[position].type.int

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        LayoutInflater.from(parent.context).let { inflater ->
            when (RecyclerType.fromInt(viewType)) {
                RecyclerType.BODY ->
                    ViewHolder(inflater.inflate(R.layout.listview_item_stars, parent, false)).apply {
                        itemView.setOnClickListener {
                            val star = states[adapterPosition].body!!
                            onItemClicked(star)
                        }
                        itemView.setOnLongClickListener {
                            val star = states[adapterPosition].body!!
                            onItemLongClicked(star)
                        }
                    }

                RecyclerType.FOOTER ->
                    FooterViewHolder(inflater.inflate(R.layout.footer_recycler_view, parent, false))

                else -> throw RuntimeException("an invalid view type")
            }
        }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (RecyclerType.fromInt(holder.itemViewType)) {
            RecyclerType.BODY ->
                (holder as ViewHolder).body = states[position].body!!
        }
    }

    fun setStars(starsEntry: StarsEntry) {
        val newStates = RecyclerState.makeStatesWithFooter(starsEntry.allStars)

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
                        old.body?.color == new.body?.color &&
                        old.body?.count == new.body?.count &&
                        old.body?.quote == new.body?.quote
            }
        })
        states = newStates
        diff.dispatchUpdatesTo(this)
    }

    class ViewHolder(private val view: View) : RecyclerView.ViewHolder(view) {
        var body: Star? = null
            set(value) {
                field = value

                if (value != null) {
                    view.star_user_name.text = value.user
                    Glide.with(view.context)
                        .load(value.userIconUrl)
                        .into(view.star_user_icon)

                    val colorId = when (value.color) {
                        StarColor.Yellow -> R.color.starYellow
                        StarColor.Red -> R.color.starRed
                        StarColor.Green -> R.color.starGreen
                        StarColor.Blue -> R.color.starBlue
                        StarColor.Purple -> R.color.starPurple
                    }
                    val starColor = view.context.getColor(colorId)
                    val starText =
                        if (value.count > 9)
                            view.context.getString(R.string.star_with_count, value.count)
                        else
                            buildString {
                                for (i in 1..value.count) append(view.context.getString(R.string.star))
                            }

                    view.star_stars_count.setHtml("<font color=\"$starColor\">$starText</font>")
                }
            }
    }
}
