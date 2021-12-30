package com.suihan74.utilities.extensions

infix fun Int.and(other: Boolean) : Int =
    if (other) this
    else 0

infix fun Int.or(other: Boolean) : Int =
    if (other) Int.MAX_VALUE
    else this
