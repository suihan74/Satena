package com.suihan74.satena.scenes.preferences.pages

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.BindingAdapter
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import com.suihan74.satena.R
import com.suihan74.satena.databinding.FragmentPreferencesFavoriteSitesBinding
import com.suihan74.satena.models.FavoriteSite
import com.suihan74.satena.scenes.browser.BrowserActivity
import com.suihan74.satena.scenes.browser.BrowserViewModel
import com.suihan74.satena.scenes.browser.favorites.FavoriteSitesRepository
import com.suihan74.satena.scenes.preferences.PreferencesActivity
import com.suihan74.satena.scenes.preferences.favoriteSites.FavoriteSitesAdapter
import com.suihan74.utilities.SafeSharedPreferences
import com.suihan74.utilities.ScrollableToTop
import com.suihan74.utilities.extensions.alsoAs
import com.suihan74.utilities.provideViewModel

class PreferencesFavoriteSitesFragment : Fragment(), ScrollableToTop {
    companion object {
        fun createInstance() = PreferencesFavoriteSitesFragment()

        @BindingAdapter("items")
        @JvmStatic
        fun setFavoriteSites(recyclerView: RecyclerView, items: List<FavoriteSite>) {
            recyclerView.adapter.alsoAs<FavoriteSitesAdapter> { adapter ->
                adapter.setItems(items)
            }
        }
    }

    private val preferencesActivity : PreferencesActivity?
        get() = requireActivity() as? PreferencesActivity

    private val browserActivity : BrowserActivity?
        get() = requireActivity() as? BrowserActivity

    private val browserViewModel : BrowserViewModel?
        get() = browserActivity?.viewModel

    private lateinit var binding: FragmentPreferencesFavoriteSitesBinding

    private val viewModel: PreferencesFavoriteSitesViewModel by lazy {
        provideViewModel(this) {
            val prefs = SafeSharedPreferences.create<FavoriteSitesKey>(context)
            PreferencesFavoriteSitesViewModel(prefs)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = DataBindingUtil.inflate<FragmentPreferencesFavoriteSitesBinding>(
            inflater,
            R.layout.fragment_preferences_favorite_sites,
            container,
            false
        ).apply {
            vm = viewModel
            lifecycleOwner = viewLifecycleOwner
        }

        binding.recyclerView.also {
            it.setHasFixedSize(true)
            it.adapter = FavoriteSitesAdapter(viewLifecycleOwner).apply {
                setOnClickItemListener { binding ->
                    val site = binding.site ?: return@setOnClickItemListener
                    viewModel.openMenuDialog(requireActivity(), site, childFragmentManager)
                }
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
        binding.recyclerView.scrollToPosition(0)
    }
}
