package com.suihan74.hatenaLib

import androidx.annotation.StringRes
import com.suihan74.satena.R


enum class EntriesType(val int: Int) {
    Hot(0),
    Recent(1);

    // for Gson
    private constructor() : this(0)

    companion object {
        fun fromInt(i: Int) = values().first { it.int == i }
    }
}

enum class SearchType(
    val id: Int,
    @StringRes val textId: Int
) {
    Tag(0,
        R.string.search_type_tag
    ),

    Text(1,
        R.string.search_type_text
    );

    // for Gson
    private constructor() : this(0, 0)

    companion object {
        fun fromInt(i: Int) = values().first { it.id == i }
    }
}
