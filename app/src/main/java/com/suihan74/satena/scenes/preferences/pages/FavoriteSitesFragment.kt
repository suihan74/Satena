package com.suihan74.satena.scenes.preferences.pages

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.suihan74.satena.SatenaApplication
import com.suihan74.satena.databinding.FragmentBrowserFavoritesBinding
import com.suihan74.satena.scenes.browser.BrowserActivity
import com.suihan74.satena.scenes.browser.favorites.FavoriteSitesActionsImplForBrowser
import com.suihan74.satena.scenes.preferences.favoriteSites.FavoriteSitesActionsImplForPreferences
import com.suihan74.satena.scenes.preferences.favoriteSites.FavoriteSitesAdapter
import com.suihan74.satena.scenes.preferences.favoriteSites.FavoriteSitesViewModel
import com.suihan74.utilities.ScrollableToTop
import com.suihan74.utilities.lazyProvideViewModel

/** お気に入りサイトを表示するタブ */
class FavoriteSitesFragment :
    Fragment(),
    ScrollableToTop
{
    companion object {
        fun createInstance() = FavoriteSitesFragment()
    }

    // ------ //

    private val browserActivity : BrowserActivity?
        get() = requireActivity() as? BrowserActivity

    val viewModel by lazyProvideViewModel {
        val browserDao = SatenaApplication.instance.browserDao
        val actionsImpl =
            if (browserActivity != null) FavoriteSitesActionsImplForBrowser(browserDao)
            else FavoriteSitesActionsImplForPreferences()

        FavoriteSitesViewModel(
            SatenaApplication.instance.favoriteSitesRepository,
            actionsImpl
        )
    }

    private var _binding : FragmentBrowserFavoritesBinding? = null
    private val binding get() = _binding!!

    // ------ //

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBrowserFavoritesBinding.inflate(inflater, container, false).also {
            it.vm = viewModel
            it.lifecycleOwner = viewLifecycleOwner
        }

        binding.recyclerView.adapter = FavoriteSitesAdapter(viewLifecycleOwner).also {
            it.setOnClickItemListener { binding ->
                val site = binding.site ?: return@setOnClickItemListener
                viewModel.onClickItem(site, requireActivity(), this)
            }

            it.setOnLongLickItemListener { binding ->
                val site = binding.site ?: return@setOnLongLickItemListener
                viewModel.onLongClickItem(site, requireActivity(), this)
            }
        }

        binding.addButton.also { fab ->
            fab.size =
                if (browserActivity != null) FloatingActionButton.SIZE_MINI
                else FloatingActionButton.SIZE_NORMAL

            fab.setOnClickListener {
                viewModel.onClickAddButton(requireActivity(), this)
            }
        }

        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun scrollToTop() {
        binding.recyclerView.scrollToPosition(0)
    }
}
