@file:Suppress("unused")

package com.suihan74.utilities

import androidx.fragment.app.DialogFragment

/** 成功時 */
typealias OnSuccess<ResultT> = (ResultT)->Unit

/** 失敗時 */
typealias OnError = (Throwable)->Unit

/** 終了時(成功/失敗に関わらず呼ぶ) */
typealias OnFinally = ()->Unit

/** 汎用リスナ */
typealias Listener<T> = (T)->Unit

/** 汎用リスナ(suspend関数) */
typealias SuspendListener<T> = suspend (T)->Unit

/** 処理が成功したかを返すリスナ */
typealias Switcher<T> = (T)->Boolean

/** 処理が成功したかを返すリスナ(suspend関数) */
typealias SuspendSwitcher<T> = suspend (T)->Boolean

/** ダイアログが自身のインスタンスを添えて呼ぶリスナ */
typealias DialogListener<T> = (value: T, f: DialogFragment)->Unit

/** ダイアログが自身のインスタンスを添えて呼ぶリスナ(suspend関数) */
typealias SuspendDialogListener<T> = suspend (value: T, f: DialogFragment)->Unit
