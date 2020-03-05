package com.suihan74.satena.scenes.entries2

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProviders
import com.suihan74.satena.R
import com.suihan74.satena.databinding.FragmentEntriesTab2Binding
import com.suihan74.satena.models.Category

class SingleTabEntriesFragment : EntriesFragment() {
    companion object {
        fun createInstance(category: Category) = SingleTabEntriesFragment().apply {
            arguments = Bundle().apply {
                putInt(ARG_CATEGORY, category.ordinal)
            }
        }
    }

    private lateinit var tabViewModel: EntriesTabFragmentViewModel

    override fun getTabTitleId(position: Int) = 0
    override val tabCount = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (savedInstanceState == null) {
            val factory = EntriesTabFragmentViewModel.Factory(
                activityViewModel.repository,
                Category.All
            )
            tabViewModel = ViewModelProviders.of(this, factory)[EntriesTabFragmentViewModel::class.java]
            tabViewModel.init()
        }
        else {
            tabViewModel = ViewModelProviders.of(this)[EntriesTabFragmentViewModel::class.java]
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        super.onCreateView(inflater, container, savedInstanceState)

        val binding = DataBindingUtil.inflate<FragmentEntriesTab2Binding>(inflater, R.layout.fragment_entries_tab2, container, false).apply {
            lifecycleOwner = this@SingleTabEntriesFragment
            vm = tabViewModel
        }

        return binding.root
    }
}
