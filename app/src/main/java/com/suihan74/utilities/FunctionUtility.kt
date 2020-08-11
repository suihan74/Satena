package com.suihan74.utilities

import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

/** 与えられた型のときだけ実行するlet */
@Suppress("UNCHECKED_CAST")
@OptIn(kotlin.contracts.ExperimentalContracts::class)
inline fun <T, R> Any?.letAs(crossinline block: (T) -> R) : R? {
    contract {
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
    }
    val obj = this as? T
    return if (obj == null) null else block(obj)
}

/** 与えられた型のときだけ実行するalso */
@Suppress("UNCHECKED_CAST")
@OptIn(kotlin.contracts.ExperimentalContracts::class)
inline fun <T> Any?.alsoAs(crossinline block: (T) -> Unit) : T? {
    contract {
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
    }
    val obj = this as? T
    if (obj != null) block(obj)

    return obj
}
