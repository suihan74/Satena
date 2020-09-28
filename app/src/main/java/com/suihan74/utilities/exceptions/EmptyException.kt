package com.suihan74.utilities.exceptions

/** 空だと困るものが空だった時の例外 */
class EmptyException(msg: String? = null) : Throwable(msg)
