package com.suihan74.satena.scenes.browser.favorites

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import com.suihan74.satena.R
import com.suihan74.satena.databinding.FragmentBrowserFavoritesBinding
import com.suihan74.satena.models.FavoriteSitesKey
import com.suihan74.satena.scenes.browser.BrowserActivity
import com.suihan74.satena.scenes.browser.BrowserViewModel
import com.suihan74.satena.scenes.preferences.favoriteSites.FavoriteSitesAdapter
import com.suihan74.utilities.SafeSharedPreferences
import com.suihan74.utilities.provideViewModel

/** お気に入りサイトを表示するタブ */
class FavoriteSitesFragment : Fragment() {
    companion object {
        fun createInstance() = FavoriteSitesFragment()
    }

    private val browserActivity : BrowserActivity
        get() = requireActivity() as BrowserActivity

    private val activityViewModel : BrowserViewModel
        get() = browserActivity.viewModel

    private val viewModel by lazy {
        provideViewModel(this) {
            val prefs = SafeSharedPreferences.create<FavoriteSitesKey>(requireContext())
            FavoriteSitesViewModel(prefs)
        }
    }

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

        binding.recyclerView.adapter = FavoriteSitesAdapter(viewLifecycleOwner).also {
            it.setOnClickItemListener { site ->
                activityViewModel.goAddress(site.url)
                browserActivity.closeDrawer()
            }
        }

        return binding.root
    }
}
