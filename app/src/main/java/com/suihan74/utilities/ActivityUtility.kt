package com.suihan74.utilities

import android.content.Context
import android.util.TypedValue

fun Context.getThemeColor(attrId: Int) : Int {
    val outValue = TypedValue()
    theme.resolveAttribute(attrId, outValue, true)
    return outValue.data
}
