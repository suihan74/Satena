package com.suihan74.hatenaLib

import com.google.gson.annotations.SerializedName

data class TweetsAndClicks (
    @SerializedName("user_name")
    val user: String,

    val tweetUrl: String,

    val bookmarkedUrl: String,

    val count: Int
) {
    private constructor() : this("", "", "", 0)
}


data class TweetsAndClicksRequestBody (
    val user: String,
    val url: String
) {
    private constructor() : this("", "")
}
