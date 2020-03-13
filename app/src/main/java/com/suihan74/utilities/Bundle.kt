package com.suihan74.utilities

import android.os.Bundle
import kotlin.reflect.KClass

/** Enum<T>::classから直接valuesを取得する */
@Suppress("UNCHECKED_CAST")
inline fun <reified T : Enum<T>> KClass<T>.getEnumConstants() =
    Class.forName(T::class.qualifiedName!!).enumConstants as Array<out T>

// --------- //

/** BundleにEnumをセットする */
inline fun <reified T : Enum<T>> Bundle.putEnum(key: String, value: T) {
    putInt(key, value.ordinal)
}

/** BundleからEnumを取得する(失敗時null) */
inline fun <reified T : Enum<T>> Bundle.getEnum(key: String) : T? =
    try {
        (get(key) as? Int)?.let { ordinal ->
            T::class.getEnumConstants().getOrNull(ordinal)
        }
    }
    catch (e: Throwable) {
        null
    }

/** BundleからEnumを取得する(失敗時デフォルト値) */
inline fun <reified T : Enum<T>> Bundle.getEnum(key: String, defaultValue: T) : T =
    getEnum<T>(key) ?: defaultValue

// --------- //

/** BundleにEnumをセットする(ordinal以外を使用) */
inline fun <reified T : Enum<T>> Bundle.putEnum(key: String, value: T, selector: (T)->Int) {
    putInt(key, selector(value))
}

/** BundleからEnumを取得する(ordinal以外を使用, 失敗時null) */
inline fun <reified T : Enum<T>> Bundle.getEnum(key: String, selector: (T)->Int) : T? =
    try {
        (get(key) as? Int)?.let { intValue ->
            T::class.getEnumConstants().firstOrNull { selector(it) == intValue }
        }
    }
    catch (e: Throwable) {
        null
    }

/** BundleからEnumを取得する(ordinal以外を使用, 失敗時デフォルト値) */
inline fun <reified T : Enum<T>> Bundle.getEnum(key: String, defaultValue: T, selector: (T) -> Int) : T =
    getEnum(key, selector) ?: defaultValue
