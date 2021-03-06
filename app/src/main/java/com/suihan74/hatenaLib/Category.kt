package com.suihan74.hatenaLib

import com.google.gson.annotations.SerializedName

enum class Category (
    val id: Int,
    val code: String
) {
    All(0, "315767106563433873"),
    General(1, "315756341902288872"),
    Social(2, "301816409282464093"),
    Economics(3, "300989576564947867"),
    Life(4, "244148959988020477"),
    Knowledge(5, "315890158150969179"),
    It(6, "261248828312298389"),
    Entertainment(7, "302115476501939948"),
    Game(8, "297347994088281699"),
    Fun(9, "302115476506048236");
//    CurrentEvents(10, "83497569613451046"),

    // for Gson
    private constructor() : this(0, "")

    companion object {
        fun fromId(id: Int) = values().firstOrNull { it.id == id } ?: All
    }
}

data class Issue (
    val name: String,

    @SerializedName("issue_id")
    val code: String,

    val imageUrl: String? = null,
    val entry: Entry? = null
) {
    // for Gson
    internal constructor() : this("", "")
}

data class CategoryEntry (
    val name: String,
    @SerializedName("category_id")
    val code: String,
    val imageUrl: String?,
    val pickupEntry: Entry?,
    val issues: List<Issue>
) {
    // for Gson
    private constructor() : this("", "", null, null, emptyList())
}

internal data class CategoryEntriesResponse (
    val categories : List<CategoryEntry>
) {
    // for Gson
    private constructor() : this(emptyList())
}

internal data class IssuesResponse (
    val issues: List<Issue>
) {
    // for Gson
    private constructor() : this(emptyList())
}
