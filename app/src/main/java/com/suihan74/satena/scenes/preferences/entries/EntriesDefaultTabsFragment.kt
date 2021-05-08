package com.suihan74.satena.scenes.preferences.entries

import android.os.Bundle
import android.transition.Fade
import android.transition.Slide
import android.transition.TransitionSet
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.addCallback
import androidx.databinding.BindingAdapter
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import com.suihan74.satena.databinding.FragmentPrefsEntriesDefaultTabsBinding
import com.suihan74.satena.scenes.preferences.PreferencesActivity
import com.suihan74.satena.scenes.preferences.PreferencesTab
import com.suihan74.utilities.SafeSharedPreferences
import com.suihan74.utilities.extensions.alsoAs
import com.suihan74.utilities.lazyProvideViewModel

/**
 * カテゴリごとの最初に表示するタブを設定する画面
 */
class EntriesDefaultTabsFragment : Fragment() {
    companion object {
        fun createInstance() = EntriesDefaultTabsFragment()
    }

    // ------ //

    private val preferencesActivity
        get() = requireActivity() as PreferencesActivity

    private val viewModel by lazyProvideViewModel {
        EntriesDefaultTabsViewModel(
            SafeSharedPreferences.create(requireContext())
        )
    }

    // ------ //

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enterTransition = TransitionSet()
            .addTransition(Fade())
            .addTransition(Slide(Gravity.END))

        exitTransition = TransitionSet()
            .addTransition(Fade())
            .addTransition(Slide(Gravity.END))
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val binding = FragmentPrefsEntriesDefaultTabsBinding.inflate(inflater, container, false).also {
            it.vm = viewModel
            it.lifecycleOwner = viewLifecycleOwner
        }

        val adapter = EntriesDefaultTabsAdapter(viewLifecycleOwner)
        binding.recyclerView.adapter = adapter

        adapter.setOnClickItemListener { b ->
            viewModel.openTabSelectionDialog(b.data!!.category, childFragmentManager)
        }

        val callback = requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner) {
            parentFragmentManager.beginTransaction()
                .remove(this@EntriesDefaultTabsFragment)
                .commit()
        }

        preferencesActivity.viewModel.currentTab.observe(viewLifecycleOwner, {
            callback.isEnabled = it == PreferencesTab.ENTRIES
        })

        binding.backButton.setOnClickListener {
            callback.handleOnBackPressed()
        }

        return binding.root
    }

    // ------ //

    object BindingAdapters {
        @JvmStatic
        @BindingAdapter("items")
        fun bindItems(recyclerView: RecyclerView, items: List<EntriesDefaultTabSetting>?) {
            if (items == null) return
            recyclerView.adapter.alsoAs<EntriesDefaultTabsAdapter> { adapter ->
                adapter.setItems(items)
            }
        }
    }
}
