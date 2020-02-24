package com.suihan74.satena.scenes.entries2

import android.os.Bundle
import android.util.Log
import android.view.*
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProviders
import com.suihan74.satena.R
import com.suihan74.satena.databinding.FragmentEntries2Binding
import com.suihan74.satena.models.Category
import kotlinx.android.synthetic.main.activity_entries2.*

class EntriesFragmentViewModel : ViewModel() {
    /** この画面で表示しているカテゴリ */
    val category by lazy {
        MutableLiveData<Category>()
    }
}

class TwinTabsEntriesFragment : Fragment() {
    companion object {
        fun createInstance() = TwinTabsEntriesFragment()
    }

    /** EntriesActivityのViewModel */
    private lateinit var activityViewModel : EntriesViewModel

    private lateinit var viewModel : EntriesFragmentViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        activityViewModel = ViewModelProviders.of(requireActivity())[EntriesViewModel::class.java]
        val category = activityViewModel.currentCategory.value

        viewModel = ViewModelProviders.of(this)[EntriesFragmentViewModel::class.java]
        viewModel.category.value = category
        setHasOptionsMenu(category == Category.MyBookmarks || category?.hasIssues == true)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val binding = DataBindingUtil.inflate<FragmentEntries2Binding>(inflater, R.layout.fragment_entries2, container, false).apply {
            lifecycleOwner = this@TwinTabsEntriesFragment
            vm = viewModel
        }

        requireActivity().toolbar.setTitle(viewModel.category.value?.textId ?: 0)

        return binding.root
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)

        Log.i("TODO", "issues list")
    }
}
