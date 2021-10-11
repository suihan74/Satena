package com.suihan74.satena.scenes.preferences.pages

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.suihan74.satena.R
import com.suihan74.satena.SatenaApplication
import com.suihan74.satena.databinding.FragmentPreferencesIgnoredEntriesBinding
import com.suihan74.satena.models.ignoredEntry.IgnoredEntryType
import com.suihan74.satena.scenes.preferences.ignored.IgnoredEntriesAdapter
import com.suihan74.satena.scenes.preferences.ignored.IgnoredEntryViewModel
import com.suihan74.utilities.lazyProvideViewModel

/**
 * フォロー中ユーザーリスト
 */
class IgnoredEntriesFragment : Fragment() {
    companion object {
        fun createInstance() = IgnoredEntriesFragment()
    }

    // ------ //

    private val viewModel by lazyProvideViewModel {
        IgnoredEntryViewModel(SatenaApplication.instance.ignoredEntriesRepository)
    }

    // ------ //

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        viewModel.onCreate(viewLifecycleOwner)

        val binding = FragmentPreferencesIgnoredEntriesBinding.inflate(inflater, container, false).also {
            it.vm = viewModel
            it.lifecycleOwner = viewLifecycleOwner
        }

        binding.itemsList.apply {
            adapter = IgnoredEntriesAdapter(viewLifecycleOwner).also { adapter ->
                adapter.setOnClickItemListener {
                    val entry = it.entry ?: return@setOnClickItemListener
                    viewModel.openModifyItemDialog(entry, childFragmentManager)
                }
                adapter.setOnLongLickItemListener {
                    val entry = it.entry ?: return@setOnLongLickItemListener
                    viewModel.openMenuDialog(context, entry, childFragmentManager)
                }
            }
        }

        binding.addButton.setOnClickListener {
            viewModel.openAddItemDialog(childFragmentManager)
        }

        binding.modeToggleButton.apply {
            lifecycleScope.launchWhenResumed {
                check(modeToCheckedId(viewModel.mode))
            }
            isSingleSelection = true
            isSelectionRequired = true
            addOnButtonCheckedListener { _, checkedId, isChecked ->
                if (!isChecked) return@addOnButtonCheckedListener
                viewModel.setMode(checkedIdToMode(checkedId), viewLifecycleOwner)
            }
        }

        return binding.root
    }

    private fun checkedIdToMode(checkedId: Int) = when(checkedId) {
        R.id.urlsButton -> IgnoredEntryType.URL
        R.id.wordsButton -> IgnoredEntryType.TEXT
        else -> throw IllegalStateException()
    }

    private fun modeToCheckedId(mode: IgnoredEntryType) = when(mode) {
        IgnoredEntryType.URL -> R.id.urlsButton
        IgnoredEntryType.TEXT -> R.id.wordsButton
    }
}
