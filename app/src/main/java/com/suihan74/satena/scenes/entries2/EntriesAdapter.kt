package com.suihan74.satena.scenes.entries2

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.suihan74.hatenaLib.BookmarkResult
import com.suihan74.hatenaLib.Entry
import com.suihan74.satena.databinding.FooterRecyclerViewLoadableBinding
import com.suihan74.satena.databinding.ListviewItemEntries2Binding
import com.suihan74.utilities.*
import com.suihan74.utilities.extensions.alsoAs
import kotlinx.coroutines.*

class EntriesAdapter(
    private var lifecycleOwner: LifecycleOwner
) : ListAdapter<RecyclerState<Entry>, RecyclerView.ViewHolder>(DiffCallback()) {

    /** エントリクリック時の挙動 */
    private var onItemClicked : ItemClickedListener<Entry>? = null
    /** エントリ複数回クリック時の挙動 */
    private var onItemMultipleClicked : ItemMultipleClickedListener<Entry>? = null
    /** エントリ長押し時の挙動 */
    private var onItemLongClicked : ItemLongClickedListener<Entry>? = null

    /** エントリ右端クリック時の挙動 */
    private var onItemEdgeClicked : ItemClickedListener<Entry>? = null
    /** エントリ右端複数回クリック時の挙動 */
    private var onItemEdgeMultipleClicked : ItemMultipleClickedListener<Entry>? = null
    /** エントリ右端長押し時の挙動 */
    private var onItemEdgeLongClicked : ItemLongClickedListener<Entry>? = null

    /** コメント部分クリック時の挙動 */
    private var onCommentClicked : ((Entry, BookmarkResult)->Unit)? = null

    /** コメント部分長押し時の挙動 */
    private var onCommentLongClicked : ((Entry, BookmarkResult)->Unit)? = null

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

    /** 項目クリック時の挙動をセットする */
    fun setOnItemEdgeClickedListener(listener: ItemClickedListener<Entry>?) {
        onItemEdgeClicked = listener
    }

    /** 項目連続複数回クリック時の挙動をセットする */
    fun setOnItemEdgeMultipleClickedListener(listener: ItemMultipleClickedListener<Entry>?) {
        onItemEdgeMultipleClicked = listener
    }

    /** 項目長押し時の挙動をセットする */
    fun setOnItemEdgeLongClickedListener(listener: ItemLongClickedListener<Entry>?) {
        onItemEdgeLongClicked = listener
    }

    /** エントリに含まれるコメントをクリックしたときの挙動をセットする */
    fun setOnCommentClickedListener(listener: ((Entry, BookmarkResult)->Unit)?) {
        onCommentClicked = listener
    }

    /** エントリに含まれるコメントを長押ししたときの挙動をセットする */
    fun setOnCommentLongClickedListener(listener: ((Entry, BookmarkResult)->Unit)?) {
        onCommentLongClicked = listener
    }

    /** アイテム追加完了時の挙動をセットする */
    fun setOnItemsSubmittedListener(listener: Listener<List<Entry>?>?) {
        onItemsSubmitted = listener
    }

    /** Footer: ロード中の表示用 */
    val loading = MutableLiveData<Boolean>()

    /** Footer: 追加ロードボタンを表示するか */
    val loadable = MutableLiveData<Boolean>(false)

    /** 復帰時に実行する */
    fun onResume() {
        itemClicked = false
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) : RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            RecyclerType.BODY.id -> {
                val binding = ListviewItemEntries2Binding.inflate(inflater, parent, false).also {
                    it.lifecycleOwner = lifecycleOwner
                }
                ViewHolder(binding)
            }

            RecyclerType.FOOTER.id -> LoadableFooterViewHolder(
                FooterRecyclerViewLoadableBinding.inflate(inflater, parent, false).also {
                    it.loading = loading
                    it.loadable = loadable
                    it.lifecycleOwner = lifecycleOwner
                }
            )

            else -> throw NotImplementedError()
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder.itemViewType) {
            RecyclerType.BODY.id -> holder.alsoAs<ViewHolder> {
                it.initialize(currentList[position].body)
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
            lifecycleOwner.lifecycleScope.launch(Dispatchers.Main) {
                commitCallback?.invoke()
                onItemsSubmitted?.invoke(items ?: emptyList())
            }
        }
    }

    fun showProgressBar() {
        lifecycleOwner.lifecycleScope.launch(Dispatchers.Main) {
            loading.value = true
        }
    }

    fun hideProgressBar() {
        lifecycleOwner.lifecycleScope.launch(Dispatchers.Main) {
            loading.value = false
        }
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
        private var clickCount = 0
        private val clickGuardRefreshDelay = 800L

        var entry: Entry? = null
            private set(value) {
                field = value
                binding.entry = value
            }

        init {
            binding.commentsList.adapter = CommentsAdapter().apply {
                setOnItemClickedListener { comment ->
                    if (!itemClicked) {
                        itemClicked = true
                        val entry = entry ?: return@setOnItemClickedListener
                        onCommentClicked?.invoke(entry, comment)
                        lifecycleOwner.lifecycleScope.launch {
                            delay(clickGuardRefreshDelay)
                            itemClicked = false
                        }
                    }
                }
                setOnItemLongClickedListener { comment ->
                    if (!itemClicked) {
                        itemClicked = true
                        val entry = entry ?: return@setOnItemLongClickedListener false
                        onCommentLongClicked?.invoke(entry, comment)
                        lifecycleOwner.lifecycleScope.launch {
                            delay(clickGuardRefreshDelay)
                            itemClicked = false
                        }
                    }
                    onCommentLongClicked != null
                }
            }
        }

        // ------ //

        fun initialize(entry: Entry?) {
            this.entry = entry
            this.clickCount = 0

            // 項目 タップ/長押し/複数回
            itemView.setOnClickListener(clickListener(entry, onItemClicked, onItemMultipleClicked))
            itemView.setOnLongClickListener(longClickListener(entry, onItemLongClicked))

            // 右端 タップ/長押し/複数回
            binding.edgeClickArea.setOnClickListener(clickListener(entry, onItemEdgeClicked, onItemEdgeMultipleClicked))
            binding.edgeClickArea.setOnLongClickListener(longClickListener(entry, onItemEdgeLongClicked))
        }

        private fun clickListener(
            entry: Entry?,
            singleClickAction: ItemClickedListener<Entry>?,
            multipleClickAction: ItemMultipleClickedListener<Entry>?
        ) : (View)->Unit = {
            if (multipleClickDuration == 0L) {
                if (entry != null && !itemClicked) {
                    itemClicked = true
                    singleClickAction?.invoke(entry)
                    lifecycleOwner.lifecycleScope.launch {
                        delay(clickGuardRefreshDelay)
                        itemClicked = false
                    }
                }
            }
            else {
                considerMultipleClick(entry, singleClickAction, multipleClickAction)
            }
        }

        private fun longClickListener(
            entry: Entry?,
            longClickAction: ItemLongClickedListener<Entry>?
        ) : (View)->Boolean = {
            if (entry != null) longClickAction?.invoke(entry) ?: false
            else true
        }

        // 複数回クリックを雑に検出する
        private fun considerMultipleClick(
            entry: Entry?,
            singleClickAction: ItemClickedListener<Entry>?,
            multipleClickAction: ItemMultipleClickedListener<Entry>?
        ) {
            if (clickCount++ > 0) return
            val duration = multipleClickDuration
            lifecycleOwner.lifecycleScope.launch {
                delay(duration)
                val count = clickCount
                clickCount = 0
                if (entry != null && !itemClicked) {
                    itemClicked = true
                    withContext(Dispatchers.Main) {
                        if (count > 1) {
                            multipleClickAction?.invoke(entry, count)
                        }
                        else {
                            singleClickAction?.invoke(entry)
                        }
                    }
                    delay(clickGuardRefreshDelay)
                    itemClicked = false
                }
            }
        }
    }
}
