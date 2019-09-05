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

    abstract fun getProgressBarId() : Int?
    abstract fun getProgressBackgroundId() : Int?

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
        val progressBar = getProgressBarId()?.let {
            findViewById<ProgressBar>(it)?.apply { visibility = View.VISIBLE }
        }
        val background = if (clickGuard) {
            getProgressBackgroundId()?.let {
                findViewById<View>(it)?.apply { visibility = View.VISIBLE }
            }
        }
        else null

        showAction?.invoke(progressBar, background)
    }

    fun hideProgressBar() {
        val progressBar = getProgressBarId()?.let {
            findViewById<ProgressBar>(it)
        }
        val background = getProgressBackgroundId()?.let {
            findViewById<View>(it)
        }

        if (hideAction == null) {
            progressBar?.visibility = View.GONE
            background?.visibility = View.GONE
        }
        else {
            hideAction!!(progressBar, background)
        }
    }
}
