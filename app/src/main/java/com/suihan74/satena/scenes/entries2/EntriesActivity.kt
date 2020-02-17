package com.suihan74.satena.scenes.entries2

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.BindingAdapter
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.suihan74.HatenaLib.HatenaClient
import com.suihan74.satena.R
import com.suihan74.satena.databinding.ActivityEntries2Binding
import com.suihan74.satena.models.Category
import com.suihan74.satena.models.PreferenceKey
import com.suihan74.utilities.SafeSharedPreferences
import kotlinx.android.synthetic.main.activity_entries2.*

class EntriesActivity : AppCompatActivity() {
    private lateinit var viewModel : EntriesViewModel
    private lateinit var binding : ActivityEntries2Binding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val prefs = SafeSharedPreferences.create<PreferenceKey>(this)
        setTheme(
            if (prefs.getBoolean(PreferenceKey.DARK_THEME)) R.style.AppTheme_Dark
            else R.style.AppTheme_Light
        )

        viewModel =
            if (savedInstanceState == null) {
                val factory = EntriesViewModel.Factory(
                    EntriesRepository(
                        client = HatenaClient,
                        prefs = prefs,
                        historyPrefs = SafeSharedPreferences.create(this)
                    )
                )
                ViewModelProviders.of(this, factory)[EntriesViewModel::class.java]
            }
            else {
                ViewModelProviders.of(this)[EntriesViewModel::class.java]
            }

        binding = DataBindingUtil.setContentView<ActivityEntries2Binding>(
            this,
            R.layout.activity_entries2
        ).apply {
            lifecycleOwner = this@EntriesActivity
            vm = viewModel
        }

        val categoriesAdapter = CategoriesAdapter()
        categories_list.apply {
            adapter = categoriesAdapter
            layoutManager = LinearLayoutManager(context)
        }
    }
}

@BindingAdapter("bind:viewmodels")
fun RecyclerView.setViewModels(categories: List<Category>?) {
    if (categories != null) {
        val adapter = this.adapter as CategoriesAdapter
        adapter.submitList(categories)
    }
}
