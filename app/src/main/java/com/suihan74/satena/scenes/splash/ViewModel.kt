package com.suihan74.satena.scenes.splash

import androidx.core.app.ActivityCompat
import androidx.core.app.ActivityOptionsCompat
import androidx.lifecycle.ViewModel
import com.suihan74.satena.R
import com.suihan74.satena.SatenaApplication
import com.suihan74.utilities.AccountLoader
import com.suihan74.utilities.extensions.showToast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ViewModel(
    val repository: Repository
) : ViewModel() {
    /** バージョン名 */
    val appVersion : String by lazy { SatenaApplication.instance.versionName }

    suspend fun start(activity: SplashActivity) = withContext(Dispatchers.Main) {
        val intent = repository.createIntent(activity) { e -> when (e) {
            is AccountLoader.HatenaSignInException ->
                activity.showToast(R.string.msg_hatena_sign_in_failed)

            is AccountLoader.MastodonSignInException ->
                activity.showToast(R.string.msg_auth_mastodon_failed)
        } }

        try {
            val optionsCompat = ActivityOptionsCompat.makeCustomAnimation(
                activity,
                android.R.anim.fade_in, android.R.anim.fade_out
            )

            ActivityCompat.startActivity(activity, intent, optionsCompat.toBundle())
        }
        catch (e: Throwable) {
            activity.startActivity(intent)
        }
    }
}
