package com.suihan74.satena

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import androidx.annotation.RequiresApi
import androidx.annotation.WorkerThread
import androidx.lifecycle.LiveData
import com.suihan74.utilities.SingleUpdateMutableLiveData

class NetworkReceiver(private val context: Context) {
    enum class State {
        /** 初期化前 */
        INITIALIZING,
        /** 接続済み */
        CONNECTED,
        /** 切断 */
        DISCONNECTED,
    }

    private val mState = SingleUpdateMutableLiveData<State>()
    val state : LiveData<State>  = mState

    var previousState = State.INITIALIZING
        private set

    @RequiresApi(23)
    val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) = checkConnection()
        override fun onLost(network: Network) = checkConnection()
    }

    @RequiresApi(23)
    @WorkerThread
    private fun checkConnection() {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val activeNetworks = cm.allNetworks
            .mapNotNull { cm.getNetworkCapabilities((it)) }
            .filter {
                it.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                it.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
            }

        // 「wifi,lteなど問わず何か一つでも通信手段が確立されている状態」をCONNECTEDと判断する
        val state =
            if (activeNetworks.isNotEmpty()) State.CONNECTED
            else State.DISCONNECTED

        if (mState.value != state) {
            // 初回の状態を通知しないようにする
            if (previousState == State.INITIALIZING) {
                previousState = state
                // mStateはnullのまま
            }
            else {
                previousState = mState.value ?: previousState
                mState.postValue(state)
            }
        }
    }
}
