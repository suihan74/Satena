package com.suihan74.satena.scenes.entries2

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.RecyclerView
import com.suihan74.hatenaLib.BookmarkResult
import com.suihan74.satena.R
import com.suihan74.satena.databinding.ListviewItemComments2Binding

class BookmarkCommentsAdapter : RecyclerView.Adapter<BookmarkCommentsAdapter.ViewHolder>() {
    // 非同期で更新するとスクロール時にエントリタイルの中でブコメ部分の反映だけが遅れてちらつくのが気持ち悪かったので
    // あえてUIスレッドで同期的に変更反映するためListAdapter<T,VH>は使用していない

    /** 現在表示中のリスト */
    private var currentList: List<BookmarkResult> = emptyList()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = DataBindingUtil.inflate<ListviewItemComments2Binding>(
            inflater,
            R.layout.listview_item_comments2, parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.item = currentList[position]
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
