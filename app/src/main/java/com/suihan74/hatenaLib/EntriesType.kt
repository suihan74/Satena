package com.suihan74.hatenaLib

import androidx.annotation.StringRes
import com.suihan74.satena.R


enum class EntriesType(val id: Int) {
    Hot(0),
    Recent(1);

    // for Gson
    private constructor() : this(0)

    companion object {
        fun fromId(id: Int) = values().firstOrNull { it.id == id } ?: Hot
        fun fromOrdinal(idx: Int) = values().getOrElse(idx) { Hot }
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
        fun fromId(i: Int) = values().firstOrNull { it.id == i } ?: Tag
        fun fromOrdinal(i: Int) = values().getOrElse(i) { Tag }
    }
}
