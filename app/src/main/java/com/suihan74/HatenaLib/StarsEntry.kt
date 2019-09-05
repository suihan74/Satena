package com.suihan74.HatenaLib

import com.google.gson.annotations.SerializedName
import java.io.Serializable

data class StarsEntry (
    @SerializedName("uri")
    val url : String,
    val stars : List<Star>,
    val coloredStars : List<ColorStars>?
) : Serializable {

    fun getStarsCount(color : StarColor) : Int = when (color) {
        StarColor.Yellow -> stars.sumBy { it.count }
        else -> coloredStars?.find { it.color == color } ?.starsCount ?: 0
    }

    private var mTotalStarsCount: Int? = null
    val totalStarsCount: Int
        get() {
            if (mTotalStarsCount == null) {
                mTotalStarsCount = stars.sumBy { it.count } + (coloredStars?.sumBy { it.starsCount } ?: 0)
            }
            return mTotalStarsCount!!
        }

    private var mAllStars : List<Star>? = null
    val allStars : List<Star>
        get() {
            if (mAllStars == null) {
                val stars = if (coloredStars == null) {
                    stars
                } else {
                    stars.plus(coloredStars.flatMap { it.stars })
                }

                mAllStars = stars
                    .groupBy { "${it.user},${it.color.name}" }
                    .map {
                        val count = it.value.sumBy { s -> s.count }
                        val star = it.value[0]
                        Star(
                            user = star.user,
                            quote = star.quote,
                            count = count,
                            color = star.color
                        )
                    }
                    .reversed()
            }
            return mAllStars!!
        }
}

data class StarsEntries (
    val entries : List<StarsEntry>,
    val rks : String? = null
) : Serializable



