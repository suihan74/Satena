package com.suihan74.HatenaLib

import java.io.Serializable

data class UserColorStarsCount (
    val red : Int,
    val green : Int,
    val blue : Int,
    val purple : Int
) : Serializable

data class UserColorStarsResponse (
    val success : Boolean,
    val Message : String,
    val result : Map<String, UserColorStarsCount>
) : Serializable
