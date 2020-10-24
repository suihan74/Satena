package com.suihan74.utilities

import androidx.annotation.MainThread
import androidx.lifecycle.MutableLiveData

/**
 * 現在値と同じ値のセット時に通知を発生しないMutableLiveData
 */
open class SingleUpdateMutableLiveData<T> : MutableLiveData<T> {

    constructor(initialValue: T, selector: ((T?)->Any?) = { it }) : super(initialValue) {
        this.selector = selector
    }

    constructor(selector: ((T?)->Any?) = { it }) : super() {
        this.selector = selector
    }

    private val selector: ((T?)->Any?)

    /** 更新可能か調べる */
    fun checkUpdatable(other: T?, currentValue: T? = this.value) : Boolean =
        selector(other) != selector(currentValue)

    @MainThread
    final override fun setValue(value: T?) {
        if (checkUpdatable(value)) {
            super.setValue(value)
        }
    }

    final override fun postValue(value: T?) {
        if (checkUpdatable(value)) {
            super.postValue(value)
        }
    }
}

