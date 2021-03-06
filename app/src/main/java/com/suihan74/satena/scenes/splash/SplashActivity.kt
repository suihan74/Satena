package com.suihan74.satena.scenes.splash

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.suihan74.satena.R
import com.suihan74.satena.SatenaApplication
import com.suihan74.satena.databinding.ActivitySplashBinding
import com.suihan74.utilities.lazyProvideViewModel
import kotlinx.coroutines.launch

class SplashActivity : AppCompatActivity() {
    private val viewModel by lazyProvideViewModel {
        val repository = Repository(
            SatenaApplication.instance.accountLoader
        )
        ViewModel(repository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (viewModel.repository.needToLoad) {
            val binding = ActivitySplashBinding.inflate(layoutInflater).also {
                it.vm = viewModel
                it.lifecycleOwner = this
            }
            setContentView(binding.root)
        }

        lifecycleScope.launch {
            viewModel.start(this@SplashActivity)
        }
    }
}
