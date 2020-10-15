package com.suihan74.utilities.extensions

/**
 * overflow/underflowせず、上限値を超えない Long#plus
 */
fun Long.limitedPlus(other: Long, limit: Long = Long.MAX_VALUE) : Long {
    return minOf(
        limit,
        if (other > 0 && Long.MAX_VALUE - other < this) Long.MAX_VALUE
        else if (other < 0 && Long.MIN_VALUE - other > this) Long.MIN_VALUE
        else this + other
    )
}
