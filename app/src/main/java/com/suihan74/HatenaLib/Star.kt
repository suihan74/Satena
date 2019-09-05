package com.suihan74.HatenaLib

import com.google.gson.annotations.JsonAdapter
import com.google.gson.annotations.SerializedName
import java.io.Serializable

data class Star (
    @SerializedName("name")
    val user : String,
    val quote : String,
    @JsonAdapter(StarColorDeserializer::class)
    val color : StarColor,
    val count : Int = 1
) : Serializable {

    val userIconUrl : String = "http://cdn1.www.st-hatena.com/users/$user/profile.gif"
}

