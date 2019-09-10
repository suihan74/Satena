package com.suihan74.satena.activities

import android.view.View
import android.widget.ProgressBar
import com.suihan74.satena.SatenaApplication
import com.suihan74.utilities.FragmentContainerActivity
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlin.coroutines.CoroutineContext

abstract class ActivityBase(
    private val defaultDispatcher: CoroutineDispatcher = Dispatchers.Default
) : FragmentContainerActivity(), CoroutineScope {

    protected abstract val progressBarId : Int?
    protected abstract val progressBackgroundId : Int?

    private var showAction: ((ProgressBar?, View?)->Unit)? = null
    private var hideAction: ((ProgressBar?, View?)->Unit)? = null

    private val job = SupervisorJob()
    override val coroutineContext: CoroutineContext
        get() = job + defaultDispatcher

    override fun setContentView(layoutResID: Int) {
        super.setContentView(layoutResID)
        SatenaApplication.instance.currentActivity = this
    }

    protected fun setShowingProgressBarAction(action: (progressBar: ProgressBar?, background: View?)->Unit) {
        showAction = action
    }
    protected fun setHidingProgressBarAction(action: (progressBar: ProgressBar?, background: View?)->Unit) {
        hideAction = action
    }

    fun showProgressBar(clickGuard: Boolean = true) {
        val progressBar = progressBarId?.let {
            findViewById<ProgressBar>(it).apply { visibility = View.VISIBLE }
        }
        val background = if (clickGuard) {
            progressBackgroundId?.let {
                findViewById<View>(it).apply { visibility = View.VISIBLE }
            }
        } else null

        showAction?.invoke(progressBar, background)
    }

    fun hideProgressBar() {
        val progressBar = progressBarId?.let {
            findViewById<ProgressBar>(it)
        }
        val background = progressBackgroundId?.let {
            findViewById<View>(it)
        }

        if (hideAction == null) {
            progressBar?.visibility = View.INVISIBLE
            background?.visibility = View.INVISIBLE
        }
        else {
            hideAction!!.invoke(progressBar, background)
        }
    }
}
