package com.suihan74.satena.scenes.splash

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import com.suihan74.hatenaLib.HatenaClient
import com.suihan74.satena.R
import com.suihan74.satena.databinding.ActivitySplashBinding
import com.suihan74.utilities.AccountLoader
import com.suihan74.utilities.MastodonClientHolder
import com.suihan74.utilities.extensions.showToast
import com.suihan74.utilities.provideViewModel

class SplashActivity : AppCompatActivity() {
    private val viewModel : ViewModel by lazy {
        provideViewModel(this) {
            val repository = Repository(
                AccountLoader(this, HatenaClient, MastodonClientHolder)
            )
            ViewModel(this, repository)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        DataBindingUtil.setContentView<ActivitySplashBinding>(this, R.layout.activity_splash).apply {
            lifecycleOwner = this@SplashActivity
            vm = viewModel
        }

        viewModel.start { e -> when (e) {
            is AccountLoader.HatenaSignInException ->
                showToast(R.string.msg_hatena_sign_in_failed)

            is AccountLoader.MastodonSignInException ->
                showToast(R.string.msg_auth_mastodon_failed)
        }}
    }
}
