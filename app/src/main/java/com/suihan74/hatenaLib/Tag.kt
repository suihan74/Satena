package com.suihan74.hatenaLib

data class Tag (
    val text : String,
    val index : Long,
    val count : Long,
    val timestamp : Long
) {
    // for Gson
    private constructor() : this("", 0, 0, 0)
}

data class TagsResponse (
    val count : Long,
    val status : Int,
    val tags : Map<String, Tag>
) {
    // for Gson
    private constructor() : this(0, 0, emptyMap())
}
