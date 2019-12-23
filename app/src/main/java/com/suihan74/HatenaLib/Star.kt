package com.suihan74.HatenaLib

import com.google.gson.annotations.JsonAdapter
import com.google.gson.annotations.SerializedName
import java.io.Serializable

class Star(
    @SerializedName("name") val user: String,
    val quote: String,
    color: StarColor?,
    val count: Int
) : Serializable {

    @SerializedName("color")
    @JsonAdapter(StarColorDeserializer::class)
    private val mColor : StarColor? = color ?: StarColor.Yellow

    val color
        get() = mColor ?: StarColor.Yellow

    val userIconUrl : String
        get() = "http://cdn1.www.st-hatena.com/users/$user/profile.gif"

    override fun equals(other: Any?): Boolean {
        if (other !is Star) return false
        return color == other.color && count == other.count && quote == other.quote
    }
}

