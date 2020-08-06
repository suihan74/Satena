package com.suihan74.hatenaLib

import com.google.gson.annotations.SerializedName

class ColorStars (
    // starsは取得して未加工の状態だとcolor=yellowなので注意
    @SerializedName("stars")
    private val _stars : List<Star>?,
    val color : StarColor
) {

    // for Gson
    private constructor() : this(null, StarColor.Yellow)

    @delegate:Transient
    val stars : List<Star> by lazy {
        _stars?.map {
            Star(
                user = it.user,
                quote = it.quote,
                count = it.count,
                color = this.color
            )
        }?.toList() ?: emptyList()
    }

    @delegate:Transient
    val starsCount : Int by lazy { stars.sumBy { it.count } }
}
