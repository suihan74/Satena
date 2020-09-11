package com.suihan74.satena.scenes.entries2.pages

import android.content.res.ColorStateList
import android.view.Menu
import android.view.MenuInflater
import androidx.core.view.MenuItemCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStoreOwner
import com.google.android.material.bottomappbar.BottomAppBar
import com.google.android.material.tabs.TabLayout
import com.suihan74.satena.R
import com.suihan74.satena.models.Category
import com.suihan74.satena.scenes.entries2.EntriesActivity
import com.suihan74.satena.scenes.entries2.EntriesFragmentViewModel
import com.suihan74.satena.scenes.entries2.EntriesRepository
import com.suihan74.utilities.alsoAs
import com.suihan74.utilities.getThemeColor
import com.suihan74.utilities.putEnum
import com.suihan74.utilities.withArguments

class FavoriteSitesFragment : MultipleTabsEntriesFragment() {
    companion object {
        fun createInstance() = FavoriteSitesFragment().withArguments {
            putEnum(ARG_CATEGORY, Category.FavoriteSites)
        }
    }

    private val DIALOG_FAVORITE_SITES_SELECTION by lazy { "DIALOG_FAVORITE_SITES_SELECTION" }

    override fun generateViewModel(
        owner: ViewModelStoreOwner,
        viewModelKey: String,
        repository: EntriesRepository,
        category: Category
    ): EntriesFragmentViewModel {
        val factory = FavoriteSitesViewModel.Factory(repository)
        return ViewModelProvider(owner, factory)[viewModelKey, FavoriteSitesViewModel::class.java]
    }

    override fun updateActivityAppBar(
        activity: EntriesActivity,
        tabLayout: TabLayout,
        bottomAppBar: BottomAppBar?
    ): Boolean {
        val result = super.updateActivityAppBar(activity, tabLayout, bottomAppBar)

        val useTopBar = null == bottomAppBar?.also {
            activity.inflateExtraBottomMenu(R.menu.favorite_sites)
            initializeMenu(it.menu, it)
        }
        setHasOptionsMenu(useTopBar)

        return result
    }

    /** トップバーの作成 */
    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.favorite_sites, menu)
        initializeMenu(menu, null)
    }

    /** メニューを設定 */
    private fun initializeMenu(menu: Menu, bottomAppBar: BottomAppBar?) {
        val context = requireContext()
        menu.findItem(R.id.settings)?.let { menuItem ->
            if (bottomAppBar != null) {
                val iconColor = context.getThemeColor(R.attr.textColor)
                MenuItemCompat.setIconTintList(
                    menuItem,
                    ColorStateList.valueOf(iconColor)
                )
            }

            menuItem.setOnMenuItemClickListener {
                viewModel.alsoAs<FavoriteSitesViewModel> { vm ->
                    vm.showSitesSelectionDialog(
                        requireContext(),
                        childFragmentManager,
                        DIALOG_FAVORITE_SITES_SELECTION
                    )
                }
                true
            }
        }
    }
}
