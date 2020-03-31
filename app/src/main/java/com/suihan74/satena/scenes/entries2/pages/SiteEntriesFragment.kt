package com.suihan74.satena.scenes.entries2.pages

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStoreOwner
import com.suihan74.satena.models.Category
import com.suihan74.satena.scenes.entries2.EntriesFragmentViewModel
import com.suihan74.satena.scenes.entries2.EntriesRepository
import com.suihan74.utilities.putEnum
import com.suihan74.utilities.withArguments
import kotlinx.android.synthetic.main.activity_entries2.*

class SiteEntriesFragment : TwinTabsEntriesFragment() {
    companion object {
        fun createInstance(siteUrl: String) = SiteEntriesFragment().withArguments {
            putEnum(ARG_CATEGORY, Category.Site)
            putString(ARG_SITE_URL, siteUrl)
        }

        private const val ARG_SITE_URL = "ARG_SITE_URL"
    }

    override fun generateViewModel(
        owner: ViewModelStoreOwner,
        viewModelKey: String,
        repository: EntriesRepository,
        category: Category
    ): EntriesFragmentViewModel {
        val factory = HatenaEntriesViewModel.Factory(repository)
        return ViewModelProvider(owner, factory)[viewModelKey, HatenaEntriesViewModel::class.java].apply {
            siteUrl.value = requireArguments().getString(ARG_SITE_URL)
        }
    }

    override val title : String?
        get() = viewModel.siteUrl.value

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = super.onCreateView(inflater, container, savedInstanceState)

        val arguments = requireArguments()
        viewModel.siteUrl.value = arguments.getString(ARG_SITE_URL)

        // Category.SiteではサイトURLをタイトルに表示する
        val toolbar = requireActivity().toolbar
        viewModel.siteUrl.observe(viewLifecycleOwner, Observer {
            toolbar.title = title
        })

        return root
    }
}
