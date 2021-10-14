package com.suihan74.satena.scenes.splash

import androidx.core.app.ActivityCompat
import androidx.core.app.ActivityOptionsCompat
import androidx.lifecycle.ViewModel
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.suihan74.satena.R
import com.suihan74.satena.SatenaApplication
import com.suihan74.satena.models.NoticesKeyMigration
import com.suihan74.utilities.AccountLoader
import com.suihan74.utilities.extensions.ContextExtensions.showToast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ViewModel(
    val repository: Repository
) : ViewModel() {
    /** バージョン名 */
    val appVersion : String by lazy { SatenaApplication.instance.versionName }

    suspend fun start(activity: SplashActivity) = withContext(Dispatchers.Main) {
        runCatching {
            NoticesKeyMigration.check(activity)
        }

        val intent = repository.createIntent(activity) { e -> when (e) {
            is AccountLoader.HatenaSignInException ->
                SatenaApplication.instance.showToast(R.string.msg_hatena_sign_in_failed)

            is AccountLoader.MastodonSignInException ->
                SatenaApplication.instance.showToast(R.string.msg_auth_mastodon_failed)
        } }

        try {
            val optionsCompat = ActivityOptionsCompat.makeCustomAnimation(
                activity,
                android.R.anim.fade_in, android.R.anim.fade_out
            )

            ActivityCompat.startActivity(activity, intent, optionsCompat.toBundle())
        }
        catch (e: Throwable) {
            FirebaseCrashlytics.getInstance().recordException(e)
            activity.startActivity(intent)
        }
    }
}
