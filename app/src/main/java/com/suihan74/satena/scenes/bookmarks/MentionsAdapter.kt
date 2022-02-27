package com.suihan74.satena.scenes.bookmarks

import android.net.Uri
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.suihan74.hatenaLib.Bookmark
import com.suihan74.hatenaLib.StarColor
import com.suihan74.hatenaLib.countMap
import com.suihan74.satena.R
import com.suihan74.satena.databinding.ListviewItemCommentBinding
import com.suihan74.utilities.extensions.appendStarText
import com.suihan74.utilities.extensions.setHtml
import com.suihan74.utilities.extensions.toVisibility
import java.time.format.DateTimeFormatter

open class MentionsAdapter (
    private val comments: List<Bookmark>
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    open fun onItemClicked(item: Bookmark) {}
    open fun onItemLongClicked(item: Bookmark) : Boolean = true

    override fun getItemCount(): Int = comments.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val binding = ListviewItemCommentBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )

        return ViewHolder(binding).apply {
            itemView.setOnClickListener {
                onItemClicked(value!!)
            }

            itemView.setOnLongClickListener {
                onItemLongClicked(value!!)
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        (holder as ViewHolder).value = comments[position]
    }

    class ViewHolder(val binding: ListviewItemCommentBinding) : RecyclerView.ViewHolder(binding.root) {
        var value : Bookmark? = null
            internal set(value) {
                field = value
                value ?: return

                val context = binding.root.context
                binding.userName.text = value.user

                val comment = buildString {
                    append(Uri.decode(value.comment))
                    value.starCount?.let { stars ->
                        val map = stars.countMap()

                        append(" ")
                        map[StarColor.Purple]?.let { appendStarText(it, context, R.color.starPurple) }
                        map[StarColor.Blue]?.let { appendStarText(it, context, R.color.starBlue) }
                        map[StarColor.Red]?.let { appendStarText(it, context, R.color.starRed) }
                        map[StarColor.Green]?.let { appendStarText(it, context, R.color.starGreen) }
                        map[StarColor.Yellow]?.let { appendStarText(it, context, R.color.starYellow) }
                    }
                }
                binding.comment.apply {
                    setHtml(comment)
                    visibility = value.comment.isNotBlank().toVisibility()
                }

                binding.timestamp.text = value.timestamp.format(
                    DateTimeFormatter.ofPattern("uuuu/MM/dd HH:mm")
                )

                Glide.with(context).run {
                    clear(binding.icon)
                    load(value.userIconUrl)
                    .into(binding.icon)
                }
            }
    }
}
