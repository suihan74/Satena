package com.suihan74.utilities.extensions

import android.os.SystemClock
import android.util.Log
import android.view.MotionEvent
import android.view.View
import androidx.lifecycle.findViewTreeLifecycleOwner
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.delay

/**
 * ボタン押下時のアニメーションを任意タイミングで再生する
 */
fun View.simulateRippleEffect(duration: Long = 250L, times: Int = 1, delay: Long = 0L) {
    runCatching {
        val lifecycleScope = findViewTreeLifecycleOwner()!!.lifecycleScope
        lifecycleScope.launchWhenResumed {
            delay(delay)
            SystemClock.uptimeMillis().let { now ->
                val pressEvent =
                    MotionEvent.obtain(
                        now, now, MotionEvent.ACTION_DOWN, width / 2f, height / 2f, 0
                    )
                dispatchTouchEvent(pressEvent)
            }

            lifecycleScope.launchWhenResumed {
                delay(duration)
                SystemClock.uptimeMillis().let { now ->
                    val cancelEvent =
                        MotionEvent.obtain(
                            now, now, MotionEvent.ACTION_CANCEL, width / 2f, height / 2f, 0
                        )
                    dispatchTouchEvent(cancelEvent)
                }
                if (times > 1) {
                    delay(duration)
                    simulateRippleEffect(duration, times - 1, 0L)
                }
            }
        }
    }.onFailure {
        Log.e("simulateRippleEffect", it.stackTraceToString())
    }
}
