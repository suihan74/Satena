package com.suihan74.satena.models.misskey

import androidx.annotation.StringRes
import com.suihan74.misskey.entity.Visibility
import com.suihan74.satena.R
import com.suihan74.satena.models.TextIdContainer

/**
 * Misskey投稿の公開範囲
 */
enum class NoteVisibility(
    val value : Visibility,
    @StringRes override val textId: Int
) : TextIdContainer {

    Public(
        value = Visibility.Public,
        textId = R.string.misskey_status_visibility_public
    ),

    Home(
        value = Visibility.Home,
        textId = R.string.misskey_status_visibility_home
    ),

    Followers(
        value = Visibility.Followers,
        textId = R.string.misskey_status_visibility_followers
    ),

    Specified(
        value = Visibility.Specified,
        textId = R.string.misskey_status_visibility_specified
    )
}
