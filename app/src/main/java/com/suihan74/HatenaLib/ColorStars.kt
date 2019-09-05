package com.suihan74.HatenaLib

import com.google.gson.annotations.SerializedName
import java.io.Serializable

class ColorStars (
    // starsは取得して未加工の状態だとcolor=yellowなので注意
    @SerializedName("stars")
    private val _stars : List<Star>,
    val color : StarColor
) : Serializable {

    private var mStars : List<Star>? = null
    val stars : List<Star>
        get() {
            if (mStars == null) {
                mStars = _stars.map {
                    Star(
                        user = it.user,
                        quote = it.quote,
                        count = it.count,
                        color = this.color
                    )
                }.toList()
            }
            return mStars!!
        }

    private var mStarsCount : Int? = null
    val starsCount : Int
        get() {
            if (mStarsCount == null) {
                mStarsCount = stars.sumBy { it.count }
            }
            return mStarsCount!!
        }
}
