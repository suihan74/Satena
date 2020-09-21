package com.suihan74.satena.scenes.browser.history

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.BindingAdapter
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import com.suihan74.satena.R
import com.suihan74.satena.databinding.FragmentBrowserHistoryBinding
import com.suihan74.satena.models.browser.History
import com.suihan74.satena.scenes.browser.BrowserActivity
import com.suihan74.satena.scenes.browser.BrowserViewModel
import com.suihan74.utilities.extensions.alsoAs
import com.suihan74.utilities.provideViewModel

class HistoryFragment : Fragment() {
    companion object {
        fun createInstance() = HistoryFragment()

        @BindingAdapter("items")
        @JvmStatic
        fun setHistory(recyclerView: RecyclerView, items: List<History>) {
            recyclerView.adapter.alsoAs<HistoryAdapter> { adapter ->
                adapter.setItems(items)
            }
        }
    }

    private val browserActivity : BrowserActivity
        get() = requireActivity() as BrowserActivity

    private val activityViewModel : BrowserViewModel
        get() = browserActivity.viewModel

    private val viewModel by lazy {
        provideViewModel(this) {
            val repository = activityViewModel.repository
            HistoryViewModel(repository)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val binding = DataBindingUtil.inflate<FragmentBrowserHistoryBinding>(
            inflater,
            R.layout.fragment_browser_history,
            container,
            false
        ).also {
            it.vm = viewModel
            it.lifecycleOwner = viewLifecycleOwner
        }

        binding.recyclerView.let { recyclerView ->
            recyclerView.setHasFixedSize(true)
            recyclerView.adapter = HistoryAdapter(viewLifecycleOwner).also {
                it.setOnClickItemListener { binding ->
                    val site = binding.site ?: return@setOnClickItemListener
                    activityViewModel.goAddress(site.url)
                    browserActivity.closeDrawer()
                }

                it.setOnLongLickItemListener { binding ->
                }
            }
        }

        return binding.root
    }
}
