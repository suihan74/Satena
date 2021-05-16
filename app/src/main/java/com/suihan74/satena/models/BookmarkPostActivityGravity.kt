package com.suihan74.satena.models

import android.view.Gravity
import com.suihan74.satena.R

/**
 * 投稿ダイアログの表示位置
 */
enum class BookmarkPostActivityGravity(
    val gravity : Int,
    override val textId : Int
) : TextIdContainer {

    DEFAULT(Gravity.NO_GRAVITY, R.string.pref_post_dialog_gravity_default),

    TOP(Gravity.TOP, R.string.pref_post_dialog_gravity_top),

    CENTER(Gravity.CENTER_VERTICAL, R.string.pref_post_dialog_gravity_center),

    BOTTOM(Gravity.BOTTOM, R.string.pref_post_dialog_gravity_bottom);

    companion object {
        fun fromOrdinal(ordinal: Int) = values().getOrElse(ordinal) { DEFAULT }
    }
}
