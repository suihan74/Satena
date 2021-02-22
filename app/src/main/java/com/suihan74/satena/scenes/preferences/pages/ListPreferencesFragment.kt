package com.suihan74.satena.scenes.preferences.pages

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.suihan74.satena.databinding.FragmentListPreferencesBinding
import com.suihan74.satena.scenes.preferences.PreferencesActivity
import com.suihan74.satena.scenes.preferences.PreferencesAdapter
import com.suihan74.satena.scenes.preferences.PreferencesFragmentBase

/**
 * 設定リスト画面共通フラグメント
 */
abstract class ListPreferencesFragment : PreferencesFragmentBase() {
    val preferencesActivity
        get() = requireActivity() as PreferencesActivity

    // ------ //

    abstract val viewModel : ListPreferencesViewModel

    // ------ //

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val binding = FragmentListPreferencesBinding.inflate(inflater, container, false).also {
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
