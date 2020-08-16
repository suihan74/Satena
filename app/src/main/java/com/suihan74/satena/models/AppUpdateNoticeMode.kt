package com.suihan74.satena.models

import com.suihan74.satena.R

enum class AppUpdateNoticeMode(
    val int: Int,
    val textId: Int
) {
    NONE(0, R.string.app_update_mode_none),

    ADD_FEATURES(1, R.string.app_update_mode_add_features),

    FIX(2, R.string.app_update_mode_fix);

    companion object {
        fun fromInt(int: Int) = values().firstOrNull { it.int == int } ?: FIX
    }
}
