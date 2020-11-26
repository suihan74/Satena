package com.suihan74.satena.scenes.bookmarks2.detail

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.suihan74.hatenaLib.StarColor
import com.suihan74.satena.R
import com.suihan74.satena.databinding.FooterRecyclerViewBinding
import com.suihan74.satena.databinding.ListviewItemStarsBinding
import com.suihan74.utilities.FooterViewHolder
import com.suihan74.utilities.RecyclerState
import com.suihan74.utilities.RecyclerType
import com.suihan74.utilities.extensions.setHtml
import com.suihan74.utilities.extensions.toVisibility

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
    override fun getItemViewType(position: Int) = currentList[position].type.id

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        LayoutInflater.from(parent.context).let { inflater ->
            when (RecyclerType.fromId(viewType)) {
                RecyclerType.BODY -> {
                    val binding = ListviewItemStarsBinding.inflate(
                        inflater,
                        parent,
                        false
                    )
                    ViewHolder(binding).apply {
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
                }

                RecyclerType.FOOTER -> {
                    val binding = FooterRecyclerViewBinding.inflate(
                        inflater,
                        parent,
                        false
                    )
                    FooterViewHolder(binding)
                }

                else -> throw RuntimeException("an invalid view type")
            }
        }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) =
        when (RecyclerType.fromId(holder.itemViewType)) {
            RecyclerType.BODY ->
                (holder as ViewHolder).body = currentList[position].body!!

            else -> {}
        }

    fun setStars(stars: List<StarWithBookmark>) {
        if (currentList.isEmpty() && stars.isEmpty()) return
        submitList(RecyclerState.makeStatesWithFooter(stars))
    }

    class ViewHolder(private val binding: ListviewItemStarsBinding) : RecyclerView.ViewHolder(binding.root) {
        var body: StarWithBookmark? = null
            set(value) {
                field = value
                if (value == null) return // TODO: 既存の内容を削除するようにするべき

                val context = binding.root.context

                val star = value.star
                val bookmark = value.bookmark

                // IDとアイコン
                binding.starUserName.text = bookmark.user
                Glide.with(context).run {
                    clear(binding.starUserIcon)
                    load(bookmark.userIconUrl)
                        .into(binding.starUserIcon)
                }

                // ユーザーのブコメと引用文
                val quote = star?.quote
                val comment =
                    when {
                        quote.isNullOrBlank() -> bookmark.comment
                        bookmark.comment.isBlank() -> quote
                        else -> "\"$quote\"\n${bookmark.comment}"
                    }
                binding.starComment.let {
                    it.text = comment
                    it.visibility = (!comment.isBlank()).toVisibility()
                }

                // 非表示ブクマかどうかのマークを表示
                binding.mutedMark.visibility = (value.state == StarWithBookmark.DisplayState.COVER).toVisibility()

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
                val starColor = binding.root.context.getColor(colorId)
                val starText =
                    if (star.count > 9)
                        context.getString(R.string.star_with_count, star.count)
                    else
                        buildString {
                            repeat (star.count) { append(context.getString(R.string.star)) }
                        }

                binding.starStarsCount.setHtml("<font color=\"$starColor\">$starText</font>")
            }
    }
}
