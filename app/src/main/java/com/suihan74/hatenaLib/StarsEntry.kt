package com.suihan74.hatenaLib

import com.google.gson.annotations.SerializedName

data class StarsEntry (
    @SerializedName("uri")
    val url : String,
    val stars : List<Star>,
    val coloredStars : List<ColorStars>?
) {
    // for Gson
    private constructor() : this("", emptyList(), null)

    fun getStarsCount(color : StarColor) : Int = when (color) {
        StarColor.Yellow -> stars.sumOf { it.count }
        else -> coloredStars?.find { it.color == color } ?.starsCount ?: 0
    }

    /** BookmarkWithStarCount用のカラーごとのスター数リストを生成 */
    @delegate:Transient
    val counts : List<StarCount> by lazy {
        HashMap<StarColor, Int>().apply {
            for (c in StarColor.values()) { put(c, 0) }
            if (coloredStars != null) {
                for (s in coloredStars) {
                    put(s.color, this[s.color]?.plus(s.starsCount) ?: 0)
                }
            }
            put(StarColor.Yellow, stars.sumOf { it.count })
        }.map { StarCount(it.key, it.value) }
    }

    @delegate:Transient
    val totalStarsCount : Int by lazy {
        stars.sumOf { it.count } + (coloredStars?.sumOf { it.starsCount } ?: 0)
    }


    @delegate:Transient
    val allStars : List<Star> by lazy {
        val stars =
            if (coloredStars == null) stars
            else stars.plus(coloredStars.flatMap { it.stars })

        stars.groupBy { "${it.user},${it.color.name},${it.quote}" }
            .map {
                val count = it.value.sumOf { s -> s.count }
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
}

data class StarsEntries (
    val entries : List<StarsEntry>,
    val rks : String? = null
) {
    // for Gson
    private constructor() : this(emptyList())
}



