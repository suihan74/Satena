package com.suihan74.satena

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.util.Log
import com.suihan74.utilities.AccountLoader
import kotlinx.coroutines.*

class ConnectivityReceiver : BroadcastReceiver() {
    companion object {
        lateinit var instance : ConnectivityReceiver
    }

    init {
        instance = this
    }

    private var mPreviousState : Boolean? = null

    private var mActivatingListener : (()->Unit)? = null

    private var mActivatedListener : (()->Unit)? = null
    private var mDeactivatedListener : (()->Unit)? = null

    private var mJob : Job? = null

    override fun onReceive(context: Context?, intent: Intent?) {
        @Suppress("DEPRECATION")
        if (context == null || intent?.action != ConnectivityManager.CONNECTIVITY_ACTION) return

        val cm = context.applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val networkInfo = cm.activeNetworkInfo
        val isConnected = networkInfo?.isConnected ?: false

        if (isConnected && mPreviousState == false) {
            if (mJob == null) {
                mJob = GlobalScope.launch(SupervisorJob() + Dispatchers.Main) {
                    SatenaApplication.instance.currentActivity?.showProgressBar()
                    mActivatingListener?.invoke()

                    var success = false
                    for (i in 0 until 20) {
                        try {
                            AccountLoader.signInAccounts(context, reSignIn = false)
                            SatenaApplication.instance.startNotificationService()
                            success = true
                            break
                        }
                        catch (e: Exception) {
                            Log.d("Connection", Log.getStackTraceString(e))
                            delay(100)
                        }
                    }

                    if (!success) {
                        Log.e("Connection", "failed to automatically sign in")
                    }

                    mActivatedListener?.invoke()
                    SatenaApplication.instance.currentActivity?.hideProgressBar()
                    mJob = null
                }
            }
            mPreviousState = isConnected
        }
        else if (!isConnected && mPreviousState == true) {
            mDeactivatedListener?.invoke()
            mPreviousState = isConnected
        }
        else if (mPreviousState == null) {
            mPreviousState = isConnected
        }
    }

    fun setConnectionActivatingListener(listener: (()->Unit)?) {
        mActivatingListener = listener
    }

    fun setConnectionActivatedListener(listener: (()->Unit)?) {
        mActivatedListener = listener
    }

    fun setConnectionDeactivatedListener(listener: (()->Unit)?) {
        mDeactivatedListener = listener
    }
}
