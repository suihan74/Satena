package com.suihan74.utilities

import androidx.lifecycle.MutableLiveData

/**
 * 現在値と同じ値のセット時に通知を発生しないMutableLiveData
 */
class SingleUpdateMutableLiveData<T>(
    initialValue: T? = null,
    private val selector: ((T?)->Any?) = { it }
) : MutableLiveData<T>(initialValue) {
    override fun setValue(value: T?) {
        if (selector(value) != selector(this.value)) {
            super.setValue(value)
        }
    }

    override fun postValue(value: T?) {
        if (selector(value) != selector(this.value)) {
            super.postValue(value)
        }
    }
}
