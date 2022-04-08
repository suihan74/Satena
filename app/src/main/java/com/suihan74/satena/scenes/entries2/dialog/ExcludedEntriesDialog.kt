package com.suihan74.satena.scenes.entries2.dialog

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.databinding.BindingAdapter
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.suihan74.hatenaLib.Entry
import com.suihan74.satena.R
import com.suihan74.satena.SatenaApplication
import com.suihan74.satena.databinding.FragmentDialogExcludedEntriesBinding
import com.suihan74.satena.databinding.ListviewItemEntries2Binding
import com.suihan74.satena.scenes.entries2.*
import com.suihan74.utilities.Listener
import com.suihan74.utilities.extensions.alsoAs
import com.suihan74.utilities.extensions.requireActivity
import com.suihan74.utilities.lazyProvideViewModel

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

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        dialog.alsoAs<BottomSheetDialog> { d ->
            d.findViewById<FrameLayout>(R.id.design_bottom_sheet)?.let { bottomSheet ->
                BottomSheetBehavior.from(bottomSheet)
                    .setState(BottomSheetBehavior.STATE_EXPANDED)
            }
        }
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

        fun excludedEntriesAdapter(fragment: ExcludedEntriesDialog) =
            ExcludedEntriesAdapter(fragment.viewLifecycleOwner).apply {
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
                }
            }
    }
}

// ------ //

data class ExcludedEntry(
    val entry : Entry
)

// ------ //

class ExcludedEntriesAdapter(
    private val lifecycleOwner: LifecycleOwner
) : ListAdapter<ExcludedEntry, ExcludedEntriesAdapter.ViewHolder>(DiffCallback()) {

    private var onClickListener : Listener<Entry>? = null
    private var onLongClickListener : Listener<Entry>? = null
    private var onClickEdgeListener : Listener<Entry>? = null
    private var onLongClickEdgeListener : Listener<Entry>? = null

    fun setOnClickListener(listener : Listener<Entry>?) {
        onClickListener = listener
    }

    fun setOnLongClickListener(listener : Listener<Entry>?) {
        onLongClickListener = listener
    }

    fun setOnClickEdgeListener(listener: Listener<Entry>?) {
        onClickEdgeListener = listener
    }

    fun setOnLongClickEdgeListener(listener: Listener<Entry>?) {
        onLongClickEdgeListener = listener
    }

    // ------ //

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ListviewItemEntries2Binding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.binding.also {
            val entry = currentList[position].entry
            it.entry = entry
            it.lifecycleOwner = lifecycleOwner

            it.root.setOnClickListener {
                onClickListener?.invoke(entry)
            }
            it.root.setOnLongClickListener {
                onLongClickListener?.invoke(entry)
                onLongClickListener != null
            }
        }
    }

    // ------ //

    class ViewHolder(val binding : ListviewItemEntries2Binding) : RecyclerView.ViewHolder(binding.root) {}

    // ------ //

    class DiffCallback : DiffUtil.ItemCallback<ExcludedEntry>() {
        override fun areItemsTheSame(oldItem: ExcludedEntry, newItem: ExcludedEntry) =
            oldItem.entry.id == newItem.entry.id

        override fun areContentsTheSame(oldItem: ExcludedEntry, newItem: ExcludedEntry) =
            oldItem.entry.same(newItem.entry)
    }
}

// ------ //

@BindingAdapter("excludedEntries")
fun setExcludedEntries(recyclerView: RecyclerView, entries: List<ExcludedEntry>?) {
    recyclerView.adapter.alsoAs<ExcludedEntriesAdapter> { adapter ->
        adapter.submitList(entries.orEmpty())
    }
}
