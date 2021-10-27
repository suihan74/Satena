package com.suihan74.satena

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities.NET_CAPABILITY_INTERNET
import android.net.NetworkCapabilities.NET_CAPABILITY_VALIDATED
import androidx.annotation.RequiresApi
import androidx.lifecycle.LiveData
import com.suihan74.utilities.SingleUpdateMutableLiveData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

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

    private var previousState = State.INITIALIZING

    private val networks = HashSet<String>()

    private val mutex = Mutex()

    // ------ //

    @RequiresApi(23)
    val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            SatenaApplication.instance.coroutineScope.launch {
                mutex.withLock {
                    val nc = context.getSystemService(ConnectivityManager::class.java)
                        .getNetworkCapabilities(network)
                    if (
                        nc != null &&
                        nc.hasCapability(NET_CAPABILITY_INTERNET) &&
                        nc.hasCapability(NET_CAPABILITY_VALIDATED)
                    ) {
                        networks.add(network.toString())
                    }
                    checkConnection()
                }
            }
        }
        override fun onLost(network: Network) {
            SatenaApplication.instance.coroutineScope.launch {
                mutex.withLock {
                    networks.remove(network.toString())
                    checkConnection()
                }
            }
        }
    }

    private suspend fun checkConnection() = withContext(Dispatchers.Default) {
        val state =
            if (networks.isEmpty()) State.DISCONNECTED
            else State.CONNECTED

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
