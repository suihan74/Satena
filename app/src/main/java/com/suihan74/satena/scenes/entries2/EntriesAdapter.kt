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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex

class EntriesAdapter(
    private var lifecycleOwner: LifecycleOwner
) : ListAdapter<RecyclerState<EntriesAdapter.DisplayEntry>, RecyclerView.ViewHolder>(DiffCallback()) {

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
    private val clickLock = Mutex()

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
        runCatching {
            consideringMultipleClickedEntry = null
            clickLock.unlock()
        }
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
    fun submitEntries(
        items: List<Entry>?,
        readEntryIds: Set<Long>?,
        commitCallback: (()->Any?)? = null
    ) {
        val newList =
            when {
                items.isNullOrEmpty() -> emptyList()
                else -> RecyclerState.makeStatesWithFooter(
                    items.map {
                        DisplayEntry(it, readEntryIds?.contains(it.id) ?: false)
                    }
                )
            }

        submitList(newList) {
            lifecycleOwner.lifecycleScope.launchWhenResumed {
                commitCallback?.invoke()
                onItemsSubmitted?.invoke(items ?: emptyList())
            }
        }
    }

    // ------ //

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

    // ------ //

    /**
     * 既読状態を併せて保持した表示用のアイテム
     */
    data class DisplayEntry(
        val entry : Entry,
        val read : Boolean
    )

    // ------ //

    private class DiffCallback : DiffUtil.ItemCallback<RecyclerState<DisplayEntry>>() {
        override fun areItemsTheSame(
            oldItem: RecyclerState<DisplayEntry>,
            newItem: RecyclerState<DisplayEntry>
        ) : Boolean {
            val oldEntry = oldItem.body?.entry
            val newEntry = newItem.body?.entry
            return oldItem.type == newItem.type &&
                    oldEntry?.id == newEntry?.id && oldEntry?.url == newEntry?.url
        }

        override fun areContentsTheSame(
            oldItem: RecyclerState<DisplayEntry>,
            newItem: RecyclerState<DisplayEntry>
        ) : Boolean {
            val oldEntry = oldItem.body?.entry
            val newEntry = newItem.body?.entry
            return oldItem.type == newItem.type &&
                    oldEntry?.count == newEntry?.count &&
                    oldEntry?.title == newEntry?.title &&
                    oldEntry?.imageUrl == newEntry?.imageUrl &&
                    oldEntry?.bookmarkedData == newEntry?.bookmarkedData &&
                    oldItem.body?.read == newItem.body?.read
        }
    }

    // ------ //

    private var consideringMultipleClickedEntry : Entry? = null

    inner class ViewHolder(
        private val binding: ListviewItemEntries2Binding
    ) : RecyclerView.ViewHolder(binding.root) {
        private var clickCount = 0
        private val clickGuardRefreshDelay = 800L

        private var displayEntry : DisplayEntry? = null

        private val entry : Entry?
            get() = displayEntry?.entry

        init {
            binding.commentsList.adapter = CommentsAdapter().apply {
                setOnItemClickedListener { comment ->
                    lifecycleOwner.lifecycleScope.launchWhenResumed {
                        if (clickLock.tryLock()) {
                            try {
                                val entry = entry ?: return@launchWhenResumed
                                onCommentClicked?.invoke(entry, comment)
                                delay(clickGuardRefreshDelay)
                            }
                            finally {
                                runCatching { clickLock.unlock() }
                            }
                        }
                    }
                }
                setOnItemLongClickedListener { comment ->
                    lifecycleOwner.lifecycleScope.launchWhenResumed {
                        if (clickLock.tryLock()) {
                            try {
                                val entry = entry ?: return@launchWhenResumed
                                onCommentLongClicked?.invoke(entry, comment)
                                delay(clickGuardRefreshDelay)
                            }
                            finally {
                                runCatching { clickLock.unlock() }
                            }
                        }
                    }
                    onCommentLongClicked != null
                }
            }
        }

        // ------ //

        fun initialize(displayEntry: DisplayEntry?) {
            this.displayEntry = displayEntry
            this.clickCount = 0

            binding.entry = displayEntry?.entry
            binding.read = displayEntry?.read ?: false

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
        ) : (View)->Unit = body@ {
            if (entry == null) return@body
            if (multipleClickDuration == 0L) {
                if (clickLock.tryLock()) {
                    lifecycleOwner.lifecycleScope.launchWhenResumed {
                        try {
                            singleClickAction?.invoke(entry)
                            delay(clickGuardRefreshDelay)
                        }
                        finally {
                            runCatching { clickLock.unlock() }
                        }
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
            if (entry == null || clickLock.isLocked || clickCount++ > 0) return
            if (consideringMultipleClickedEntry != null && consideringMultipleClickedEntry != entry) return
            consideringMultipleClickedEntry = entry
            val duration = multipleClickDuration
            lifecycleOwner.lifecycleScope.launchWhenResumed {
                delay(duration)
                if (clickLock.tryLock()) {
                    val count = clickCount
                    clickCount = 0
                    try {
                        when (count) {
                            1 -> singleClickAction?.invoke(entry)
                            else -> multipleClickAction?.invoke(entry, count)
                        }
                        delay(clickGuardRefreshDelay)
                    }
                    finally {
                        consideringMultipleClickedEntry = null
                        runCatching { clickLock.unlock() }
                    }
                }
            }
        }
    }
}
