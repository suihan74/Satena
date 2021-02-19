package com.suihan74.satena.models

import android.view.Gravity
import androidx.annotation.StringRes
import com.suihan74.satena.R

enum class GravitySetting(
    val gravity : Int,
    @StringRes override val textId : Int
) : TextIdContainer {

    START(Gravity.START, R.string.gravity_start),

    END(Gravity.END, R.string.gravity_end),
    ;

    companion object {
        fun fromGravity(gravity: Int) = values().firstOrNull { it.gravity == gravity } ?: START
    }
}
