package com.suihan74.utilities.extensions

import androidx.lifecycle.Observer

/**
 * 自身をコンテキストオブジェクトとして渡して`onChanged()`を呼ぶ`Observer<T>`を作成する
 *
 * オブザーバの内側で自身を通知対象から削除する用途での使用を想定している
 */
fun <T> scopedObserver(onChanged: Observer<T>.(T)->Unit) : Observer<T> {
    lateinit var observer: Observer<T>
    observer = Observer<T> {
        onChanged(observer, it)
    }
    return observer
}

/**
 * 初回の呼び出しを無視する`Observer`
 */
fun <T> observerForOnlyUpdates(onChanged: (T)->Unit) : Observer<T> {
    var initialized = false
    val body : (T)->Unit = {
        if (initialized) {
            onChanged(it)
        }
        initialized = true
    }
    return Observer(body)
}
