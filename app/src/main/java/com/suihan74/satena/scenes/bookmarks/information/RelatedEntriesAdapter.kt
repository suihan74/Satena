package com.suihan74.satena.scenes.bookmarks.information

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.suihan74.hatenaLib.Entry
import com.suihan74.satena.databinding.ListviewItemRelatedEntriesBinding
import com.suihan74.utilities.*
import com.suihan74.utilities.extensions.alsoAs
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex

class RelatedEntriesAdapter(
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
                val binding = ListviewItemRelatedEntriesBinding.inflate(inflater, parent, false).also {
                    it.lifecycleOwner = lifecycleOwner
                }
                ViewHolder(binding)
            }

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
        }
    }

    // ------ //

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

    // ------ //

    private var consideringMultipleClickedEntry : Entry? = null

    inner class ViewHolder(
        private val binding: ListviewItemRelatedEntriesBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        private var clickCount = 0
        private val clickGuardRefreshDelay = 800L

        var entry: Entry? = null
            private set(value) {
                field = value
                binding.entry = value
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
