package com.suihan74.satena.scenes.splash

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.lifecycle.lifecycleScope
import com.suihan74.satena.SatenaApplication
import com.suihan74.satena.databinding.ActivitySplashBinding
import com.suihan74.satena.models.PreferenceKey
import com.suihan74.utilities.SafeSharedPreferences
import com.suihan74.utilities.extensions.ContextExtensions.showToast
import com.suihan74.utilities.lazyProvideViewModel
import kotlinx.coroutines.launch

@SuppressLint("CustomSplashScreen")
class SplashActivity : AppCompatActivity() {
    private val viewModel by lazyProvideViewModel {
        val repository = Repository(
            SatenaApplication.instance.accountLoader
        )
        ViewModel(repository)
    }

    private val requestNotificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { result ->
        lifecycleScope.launch {
            if (result) {
                showToast("通知送信が許可されました")
            }
            else {
                showToast("Android側で通知が拒否されています")
            }
            viewModel.start(this@SplashActivity)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val prefs = SafeSharedPreferences.create<PreferenceKey>(this)

        if (viewModel.repository.needToLoad) {
            val binding = ActivitySplashBinding.inflate(layoutInflater).also {
                it.vm = viewModel
                it.lifecycleOwner = this
            }
            setContentView(binding.root)
        }

        if (prefs.getBoolean(PreferenceKey.BACKGROUND_CHECKING_NOTICES)
            && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
            && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        else {
            lifecycleScope.launch {
                viewModel.start(this@SplashActivity)
            }
        }
    }
}
