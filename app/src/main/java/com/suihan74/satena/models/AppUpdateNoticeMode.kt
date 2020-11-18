package com.suihan74.satena.models

import com.suihan74.satena.R

enum class AppUpdateNoticeMode(
    val id: Int,
    val textId: Int
) {
    NONE(0, R.string.app_update_mode_none),

    ADD_FEATURES(1, R.string.app_update_mode_add_features),

    FIX(2, R.string.app_update_mode_fix);

    companion object {
        fun fromId(id: Int) = values().firstOrNull { it.id == id } ?: FIX
        fun fromOrdinal(index: Int) = values().getOrElse(index) { FIX }
    }
}
