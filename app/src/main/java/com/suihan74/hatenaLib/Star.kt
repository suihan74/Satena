package com.suihan74.hatenaLib

import com.google.gson.annotations.JsonAdapter
import com.google.gson.annotations.SerializedName

@JsonAdapter(StarDeserializer::class)
class Star(
    @SerializedName("name") val user: String,
    val quote: String,

    @SerializedName("color")
    val color: StarColor,

    val count: Int
) {

    // for Gson
    private constructor() : this("", "", StarColor.Yellow, 0)

    @delegate:Transient
    val userIconUrl : String by lazy {
        "http://cdn1.www.st-hatena.com/users/$user/profile.gif"
    }

    override fun equals(other: Any?): Boolean {
        if (other !is Star) return false
        return color == other.color && count == other.count && quote == other.quote
    }

    override fun hashCode(): Int {
        var result = user.hashCode()
        result = 31 * result + quote.hashCode()
        result = 31 * result + count
        result = 31 * result + (color.hashCode())
        return result
    }
}

