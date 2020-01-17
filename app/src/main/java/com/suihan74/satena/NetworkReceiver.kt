package com.suihan74.satena

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import androidx.annotation.RequiresApi
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData

class NetworkReceiver(private val context: Context) {
    private val mState by lazy { MutableLiveData<Boolean>() }
    val state : LiveData<Boolean>
        get() = mState

    var previousState : Boolean? = null
        private set

    @RequiresApi(23)
    val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network?) = checkConnection()
        override fun onLost(network: Network?) = checkConnection()
    }

    @RequiresApi(23)
    private fun checkConnection() {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val activeNetworks =cm.allNetworks
            .mapNotNull { cm.getNetworkCapabilities((it)) }
            .filter {
                it.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                it.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
            }

        val isConnected = activeNetworks.isNotEmpty()
        if (mState.value != isConnected) {
            previousState = !isConnected
            mState.postValue(isConnected)
        }
    }
}
