package com.suihan74.satena.scenes.entries2

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProviders
import com.suihan74.satena.R
import com.suihan74.satena.databinding.FragmentEntriesTab2Binding

class EntriesTabFragment : Fragment() {
    companion object {
        fun createInstance() = EntriesTabFragment()
    }

    /** EntriesActivityのViewModel */
    private lateinit var activityViewModel : EntriesViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        activityViewModel = ViewModelProviders.of(requireActivity())[EntriesViewModel::class.java]
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val binding = DataBindingUtil.inflate<FragmentEntriesTab2Binding>(inflater, R.layout.fragment_entries_tab2, container, false).apply {
            lifecycleOwner = this@EntriesTabFragment
        }

        return binding.root
    }
}
