package com.suihan74.satena.scenes.bookmarks

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.suihan74.hatenaLib.Bookmark
import com.suihan74.hatenaLib.StarColor
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

                val commentBuilder = StringBuilder(value.comment)
                value.starCount?.let {
                    val yellowStarCount = it.firstOrNull { s -> s.color == StarColor.Yellow }?.count ?: 0
                    val redStarCount = it.firstOrNull { s -> s.color == StarColor.Red }?.count ?: 0
                    val greenStarCount = it.firstOrNull { s -> s.color == StarColor.Green }?.count ?: 0
                    val blueStarCount = it.firstOrNull { s -> s.color == StarColor.Blue }?.count ?: 0
                    val purpleStarCount = it.firstOrNull { s -> s.color == StarColor.Purple }?.count ?: 0

                    commentBuilder.append(" ")
                    commentBuilder.appendStarText(purpleStarCount, context, R.color.starPurple)
                    commentBuilder.appendStarText(blueStarCount, context, R.color.starBlue)
                    commentBuilder.appendStarText(redStarCount, context, R.color.starRed)
                    commentBuilder.appendStarText(greenStarCount, context, R.color.starGreen)
                    commentBuilder.appendStarText(yellowStarCount, context, R.color.starYellow)
                }
                binding.comment.apply {
                    setHtml(commentBuilder.toString())
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
