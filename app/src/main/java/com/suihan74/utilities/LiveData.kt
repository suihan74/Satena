package com.suihan74.utilities

import androidx.annotation.MainThread
import androidx.lifecycle.MutableLiveData

/**
 * 現在値と同じ値のセット時に通知を発生しないMutableLiveData
 */
class SingleUpdateMutableLiveData<T>(
    initialValue: T? = null,
    private val selector: ((T?)->Any?) = { it }
) : MutableLiveData<T>(initialValue) {

    /** 更新可能か調べる */
    fun checkUpdatable(other: T?, currentValue: T? = this.value) : Boolean =
        selector(other) != selector(currentValue)

    @MainThread
    override fun setValue(value: T?) {
        if (checkUpdatable(value)) {
            super.setValue(value)
        }
    }

    override fun postValue(value: T?) {
        if (checkUpdatable(value)) {
            super.postValue(value)
        }
    }
}

