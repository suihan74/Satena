package com.suihan74.satena.scenes.bookmarks.detail.tabs

import android.widget.TextView
import androidx.databinding.BindingAdapter
import androidx.lifecycle.LifecycleOwner
import com.suihan74.hatenaLib.Bookmark
import com.suihan74.hatenaLib.HatenaClient
import com.suihan74.hatenaLib.Star
import com.suihan74.hatenaLib.StarColor
import com.suihan74.satena.R
import com.suihan74.satena.databinding.ListviewItemStarRelataionsBinding
import com.suihan74.satena.scenes.bookmarks.detail.DetailTabAdapter
import com.suihan74.satena.scenes.bookmarks.repository.StarRelation
import com.suihan74.utilities.GeneralAdapter
import com.suihan74.utilities.extensions.setHtml

class StarRelationsAdapter(
    val tabType: DetailTabAdapter.TabType,
    lifecycleOwner: LifecycleOwner
) : GeneralAdapter<StarRelationsAdapter.Item, ListviewItemStarRelataionsBinding>(
    lifecycleOwner,
    R.layout.listview_item_star_relataions,
    DiffCallback()
) {

    fun setStarRelations(starRelations: List<StarRelation>, ignoredUsers: List<String>, callback: Runnable? = null) {
        val items = when(tabType) {
            DetailTabAdapter.TabType.STARS_TO_USER -> starRelations.map {
                val bookmark = it.senderBookmark
                Item(
                    user = it.sender,
                    userIconUrl = HatenaClient.getUserIconUrl(it.sender),
                    comment = bookmark?.comment.orEmpty(),
                    bookmark = bookmark,
                    star = it.star,
                    ignored = ignoredUsers.contains(it.sender),
                    relation = it
                )
            }

            DetailTabAdapter.TabType.STARS_FROM_USER -> starRelations.map {
                val bookmark = it.receiverBookmark
                Item(
                    user = it.receiver,
                    userIconUrl = HatenaClient.getUserIconUrl(it.receiver),
                    comment = bookmark.comment,
                    bookmark = bookmark,
                    star = it.star,
                    ignored = ignoredUsers.contains(it.receiver),
                    relation = it
                )
            }

            // TODO
            else -> throw NotImplementedError()
        }

        setItems(items, callback)
    }

    override fun bind(model: Item?, binding: ListviewItemStarRelataionsBinding) {
        binding.item = model
    }

    // ------ //

    data class Item(
        val user : String,
        val userIconUrl : String,
        val comment : String,
        val bookmark : Bookmark?,
        val star : Star,
        val ignored : Boolean,
        val relation : StarRelation
    )

    // ------ //

    class DiffCallback : GeneralAdapter.DiffCallback<Item>() {
        override fun areModelsTheSame(oldItem: Item?, newItem: Item?): Boolean {
            return oldItem?.user == newItem?.user
                    && oldItem?.star?.color == newItem?.star?.color
                    && oldItem?.star?.quote == newItem?.star?.quote
        }

        override fun areModelContentsTheSame(oldItem: Item?, newItem: Item?): Boolean {
            return oldItem?.comment == newItem?.comment
                    && oldItem?.ignored == newItem?.ignored
                    && oldItem?.star?.color == newItem?.star?.color
                    && oldItem?.star?.count == newItem?.star?.count
                    && oldItem?.star?.quote == newItem?.star?.quote
        }
    }

    // ------ //

    object BindingAdapters {
        @JvmStatic
        @BindingAdapter("star")
        fun setStarText(textView: TextView, star: Star?) {
            if (star == null) {
                textView.text = ""
                return
            }

            val context = textView.context

            val starColor = context.getColor(
                when (star.color) {
                    StarColor.Yellow -> R.color.starYellow
                    StarColor.Red -> R.color.starRed
                    StarColor.Green -> R.color.starGreen
                    StarColor.Blue -> R.color.starBlue
                    StarColor.Purple -> R.color.starPurple
                }
            )

            val starText =
                if (star.count > 9) context.getString(R.string.star_with_count, star.count)
                else buildString {
                    repeat (star.count) {
                        append(context.getString(R.string.star))
                    }
                }

            textView.setHtml("<font color=\"$starColor\">$starText</font>")
        }
    }
}
