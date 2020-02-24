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
import com.suihan74.satena.models.Category
import kotlinx.android.synthetic.main.activity_entries2.*

class SingleTabEntriesFragment : Fragment() {
    companion object {
        fun createInstance() = SingleTabEntriesFragment()
    }

    /** EntriesActivityのViewModel */
    private lateinit var activityViewModel : EntriesViewModel

    private lateinit var viewModel : EntriesFragmentViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        activityViewModel = ViewModelProviders.of(requireActivity())[EntriesViewModel::class.java]
        viewModel = ViewModelProviders.of(this)[EntriesFragmentViewModel::class.java]

        val category = activityViewModel.currentCategory.value
        viewModel.category.value = category
        setHasOptionsMenu(category == Category.MyBookmarks || category?.hasIssues == true)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val binding = DataBindingUtil.inflate<FragmentEntriesTab2Binding>(inflater, R.layout.fragment_entries_tab2, container, false).apply {
            lifecycleOwner = this@SingleTabEntriesFragment
        }

        requireActivity().toolbar.setTitle(viewModel.category.value?.textId ?: 0)

        return binding.root
    }
}
