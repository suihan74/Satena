package com.suihan74.satena.scenes.entries2

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProviders
import com.suihan74.HatenaLib.HatenaClient
import com.suihan74.satena.R
import com.suihan74.utilities.SafeSharedPreferences

class EntriesActivity : AppCompatActivity() {
    private lateinit var viewModel : EntriesViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        viewModel =
            if (savedInstanceState == null) {
                val factory = EntriesViewModel.Factory(
                    EntriesRepository(
                        client = HatenaClient,
                        prefs = SafeSharedPreferences.create(this),
                        historyPrefs = SafeSharedPreferences.create(this)
                    )
                )
                ViewModelProviders.of(this, factory)[EntriesViewModel::class.java]
            }
            else {
                ViewModelProviders.of(this)[EntriesViewModel::class.java]
            }
    }
}
