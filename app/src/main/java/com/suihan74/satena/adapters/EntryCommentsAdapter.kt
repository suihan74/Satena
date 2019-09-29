package com.suihan74.satena.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.suihan74.HatenaLib.BookmarkResult
import com.suihan74.HatenaLib.StarColor
import com.suihan74.satena.R
import com.suihan74.utilities.appendStarText
import com.suihan74.utilities.getThemeColor
import com.suihan74.utilities.setHtml
import com.suihan74.utilities.toVisibility
import org.threeten.bp.format.DateTimeFormatter

open class EntryCommentsAdapter(
    private val comments: List<BookmarkResult>
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    class ViewHolder(val root: View) : RecyclerView.ViewHolder(root) {
        private val icon = root.findViewById<ImageView>(R.id.icon)!!
        private val user = root.findViewById<TextView>(R.id.user_name)!!
        private val comment = root.findViewById<TextView>(R.id.comment)!!
        private val timestamp = root.findViewById<TextView>(R.id.timestamp)!!

        private var mValue : BookmarkResult? = null
        var value : BookmarkResult
            get() = mValue!!
            internal set(v) {
                mValue = v

                user.apply {
                    text = v.user
                    if (v.private == true) {
                        val icon = resources.getDrawable(R.drawable.ic_baseline_lock, null).apply {
                            val size = textSize.toInt()
                            setBounds(0, 0, size, size)
                            setTint(root.context.getThemeColor(R.attr.textColor))
                        }
                        compoundDrawablePadding = 4
                        setCompoundDrawablesRelative(null, null, icon, null)
                    }
                }

                val commentBuilder = StringBuilder(v.commentRaw)
                val starsCount = value.starsCount
                if (starsCount != null) {
                    val yellowStarCount = starsCount.firstOrNull { it.color == StarColor.Yellow }?.count ?: 0
                    val redStarCount = starsCount.firstOrNull { it.color == StarColor.Red }?.count ?: 0
                    val greenStarCount = starsCount.firstOrNull { it.color == StarColor.Green }?.count ?: 0
                    val blueStarCount = starsCount.firstOrNull { it.color == StarColor.Blue }?.count ?: 0
                    val purpleStarCount = starsCount.firstOrNull { it.color == StarColor.Purple }?.count ?: 0

                    commentBuilder.append(" ")
                    appendStarText(commentBuilder, purpleStarCount, root.context, R.color.starPurple)
                    appendStarText(commentBuilder, blueStarCount, root.context, R.color.starBlue)
                    appendStarText(commentBuilder, redStarCount, root.context, R.color.starRed)
                    appendStarText(commentBuilder, greenStarCount, root.context, R.color.starGreen)
                    appendStarText(commentBuilder, yellowStarCount, root.context, R.color.starYellow)
                }
                comment.apply {
                    setHtml(commentBuilder.toString())
                    visibility = v.commentRaw.isNotBlank().toVisibility()
                }

                val formatter = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm")
                timestamp.text = v.timestamp.format(formatter)

                Glide.with(root)
                    .load(v.userIconUrl)
                    .into(icon)
            }
    }

    override fun getItemCount(): Int = comments.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflate = LayoutInflater.from(parent.context).inflate(R.layout.listview_item_comment, parent, false)
        val holder = ViewHolder(inflate)

        holder.itemView.setOnClickListener {
            onItemClicked(holder.value)
        }

        holder.itemView.setOnLongClickListener {
            onItemLongClicked(holder.value)
        }

        return holder
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        holder as ViewHolder
        holder.value = comments[position]
    }

    open fun onItemClicked(item: BookmarkResult) {}
    open fun onItemLongClicked(item: BookmarkResult) : Boolean = true
}
