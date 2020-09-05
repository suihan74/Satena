package com.suihan74.hatenaLib

data class UserColorStarsCount (
    val red : Int,
    val green : Int,
    val blue : Int,
    val purple : Int
) {
    // for Gson
    private constructor() : this(0, 0, 0, 0)

    fun has(color: StarColor) : Boolean =
        0 < when (color) {
            StarColor.Yellow -> 1
            StarColor.Red -> red
            StarColor.Green -> green
            StarColor.Blue -> blue
            StarColor.Purple -> purple
        }
}

data class UserColorStarsResponse (
    val success : Boolean,
    val Message : String,
    val result : Map<String, UserColorStarsCount>
) {
    // for Gson
    private constructor() : this(false, "", emptyMap())
}
