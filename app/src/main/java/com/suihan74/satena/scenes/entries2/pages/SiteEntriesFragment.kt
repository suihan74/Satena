package com.suihan74.satena.scenes.entries2.pages

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.ViewModelStoreOwner
import com.suihan74.satena.models.Category
import com.suihan74.satena.scenes.entries2.EntriesRepository
import com.suihan74.satena.scenes.entries2.EntriesViewModel
import com.suihan74.utilities.extensions.putEnum
import com.suihan74.utilities.extensions.withArguments
import com.suihan74.utilities.provideViewModel

class SiteEntriesFragment : MultipleTabsEntriesFragment() {
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
    ) = provideViewModel(owner, viewModelKey) {
        HatenaEntriesViewModel(repository).apply {
            siteUrl.value = requireArguments().getString(ARG_SITE_URL)
        }
    }

    private val activityViewModel by activityViewModels<EntriesViewModel>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = super.onCreateView(inflater, container, savedInstanceState)

        val arguments = requireArguments()
        viewModel.siteUrl.value = arguments.getString(ARG_SITE_URL)

        // Category.SiteではサイトURLをタイトルに表示する
        viewModel.siteUrl.observe(viewLifecycleOwner, {
            activityViewModel.toolbarTitle.value = it
        })

        return root
    }
}
