package com.suihan74.satena

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.util.Log
import com.suihan74.HatenaLib.HatenaClient
import com.suihan74.utilities.AccountLoader
import com.suihan74.utilities.MastodonClientHolder
import kotlinx.coroutines.*

class ConnectivityReceiver : BroadcastReceiver() {
    companion object {
        private var mActivatingListener : (()->Unit)? = null
        private var mActivatedListener : (()->Unit)? = null
        private var mDeactivatedListener : (()->Unit)? = null

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

    private var mPreviousState : Boolean? = null

    private var mJob : Job? = null

    @SuppressLint("UnsafeProtectedBroadcastReceiver")
    override fun onReceive(context: Context?, intent: Intent?) {
        @Suppress("DEPRECATION")
        if (context == null || intent?.action != ConnectivityManager.CONNECTIVITY_ACTION) return

        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val networkInfo = cm.activeNetworkInfo
        val isConnected = networkInfo?.isConnected ?: false

        if (isConnected && mPreviousState == false) {
            if (mJob == null) {
                mJob = GlobalScope.launch(SupervisorJob() + Dispatchers.Main) {
                    SatenaApplication.instance.currentActivity?.showProgressBar()
                    mActivatingListener?.invoke()

                    val accountLoader = AccountLoader(
                        context,
                        HatenaClient,
                        MastodonClientHolder
                    )

                    var success = false
                    for (i in 0 until 20) {
                        try {
                            accountLoader.signInAccounts()
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
}
