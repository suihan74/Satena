package com.suihan74.satena.scenes.bookmarks2

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.suihan74.hatenaLib.Bookmark
import com.suihan74.hatenaLib.StarColor
import com.suihan74.satena.R
import com.suihan74.utilities.appendStarText
import com.suihan74.utilities.setHtml
import com.suihan74.utilities.toVisibility
import kotlinx.android.synthetic.main.listview_item_comment.view.*
import org.threeten.bp.format.DateTimeFormatter

open class MentionsAdapter (
    private val comments: List<Bookmark>
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    open fun onItemClicked(item: Bookmark) {}
    open fun onItemLongClicked(item: Bookmark) : Boolean = true

    override fun getItemCount(): Int = comments.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflate = LayoutInflater.from(parent.context).inflate(R.layout.listview_item_comment, parent, false)
        val holder = ViewHolder(inflate)

        holder.itemView.setOnClickListener {
            onItemClicked(holder.value!!)
        }

        holder.itemView.setOnLongClickListener {
            onItemLongClicked(holder.value!!)
        }

        return holder
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        (holder as ViewHolder).value = comments[position]
    }

    class ViewHolder(val view: View) : RecyclerView.ViewHolder(view) {
        var value : Bookmark? = null
            internal set(value) {
                field = value
                value ?: return

                view.user_name.text = value.user

                val commentBuilder = StringBuilder(value.comment)
                value.starCount?.let {
                    val yellowStarCount = it.firstOrNull { s -> s.color == StarColor.Yellow }?.count ?: 0
                    val redStarCount = it.firstOrNull { s -> s.color == StarColor.Red }?.count ?: 0
                    val greenStarCount = it.firstOrNull { s -> s.color == StarColor.Green }?.count ?: 0
                    val blueStarCount = it.firstOrNull { s -> s.color == StarColor.Blue }?.count ?: 0
                    val purpleStarCount = it.firstOrNull { s -> s.color == StarColor.Purple }?.count ?: 0

                    commentBuilder.append(" ")
                    appendStarText(commentBuilder, purpleStarCount, view.context, R.color.starPurple)
                    appendStarText(commentBuilder, blueStarCount, view.context, R.color.starBlue)
                    appendStarText(commentBuilder, redStarCount, view.context, R.color.starRed)
                    appendStarText(commentBuilder, greenStarCount, view.context, R.color.starGreen)
                    appendStarText(commentBuilder, yellowStarCount, view.context, R.color.starYellow)
                }
                view.comment.apply {
                    setHtml(commentBuilder.toString())
                    visibility = value.comment.isNotBlank().toVisibility()
                }

                view.timestamp.text = value.timestamp.format(
                    DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm")
                )

                Glide.with(view).run {
                    clear(view.icon)
                    load(value.userIconUrl)
                    .into(view.icon)
                }
            }
    }
}
