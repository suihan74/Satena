package com.suihan74.satena.scenes.entries2

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.suihan74.hatenaLib.BookmarkResult
import com.suihan74.hatenaLib.Entry
import com.suihan74.satena.R
import com.suihan74.satena.databinding.FooterRecyclerViewLoadableBinding
import com.suihan74.satena.databinding.ListviewItemEntries2Binding
import com.suihan74.utilities.*
import kotlinx.coroutines.*

class EntriesAdapter(
    private var lifecycleOwner: LifecycleOwner,
    private val coroutineScope: CoroutineScope = GlobalScope
) : ListAdapter<RecyclerState<Entry>, RecyclerView.ViewHolder>(DiffCallback()) {
    /** ロード中表示のできるフッタ */
    private var footer: LoadableFooterViewHolder? = null

    /** エントリクリック時の挙動 */
    private var onItemClicked : ItemClickedListener<Entry>? = null
    /** エントリ複数回クリック時の挙動 */
    private var onItemMultipleClicked : ItemMultipleClickedListener<Entry>? = null
    /** エントリ長押し時の挙動 */
    private var onItemLongClicked : ItemLongClickedListener<Entry>? = null
    /** コメント部分クリック時の挙動 */
    private var onCommentClicked : ((Entry, BookmarkResult)->Unit)? = null

    /** アイテム追加完了時の挙動 */
    private var onItemsSubmitted : Listener<List<Entry>?>? = null

    /** クリック処理済みフラグ（複数回タップされないようにする） */
    private val itemClickedLock by lazy { Any() }
    private var itemClicked : Boolean = false
        get() = lock(itemClickedLock) { field }
        private set(value) {
            lock(itemClickedLock) {
                field = value
            }
        }

    /** クリック回数判定時間 */
    var multipleClickDuration: Long = 0L

    /** 項目クリック時の挙動をセットする */
    fun setOnItemClickedListener(listener: ItemClickedListener<Entry>?) {
        onItemClicked = listener
    }

    /** 項目連続複数回クリック時の挙動をセットする */
    fun setOnItemMultipleClickedListener(listener: ItemMultipleClickedListener<Entry>?) {
        onItemMultipleClicked = listener
    }


    /** 項目長押し時の挙動をセットする */
    fun setOnItemLongClickedListener(listener: ItemLongClickedListener<Entry>?) {
        onItemLongClicked = listener
    }

    /** エントリに含まれるコメントをクリックしたときの挙動をセットする */
    fun setOnCommentClickedListener(listener: ((Entry, BookmarkResult)->Unit)?) {
        onCommentClicked = listener
    }

    /** アイテム追加完了時の挙動をセットする */
    fun setOnItemsSubmittedListener(listener: Listener<List<Entry>?>?) {
        onItemsSubmitted = listener
    }

    /** 復帰時に実行する */
    fun onResume() {
        itemClicked = false
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) : RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            RecyclerType.BODY.id -> {
                val binding = DataBindingUtil.inflate<ListviewItemEntries2Binding>(
                    inflater,
                    R.layout.listview_item_entries2, parent, false
                ).also {
                    it.lifecycleOwner = lifecycleOwner
                }
                ViewHolder(binding)
            }

            RecyclerType.FOOTER.id -> LoadableFooterViewHolder(
                FooterRecyclerViewLoadableBinding.inflate(inflater, parent, false)
            ).also {
                this.footer = it
            }

            else -> throw NotImplementedError()
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder.itemViewType) {
            RecyclerType.BODY.id -> {
                holder as ViewHolder
                val entry = currentList[position].body

                holder.entry = entry
                holder.itemView.apply {

                    // 複数回クリックを雑に検出する
                    var clickCount = 0
                    val clickGuardRefreshDelay = 400L
                    fun considerMultipleClick(entry: Entry?) {
                        if (clickCount++ == 0) {
                            val duration = multipleClickDuration
                            coroutineScope.launch {
                                delay(duration)
                                val count = clickCount
                                clickCount = 0
                                if (entry != null && !itemClicked) {
                                    itemClicked = true
                                    withContext(Dispatchers.Main) {
                                        if (count > 1) {
                                            onItemMultipleClicked?.invoke(entry, count)
                                        }
                                        else {
                                            onItemClicked?.invoke(entry)
                                        }
                                    }
                                    delay(clickGuardRefreshDelay)
                                    itemClicked = false
                                }
                            }
                        }
                    }

                    setOnClickListener {
                        if (multipleClickDuration == 0L) {
                            if (entry != null && !itemClicked) {
                                itemClicked = true
                                onItemClicked?.invoke(entry)
                                coroutineScope.launch {
                                    delay(clickGuardRefreshDelay)
                                    itemClicked = false
                                }
                            }
                        }
                        else {
                            considerMultipleClick(entry)
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
        currentList[position].type.id

    /** リストをクリアする */
    fun clearEntries(commitCallback: (() -> Any?)? = null) {
        submitList(null) {
            commitCallback?.invoke()
            onItemsSubmitted?.invoke(null)
        }
    }

    /** エントリはこのメソッドを使ってセットする */
    fun submitEntries(items: List<Entry>?, commitCallback: (()->Any?)? = null) {
        val newList : List<RecyclerState<Entry>> =
            if (items.isNullOrEmpty()) emptyList()
            else RecyclerState.makeStatesWithFooter(items)

        submitList(newList) {
            hideProgressBar()
            commitCallback?.invoke()
            onItemsSubmitted?.invoke(items ?: emptyList())
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
            binding.commentsList.adapter = CommentsAdapter().apply {
                setOnItemClickedListener listener@ { comment ->
                    if (!itemClicked) {
                        itemClicked = true
                        val entry = entry ?: return@listener
                        onCommentClicked?.invoke(entry, comment)
                    }
                }
            }
        }
    }
}
