package com.suihan74.satena.scenes.post

import com.suihan74.hatenaLib.Entry

/**
 * 編集内容を一時保存したりやりとりするためにひとまとめにする
 */
data class BookmarkEditData (
    /** エントリ */
    val entry : Entry?,

    /** コメント */
    val comment : String,

    /** プライベート投稿する */
    val private : Boolean,

    /** Twitterに投稿する */
    val postTwitter : Boolean,

    /** Mastodonに投稿する */
    val postMastodon : Boolean,

    /** Facebookに投稿する */
    val postFacebook : Boolean
)
