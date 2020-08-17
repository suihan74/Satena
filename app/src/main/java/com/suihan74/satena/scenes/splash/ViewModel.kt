package com.suihan74.satena.scenes.splash

import android.content.Context
import androidx.core.app.ActivityOptionsCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.suihan74.satena.SatenaApplication
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ViewModel(
    private val context: Context,
    private val repository: Repository
) : ViewModel() {
    /** バージョン名 */
    val appVersion : String by lazy { SatenaApplication.instance.versionName }

    fun start(onError: OnError? = null) = viewModelScope.launch(Dispatchers.Default) {
        val intent = repository.createIntent(context, onError)

        withContext(Dispatchers.Main) {
            context.startActivity(
                intent, ActivityOptionsCompat.makeCustomAnimation(
                    context,
                    android.R.anim.fade_in, android.R.anim.fade_out
                ).toBundle()
            )
        }
    }

    class Factory(
        private val context: Context,
        private val repository: Repository
    ) : ViewModelProvider.NewInstanceFactory() {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel?> create(modelClass: Class<T>) : T =
            ViewModel(context, repository) as T
    }
}
