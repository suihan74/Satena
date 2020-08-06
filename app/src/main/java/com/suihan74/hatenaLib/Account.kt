package com.suihan74.hatenaLib

import com.google.gson.annotations.SerializedName

data class Account (
    val login : Boolean,

    val name : String,

    val rks : String,

    @SerializedName("plususer")
    val plusUser : Boolean,

    val favoriteCount : Long,

    val interestWordsHasBeenUsed : Long,

    val ignoresRegex : String,

    @SerializedName("is_oauth_twitter")
    val isOAuthTwitter : Boolean,

    @SerializedName("is_oauth_facebook")
    val isOAuthFaceBook : Boolean,

    @SerializedName("is_oauth_evernote")
    val isOAuthEvernote : Boolean
) {
    // for Gson
    private constructor() : this(false, "", "", false, 0, 0, "", false, false, false)
}

