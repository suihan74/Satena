package com.suihan74.utilities

import java.lang.reflect.Method
import kotlin.coroutines.intrinsics.suspendCoroutineUninterceptedOrReturn

/**
 * リフレクションで取得したsuspend関数を実行
 */
suspend fun Method.invokeSuspend(obj: Any, vararg args: Any?) : Any? =
    suspendCoroutineUninterceptedOrReturn { cont ->
        invoke(obj, *args, cont)
    }
