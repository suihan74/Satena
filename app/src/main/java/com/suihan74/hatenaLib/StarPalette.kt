package com.suihan74.hatenaLib

import com.google.gson.annotations.JsonAdapter
import com.google.gson.annotations.SerializedName

data class StarPalette(
    val token: String,

    @SerializedName("entry_uri")
    val entryUrl: String,

    @JsonAdapter(StarColorDeserializer::class)
    val color: StarColor,

    val colorStarCounts: UserColorStarsCount
)
