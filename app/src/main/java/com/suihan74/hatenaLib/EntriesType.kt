package com.suihan74.hatenaLib


enum class EntriesType(val int: Int) {
    Hot(0),
    Recent(1);

    // for Gson
    private constructor() : this(0)

    companion object {
        fun fromInt(i: Int) = values().first { it.int == i }
    }
}

enum class SearchType(val int: Int) {
    Tag(0),
    Text(1);

    // for Gson
    private constructor() : this(0)

    companion object {
        fun fromInt(i: Int) = values().first { it.int == i }
    }
}
