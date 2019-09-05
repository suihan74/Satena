package com.suihan74.utilities

import android.support.v4.app.Fragment
import kotlinx.coroutines.*
import kotlin.coroutines.CoroutineContext

abstract class CoroutineScopeFragment(
    private val defaultDispatcher: CoroutineDispatcher = Dispatchers.Default
): Fragment(), CoroutineScope {

    private val mJob: Job = SupervisorJob()

    override val coroutineContext: CoroutineContext
        get() = mJob + defaultDispatcher

    override fun onDestroy() {
        super.onDestroy()
        mJob.cancel()
    }
}
