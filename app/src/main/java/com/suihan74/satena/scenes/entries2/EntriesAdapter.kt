package com.suihan74.satena.scenes.entries2

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.suihan74.hatenaLib.BookmarkResult
import com.suihan74.hatenaLib.Entry
import com.suihan74.satena.R
import com.suihan74.satena.databinding.ListviewItemEntries2Binding
import com.suihan74.utilities.*
import kotlinx.android.synthetic.main.listview_item_entries2.view.*

class EntriesAdapter : ListAdapter<RecyclerState<Entry>, RecyclerView.ViewHolder>(DiffCallback()) {
    /** ロード中表示のできるフッタ */
    private var footer: LoadableFooterViewHolder? = null

    /** エントリクリック時の挙動 */
    private var onItemClicked : ItemClickedListener<Entry>? = null
    /** エントリ長押し時の挙動 */
    private var onItemLongClicked : ItemLongClickedListener<Entry>? = null
    /** コメント部分クリック時の挙動 */
    private var onCommentClicked : ((Entry, BookmarkResult)->Unit)? = null

    /** クリック処理済みフラグ（複数回タップされないようにする） */
    private var itemClicked = false

    /** 項目クリック時の挙動をセットする */
    fun setOnItemClickedListener(listener: ItemClickedListener<Entry>?) {
        onItemClicked = listener
    }

    /** 項目長押し時の挙動をセットする */
    fun setOnItemLongClickedListener(listener: ItemLongClickedListener<Entry>?) {
        onItemLongClicked = listener
    }

    /** エントリに含まれるコメントをクリックしたときの挙動をセットする */
    fun setOnCommentClickedListener(listener: ((Entry, BookmarkResult)->Unit)?) {
        onCommentClicked = listener
    }

    /** 復帰時に実行する */
    fun onResume() {
        itemClicked = false
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) : RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            RecyclerType.BODY.int -> {
                val binding = DataBindingUtil.inflate<ListviewItemEntries2Binding>(
                    inflater,
                    R.layout.listview_item_entries2, parent, false
                )
                ViewHolder(binding)
            }

            RecyclerType.FOOTER.int -> LoadableFooterViewHolder(
                inflater.inflate(R.layout.footer_recycler_view_loadable, parent, false)
            ).also {
                this.footer = it
            }

            else -> throw NotImplementedError()
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder.itemViewType) {
            RecyclerType.BODY.int -> {
                holder as ViewHolder
                val entry = currentList[position].body

                holder.entry = entry
                holder.itemView.apply {
                    setOnClickListener {
                        if (entry != null && !itemClicked) {
                            itemClicked = true
                            onItemClicked?.invoke(entry)
                        }
                    }
                    setOnLongClickListener {
                        if (entry != null) onItemLongClicked?.invoke(entry) ?: false
                        else true
                    }
                }
            }
        }
    }

    override fun getItemViewType(position: Int) =
        currentList[position].type.int

    /** エントリはこのメソッドを使ってセットする */
    fun submitEntries(items: List<Entry>?, commitCallback: (()->Any?)? = null) {
        val newList : List<RecyclerState<Entry>> =
            if (items.isNullOrEmpty()) emptyList()
            else RecyclerState.makeStatesWithFooter(items)

        submitList(newList) {
            hideProgressBar()
            commitCallback?.invoke()
        }
    }

    fun showProgressBar() {
        footer?.showProgressBar()
    }

    fun hideProgressBar() {
        footer?.hideProgressBar(false)
    }

    private class DiffCallback : DiffUtil.ItemCallback<RecyclerState<Entry>>() {
        override fun areItemsTheSame(
            oldItem: RecyclerState<Entry>,
            newItem: RecyclerState<Entry>
        ) = oldItem.type == newItem.type && oldItem.body?.id == newItem.body?.id && oldItem.body?.url == newItem.body?.url

        override fun areContentsTheSame(
            oldItem: RecyclerState<Entry>,
            newItem: RecyclerState<Entry>
        ) = oldItem.type == newItem.type &&
                oldItem.body?.count == newItem.body?.count &&
                oldItem.body?.title == newItem.body?.title &&
                oldItem.body?.imageUrl == newItem.body?.imageUrl &&
                oldItem.body?.bookmarkedData == newItem.body?.bookmarkedData
    }

    inner class ViewHolder(
        private val binding: ListviewItemEntries2Binding
    ) : RecyclerView.ViewHolder(binding.root) {
        var entry: Entry? = null
            set(value) {
                field = value
                binding.entry = value
            }

        init {
            binding.root.comments_list.adapter = CommentsAdapter().apply {
                setOnItemClickedListener listener@ { comment ->
                    val entry = entry ?: return@listener
                    onCommentClicked?.invoke(entry, comment)
                }
            }
        }
    }
}
