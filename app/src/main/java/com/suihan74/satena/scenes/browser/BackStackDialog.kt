package com.suihan74.satena.scenes.browser

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.suihan74.satena.databinding.FragmentDialogBrowserBackstackBinding
import com.suihan74.satena.models.browser.HistoryPage
import com.suihan74.utilities.DialogListener
import com.suihan74.utilities.lazyProvideViewModel

class BackStackDialog : BottomSheetDialogFragment() {
    companion object {
        fun createInstance() = BackStackDialog()
    }

    // ------ //

    private val browserActivity
        get() = requireActivity() as BrowserActivity

    private val browserViewModel
        get() = browserActivity.viewModel

    private val viewModel by lazyProvideViewModel {
        DialogViewModel(browserViewModel.backStack)
    }

    // ------ //

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val binding = FragmentDialogBrowserBackstackBinding.inflate(inflater, container, false).also {
            it.vm = viewModel
            it.lifecycleOwner = viewLifecycleOwner

            initializeRecyclerView(it.recyclerView)
        }
        return binding.root
    }

    // ------ //

    private fun initializeRecyclerView(recyclerView: RecyclerView) {
        recyclerView.adapter = BackStackAdapter(browserViewModel, viewLifecycleOwner).also { adapter ->
            adapter.setOnClickItemListener { binding ->
                binding.item?.let {
                    viewModel.onClickItem?.invoke(it, this)
                }
            }

            adapter.setOnLongLickItemListener { binding ->
                binding.item?.let {
                    viewModel.onLongClickItem?.invoke(it, this)
                }
            }
        }
    }

    // ------ //

    fun setOnClickItemListener(l : DialogListener<HistoryPage>?) = lifecycleScope.launchWhenCreated {
        viewModel.onClickItem = l
    }

    fun setOnLongClickItemListener(l : DialogListener<HistoryPage>?) = lifecycleScope.launchWhenCreated {
        viewModel.onLongClickItem = l
    }

    // ------ //

    class DialogViewModel(
        val items: LiveData<List<HistoryPage>>
    ) : ViewModel() {
        var onClickItem : DialogListener<HistoryPage>? = null

        var onLongClickItem : DialogListener<HistoryPage>? = null
    }
}
