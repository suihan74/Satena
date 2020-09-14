package com.suihan74.satena.scenes.preferences.pages

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.BindingAdapter
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.suihan74.satena.R
import com.suihan74.satena.databinding.FragmentPreferencesFavoriteSitesBinding
import com.suihan74.satena.models.FavoriteSite
import com.suihan74.satena.models.FavoriteSitesKey
import com.suihan74.satena.scenes.preferences.PreferencesFragmentBase
import com.suihan74.satena.scenes.preferences.favoriteSites.FavoriteSitesAdapter
import com.suihan74.utilities.SafeSharedPreferences
import com.suihan74.utilities.alsoAs
import com.suihan74.utilities.bindings.setDivider

class PreferencesFavoriteSitesFragment : PreferencesFragmentBase() {
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

    private val viewModel: PreferencesFavoriteSitesViewModel by lazy {
        val prefs = SafeSharedPreferences.create<FavoriteSitesKey>(context)
        val factory = PreferencesFavoriteSitesViewModel.Factory(prefs)
        ViewModelProvider(this, factory)[PreferencesFavoriteSitesViewModel::class.java]
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val binding = DataBindingUtil.inflate<FragmentPreferencesFavoriteSitesBinding>(
            inflater,
            R.layout.fragment_preferences_favorite_sites,
            container,
            false
        ).apply {
            vm = viewModel
            lifecycleOwner = viewLifecycleOwner
        }

        binding.recyclerView.also {
            it.adapter = FavoriteSitesAdapter(viewLifecycleOwner)
            it.layoutManager = LinearLayoutManager(requireContext())
            it.setDivider(R.drawable.recycler_view_item_divider)
            it.setHasFixedSize(true)
        }

        return binding.root
    }
}
