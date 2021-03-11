package com.suihan74.satena.models

import androidx.annotation.StringRes
import com.suihan74.satena.R
import com.sys1yagi.mastodon4j.api.entity.Status

/**
 * Mastodon投稿の公開範囲
 */
enum class TootVisibility(
    val value: Status.Visibility,
    @StringRes override val textId: Int
) : TextIdContainer {

    PUBLIC(Status.Visibility.Public, R.string.mastodon_status_visibility_public),

    UNLISTED(Status.Visibility.Unlisted, R.string.mastodon_status_visibility_unlisted),

    PRIVATE(Status.Visibility.Private, R.string.mastodon_status_visibility_private),

    DIRECT(Status.Visibility.Direct, R.string.mastodon_status_visibility_direct),
    ;

    companion object {
        fun fromOrdinal(ordinal: Int) = values().getOrElse(ordinal) { PUBLIC }
    }
}
