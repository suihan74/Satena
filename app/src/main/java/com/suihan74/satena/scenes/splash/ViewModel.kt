package com.suihan74.satena.scenes.splash

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
    val appVersion : String = SatenaApplication.instance.versionName

    suspend fun start(activity: SplashActivity) = withContext(Dispatchers.Main) {
        runCatching {
            NoticesKeyMigration.check(activity)
        }

        val intent =
            repository.createIntent(activity) { e ->
                when (e) {
                    is AccountLoader.HatenaSignInException ->
                        SatenaApplication.instance.showToast(R.string.msg_hatena_sign_in_failed)

                    is AccountLoader.MastodonSignInException ->
                        SatenaApplication.instance.showToast(R.string.msg_auth_mastodon_failed)
                }
            }

        runCatching {
            activity.startActivity(intent)
            activity.overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        }.onFailure {
            FirebaseCrashlytics.getInstance().recordException(it)
            activity.startActivity(intent)
        }
    }
}
