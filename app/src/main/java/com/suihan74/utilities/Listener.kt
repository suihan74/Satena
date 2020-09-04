@file:Suppress("unused")

package com.suihan74.utilities

/** 成功時 */
typealias OnSuccess<ResultT> = (ResultT)->Unit

/** 失敗時 */
typealias OnError = (Throwable)->Unit

/** 終了時(成功/失敗に関わらず呼ぶ) */
typealias OnFinally = ()->Unit

/** 汎用リスナ */
typealias Listener<T> = (T)->Unit
