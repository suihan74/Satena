package com.suihan74.satena.scenes.entries2

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.suihan74.hatenaLib.BookmarkResult
import com.suihan74.satena.databinding.ListviewItemComments2Binding
import com.suihan74.utilities.ItemClickedListener
import com.suihan74.utilities.ItemLongClickedListener

class CommentsAdapter : RecyclerView.Adapter<CommentsAdapter.ViewHolder>() {
    // 非同期で更新するとスクロール時にエントリタイルの中でブコメ部分の反映だけが遅れてちらつくのが気持ち悪かったので
    // あえてUIスレッドで同期的に変更反映するためListAdapter<T,VH>は使用していない

    /** 現在表示中のリスト */
    private var currentList: List<BookmarkResult> = emptyList()

    /** 項目をクリックしたときのイベント */
    private var onItemClicked : ItemClickedListener<BookmarkResult>? = null

    /** 項目を長押ししたときのイベント */
    private var onItemLongClicked : ItemLongClickedListener<BookmarkResult>? = null

    fun setOnItemClickedListener(listener: ItemClickedListener<BookmarkResult>?) {
        onItemClicked = listener
    }

    fun setOnItemLongClickedListener(listener: ItemLongClickedListener<BookmarkResult>? = null) {
        onItemLongClicked = listener
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ListviewItemComments2Binding.inflate(inflater, parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = currentList[position]
        holder.item = item
        holder.itemView.setOnClickListener {
            onItemClicked?.invoke(item)
        }
        holder.itemView.setOnLongClickListener {
            onItemLongClicked?.invoke(item) ?: true
        }
    }

    override fun getItemCount() = currentList.size

    /** リストを更新 */
    fun submitComments(items: List<BookmarkResult>?) {
        currentList = items ?: emptyList()
        notifyDataSetChanged()
    }

    class ViewHolder(
        private val binding: ListviewItemComments2Binding
    ) : RecyclerView.ViewHolder(binding.root) {
        var item: BookmarkResult? = null
            set(value) {
                field = value
                if (value != null) {
                    binding.item = value
                }
            }
    }
}
