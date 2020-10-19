package com.suihan74.satena.scenes.preferences.favoriteSites

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.suihan74.hatenaLib.HatenaClient
import com.suihan74.satena.R
import com.suihan74.satena.databinding.FragmentBrowserFavoritesBinding
import com.suihan74.satena.scenes.browser.BrowserActivity
import com.suihan74.satena.scenes.browser.BrowserViewModel
import com.suihan74.satena.scenes.browser.favorites.FavoriteSitesActionsImplForBrowser
import com.suihan74.satena.scenes.preferences.PreferencesActivity
import com.suihan74.utilities.SafeSharedPreferences
import com.suihan74.utilities.ScrollableToTop
import com.suihan74.utilities.provideViewModel

/** お気に入りサイトを表示するタブ */
class FavoriteSitesFragment :
    Fragment(),
    ScrollableToTop
{
    companion object {
        fun createInstance() = FavoriteSitesFragment()
    }

    private val browserActivity : BrowserActivity?
        get() = requireActivity() as? BrowserActivity

    private val browserViewModel : BrowserViewModel?
        get() = browserActivity?.viewModel

    private val preferencesActivity : PreferencesActivity?
        get() = requireActivity() as? PreferencesActivity

    val viewModel by lazy {
        provideViewModel(this) {
            val repository = browserViewModel?.favoriteSitesRepo ?:
                FavoriteSitesRepository(
                    SafeSharedPreferences.create(requireContext()),
                    HatenaClient
                )

            val actionsImpl =
                if (browserActivity != null) FavoriteSitesActionsImplForBrowser()
                else FavoriteSitesActionsImplForPreferences()

            FavoriteSitesViewModel(repository, actionsImpl)
        }
    }

    private var binding : FragmentBrowserFavoritesBinding? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val binding = DataBindingUtil.inflate<FragmentBrowserFavoritesBinding>(
            inflater,
            R.layout.fragment_browser_favorites,
            container,
            false
        ).also {
            it.vm = viewModel
            it.lifecycleOwner = viewLifecycleOwner
        }
        this.binding = binding

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

    override fun onResume() {
        super.onResume()
        // 設定画面では、他のタブが生成したオプションメニューがあったら消す
        if (preferencesActivity != null) {
            setHasOptionsMenu(false)
        }
    }

    override fun scrollToTop() {
        binding?.recyclerView?.scrollToPosition(0)
    }
}
