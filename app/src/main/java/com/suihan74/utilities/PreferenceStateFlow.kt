package com.suihan74.utilities

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

/** 設定と相互作用するStateFlow */
inline fun <KeyT, reified ValueT> preferenceStateFlow(
    prefs: SafeSharedPreferences<KeyT>,
    key: KeyT,
    coroutineScope: CoroutineScope,
    noinline action: (suspend (ValueT)->Unit)? = null
) : MutableStateFlow<ValueT> where KeyT : SafeSharedPreferences.Key, KeyT : Enum<KeyT> =
    MutableStateFlow(prefs.get(key) as ValueT).apply {
        onEach {
            prefs.edit { put(key, it) }
            action?.invoke(it)
        }.launchIn(coroutineScope)
    }

/** 設定と相互作用するStateFlow（Enum値用） */
inline fun <KeyT, reified ValueT> preferenceEnumStateFlow(
    prefs: SafeSharedPreferences<KeyT>,
    key: KeyT,
    noinline enumToInt: (ValueT)->Int,
    noinline intToEnum: (Int)->ValueT,
    coroutineScope: CoroutineScope,
    noinline action: (suspend (ValueT)->Unit)? = null
) : MutableStateFlow<ValueT>
where KeyT : SafeSharedPreferences.Key, KeyT : Enum<KeyT>, ValueT : Enum<ValueT> =
    MutableStateFlow(intToEnum(prefs.getInt(key))).apply {
        onEach {
            prefs.edit { putInt(key, enumToInt(it)) }
            action?.invoke(it)
        }.launchIn(coroutineScope)
    }

/** 設定と相互作用するStateFlow（任意型変換用） */
inline fun <KeyT, reified PValueT, reified ValueT> preferenceStateFlow(
    prefs: SafeSharedPreferences<KeyT>,
    key: KeyT,
    noinline fromValue: (ValueT)->PValueT,
    noinline toValue: (PValueT)->ValueT,
    coroutineScope: CoroutineScope,
    noinline action: (suspend (ValueT)->Unit)? = null
) : MutableStateFlow<ValueT>
where KeyT : SafeSharedPreferences.Key, KeyT : Enum<KeyT> =
    MutableStateFlow(toValue(prefs.get(key))).apply {
        onEach {
            prefs.edit { put(key, fromValue(it)) }
            action?.invoke(it)
        }.launchIn(coroutineScope)
    }
