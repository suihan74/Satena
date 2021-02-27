package com.suihan74.utilities.bindings

import android.content.Context
import android.content.res.ColorStateList
import androidx.annotation.AttrRes
import com.suihan74.utilities.extensions.getThemeColor

object Attrs {
    @JvmStatic
    fun color(context: Context, @AttrRes attrId: Int) : Int {
        return context.getThemeColor(attrId)
    }

    @JvmStatic
    fun colorTint(context: Context, @AttrRes attrId: Int) : ColorStateList {
        return ColorStateList.valueOf(color(context, attrId))
    }
}
