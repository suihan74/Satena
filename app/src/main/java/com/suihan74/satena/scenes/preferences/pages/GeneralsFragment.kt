package com.suihan74.satena.scenes.preferences.pages

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import com.suihan74.satena.databinding.FragmentPreferencesInformation2Binding
import com.suihan74.satena.scenes.preferences.PreferencesAdapter
import com.suihan74.utilities.lazyProvideViewModel

class GeneralsFragment : Fragment() {

    companion object {
        fun createInstance() = GeneralsFragment()
    }

    // ------ //

    private val viewModel by lazyProvideViewModel {
        GeneralsViewModel(requireContext())
    }

    // ------ //

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val binding = FragmentPreferencesInformation2Binding.inflate(inflater, container, false).also {
            it.lifecycleOwner = viewLifecycleOwner
            initializeRecyclerView(it.recyclerView)
        }
        return binding.root
    }

    // ------ //

    @OptIn(ExperimentalStdlibApi::class)
    private fun initializeRecyclerView(recyclerView: RecyclerView) {
        val adapter = PreferencesAdapter(viewLifecycleOwner)
        adapter.submitList(viewModel.createItems(childFragmentManager))
        recyclerView.adapter = adapter
    }
}
