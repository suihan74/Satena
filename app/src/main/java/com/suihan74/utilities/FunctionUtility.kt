package com.suihan74.utilities

import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

/** 与えられた型のときだけ実行するlet */
@Suppress("UNCHECKED_CAST")
@OptIn(kotlin.contracts.ExperimentalContracts::class)
inline fun <reified T, R> Any?.letAs(crossinline block: (T) -> R) : R? {
    contract {
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
    }

    return if (this is T) block(this as T)
    else null
}

/** 与えられた型のときだけ実行するalso */
@OptIn(kotlin.contracts.ExperimentalContracts::class)
inline fun <reified T> Any?.alsoAs(crossinline block: (T) -> Unit) : T? {
    contract {
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
    }

    return if (this is T) {
        block(this as T)
        this
    }
    else null
}
