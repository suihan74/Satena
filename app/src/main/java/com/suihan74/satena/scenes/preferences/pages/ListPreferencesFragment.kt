package com.suihan74.satena.scenes.preferences.pages

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import com.suihan74.satena.databinding.FragmentPreferencesInformation2Binding
import com.suihan74.satena.scenes.preferences.PreferencesActivity
import com.suihan74.satena.scenes.preferences.PreferencesAdapter
import com.suihan74.utilities.provideViewModel

class ListPreferencesFragment : Fragment() {
    companion object {
        fun createInstance(
            viewModelCreator: (Context)->ListPreferencesViewModel
        ) = ListPreferencesFragment().apply {
            lifecycleScope.launchWhenCreated {
                _viewModel = provideViewModel(this@apply, "VM") {
                    viewModelCreator(requireContext())
                }
            }
        }
    }

    // ------ //

    val preferencesActivity
        get() = requireActivity() as PreferencesActivity

    // ------ //

    private var _viewModel : ListPreferencesViewModel? = null
    private val viewModel by lazy {
        _viewModel ?: ViewModelProvider(this)["VM", ListPreferencesViewModel::class.java]
    }

    // ------ //

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val binding = FragmentPreferencesInformation2Binding.inflate(inflater, container, false).also {
            it.vm = viewModel
            it.lifecycleOwner = viewLifecycleOwner
            initializeRecyclerView(it.recyclerView)
        }
        viewModel.onCreateView(this)
        return binding.root
    }

    // ------ //

    private fun initializeRecyclerView(recyclerView: RecyclerView) {
        recyclerView.adapter = PreferencesAdapter(viewLifecycleOwner)
    }
}
