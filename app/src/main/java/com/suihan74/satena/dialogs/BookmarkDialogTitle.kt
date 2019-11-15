package com.suihan74.satena.dialogs

import android.view.View
import android.widget.ImageView
import android.widget.TextView
import com.bumptech.glide.Glide
import com.suihan74.HatenaLib.Bookmark
import com.suihan74.HatenaLib.HatenaClient
import com.suihan74.satena.R

fun View.setCustomTitle(bookmark: Bookmark) =
    this.setCustomTitle(bookmark.user, bookmark.comment)

fun View.setCustomTitle(user: String, comment: String = "") = this.apply {
    findViewById<TextView>(R.id.user_name)?.text = user
    findViewById<TextView>(R.id.bookmark_comment)?.text = comment
    findViewById<ImageView>(R.id.user_icon)?.let {
        Glide.with(this@setCustomTitle.context)
            .load(HatenaClient.getUserIconUrl(user))
            .into(it)
    }
}
