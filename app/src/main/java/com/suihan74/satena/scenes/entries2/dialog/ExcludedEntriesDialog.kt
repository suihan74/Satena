package com.suihan74.satena.scenes.entries2.dialog

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.BindingAdapter
import androidx.lifecycle.*
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.suihan74.hatenaLib.Entry
import com.suihan74.satena.SatenaApplication
import com.suihan74.satena.databinding.FragmentDialogExcludedEntriesBinding
import com.suihan74.satena.databinding.ListviewItemEntries2Binding
import com.suihan74.satena.scenes.entries2.*
import com.suihan74.utilities.ItemClickedListener
import com.suihan74.utilities.ItemLongClickedListener
import com.suihan74.utilities.ItemMultipleClickedListener
import com.suihan74.utilities.extensions.alsoAs
import com.suihan74.utilities.extensions.requireActivity
import com.suihan74.utilities.lazyProvideViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.sync.Mutex

/** フィルタで除外されたエントリ一覧を表示する画面 */
class ExcludedEntriesDialog : BottomSheetDialogFragment() {
    companion object {
        fun createInstance(excludedEntries: List<ExcludedEntry>) = ExcludedEntriesDialog().also {
            it.lifecycleScope.launchWhenCreated {
                it.viewModel.excludedEntries.value = excludedEntries
            }
        }
    }

    // ------ //

    private val viewModel by lazyProvideViewModel {
        val entriesActivity = requireActivity<EntriesActivity>()
        val entriesViewModel = entriesActivity.viewModel
        DialogViewModel(
            entriesViewModel.repository,
            SatenaApplication.instance.readEntriesRepository
        )
    }

    // ------ //

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val binding = FragmentDialogExcludedEntriesBinding.inflate(layoutInflater, container, false).also {
            it.vm = viewModel
            it.lifecycleOwner = viewLifecycleOwner
        }

        binding.entriesRecyclerView.adapter = viewModel.excludedEntriesAdapter(this)

        binding.closeButton.setOnClickListener {
            dismiss()
        }

        return binding.root
    }


    // ------ //

    class DialogViewModel(
        private val entriesRepo: EntriesRepository,
        private val readEntriesRepo: ReadEntriesRepository
    ) : ViewModel() {
        private val entryMenuActions: EntryMenuActions =
            EntryMenuActionsImplForEntries(entriesRepo, readEntriesRepo)

        val excludedEntries = MutableLiveData<List<ExcludedEntry>>()

        // ------ //

        init {
            readEntriesRepo.readEntryIds.onEach { readIds ->
                val list = _excludedEntriesAdapter?.currentList.orEmpty()
                _excludedEntriesAdapter?.submitList(
                    list.map {
                        it.copy(read = readIds.contains(it.entry.id))
                    }
                )
            }.launchIn(viewModelScope)
        }

        // ------ //

        private var _excludedEntriesAdapter : ExcludedEntriesAdapter? = null

        fun excludedEntriesAdapter(fragment: ExcludedEntriesDialog) =
            ExcludedEntriesAdapter(fragment.viewLifecycleOwner).apply {
                _excludedEntriesAdapter = this
                multipleClickDuration = entriesRepo.entryMultipleClickDuration
                setOnClickListener { entry ->
                    entryMenuActions.invokeEntryClickedAction(
                        fragment.requireActivity(),
                        entry,
                        entriesRepo.entryClickedAction,
                        fragment.childFragmentManager
                    )
                }
                setOnLongClickListener { entry ->
                    entryMenuActions.invokeEntryClickedAction(
                        fragment.requireActivity(),
                        entry,
                        entriesRepo.entryLongClickedAction,
                        fragment.childFragmentManager
                    )
                    true
                }
                setOnMultipleClickListener { entry, i ->
                    entryMenuActions.invokeEntryClickedAction(
                        fragment.requireActivity(),
                        entry,
                        entriesRepo.entryMultipleClickedAction,
                        fragment.childFragmentManager
                    )
                }
                setOnClickEdgeListener { entry ->
                    entryMenuActions.invokeEntryClickedAction(
                        fragment.requireActivity(),
                        entry,
                        entriesRepo.entryEdgeClickedAction,
                        fragment.childFragmentManager
                    )
                }
                setOnLongClickEdgeListener { entry ->
                    entryMenuActions.invokeEntryClickedAction(
                        fragment.requireActivity(),
                        entry,
                        entriesRepo.entryEdgeLongClickedAction,
                        fragment.childFragmentManager
                    )
                    true
                }
                setOnMultipleClickEdgeListener { entry, i ->
                    entryMenuActions.invokeEntryClickedAction(
                        fragment.requireActivity(),
                        entry,
                        entriesRepo.entryEdgeMultipleClickedAction,
                        fragment.childFragmentManager
                    )
                }
            }
    }
}

// ------ //

data class ExcludedEntry(
    val entry : Entry,
    val read : Boolean
)

// ------ //

class ExcludedEntriesAdapter(
    private val lifecycleOwner: LifecycleOwner
) : ListAdapter<ExcludedEntry, ExcludedEntriesAdapter.ViewHolder>(DiffCallback()) {

    /** クリック処理済みフラグ（複数回タップされないようにする） */
    private val clickLock = Mutex()

    /** クリック回数判定時間 */
    var multipleClickDuration: Long = 0L

    private var onClickListener : ItemClickedListener<Entry>? = null
    private var onLongClickListener : ItemLongClickedListener<Entry>? = null
    private var onMultipleClickListener : ItemMultipleClickedListener<Entry>? = null
    private var onClickEdgeListener : ItemClickedListener<Entry>? = null
    private var onLongClickEdgeListener : ItemLongClickedListener<Entry>? = null
    private var onMultipleClickEdgeListener : ItemMultipleClickedListener<Entry>? = null

    fun setOnClickListener(listener : ItemClickedListener<Entry>?) {
        onClickListener = listener
    }

    fun setOnLongClickListener(listener : ItemLongClickedListener<Entry>?) {
        onLongClickListener = listener
    }

    fun setOnMultipleClickListener(listener : ItemMultipleClickedListener<Entry>?) {
        onMultipleClickListener = listener
    }

    fun setOnClickEdgeListener(listener: ItemClickedListener<Entry>?) {
        onClickEdgeListener = listener
    }

    fun setOnLongClickEdgeListener(listener: ItemLongClickedListener<Entry>?) {
        onLongClickEdgeListener = listener
    }

    fun setOnMultipleClickEdgeListener(listener : ItemMultipleClickedListener<Entry>?) {
        onMultipleClickEdgeListener = listener
    }

    // ------ //

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ListviewItemEntries2Binding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.binding.also {
            holder.initialize(currentList[position], lifecycleOwner)
        }
    }

    // ------ //

    inner class ViewHolder(val binding : ListviewItemEntries2Binding) : RecyclerView.ViewHolder(binding.root) {
        private var clickCount = 0
        private val clickGuardRefreshDelay = 800L
        private var consideringMultipleClickedEntry : Entry? = null

        fun initialize(excludedEntry: ExcludedEntry?, lifecycleOwner: LifecycleOwner) {
            this.clickCount = 0

            val entry = excludedEntry?.entry
            binding.entry = excludedEntry?.entry
            binding.read = excludedEntry?.read ?: false
            binding.lifecycleOwner = lifecycleOwner

            // 項目 タップ/長押し/複数回
            itemView.setOnClickListener(clickListener(entry, onClickListener, onMultipleClickListener))
            itemView.setOnLongClickListener(longClickListener(entry, onLongClickListener))

            // 右端 タップ/長押し/複数回
            binding.edgeClickArea.setOnClickListener(clickListener(entry, onClickEdgeListener, onMultipleClickEdgeListener))
            binding.edgeClickArea.setOnLongClickListener(longClickListener(entry, onLongClickEdgeListener))
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

    // ------ //

    class DiffCallback : DiffUtil.ItemCallback<ExcludedEntry>() {
        override fun areItemsTheSame(oldItem: ExcludedEntry, newItem: ExcludedEntry) =
            oldItem.entry.id == newItem.entry.id

        override fun areContentsTheSame(oldItem: ExcludedEntry, newItem: ExcludedEntry) =
            oldItem.entry.same(newItem.entry) && oldItem.read == newItem.read
    }
}

// ------ //

@BindingAdapter("excludedEntries")
fun setExcludedEntries(recyclerView: RecyclerView, entries: List<ExcludedEntry>?) {
    recyclerView.adapter.alsoAs<ExcludedEntriesAdapter> { adapter ->
        adapter.submitList(entries.orEmpty())
    }
}
