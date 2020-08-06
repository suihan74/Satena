package com.suihan74.utilities

import android.content.Intent
import android.os.Bundle
import com.google.gson.FieldNamingPolicy
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import com.suihan74.hatenaLib.BooleanDeserializer
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
inline fun <reified T : Enum<T>> Bundle.selectEnum(key: String, selector: (T)->Int) : T? =
    try {
        (get(key) as? Int)?.let { intValue ->
            T::class.getEnumConstants().firstOrNull { selector(it) == intValue }
        }
    }
    catch (e: Throwable) {
        null
    }

/** BundleからEnumを取得する(ordinal以外を使用, 失敗時デフォルト値) */
inline fun <reified T : Enum<T>> Bundle.selectEnum(key: String, defaultValue: T, selector: (T)->Int) : T =
    selectEnum(key, selector) ?: defaultValue

// --------- //

fun Intent.putObjectExtra(key: String, value: Any?) {
    this.putExtras((this.extras ?: Bundle()).apply {
        putObject(key, value)
    })
}

inline fun <reified T> Intent.getObjectExtra(key: String) : T? {
    return this.extras?.getObject(key)
}

/** Bundle#putObject(), Bundle#getObject()で使用するGsonインスタンス */
val Bundle.gson : Gson by lazy {
    GsonBuilder()
        .serializeNulls()
        .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
        .registerTypeAdapter(Boolean::class.java, BooleanDeserializer())
        .create()
}

/** シリアライズしたオブジェクトをBundleに渡す */
fun Bundle.putObject(key: String, value: Any?) {
    putString(key, this.gson.toJson(value))
}

/** keyに対応する文字列をT型にデシリアライズして返す */
inline fun <reified T> Bundle.getObject(key: String) : T? {
    val json = getString(key) ?: return null
    return try {
        this.gson.fromJson<T>(json, object : TypeToken<T>() {}.type)
    }
    catch (e: Throwable) {
        null
    }
}

