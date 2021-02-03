package com.suihan74.hatenaLib

import com.google.gson.annotations.SerializedName

data class BookmarksEntry (
    @SerializedName("eid")
    val id : Long,
    val title : String,
    val count : Int,
    val url : String,
    val entryUrl : String,
    val screenshot : String,
    val bookmarks : List<Bookmark>
) {
    // for Gson
    internal constructor() : this(0, "", 0, "", "", "", emptyList())

    /**
     * ブクマした全ユーザーが付けたタグをその数と共に集計して返す
     */

    @delegate:Transient
    val tags : List<Pair<String, Int>> by lazy {
        bookmarks.flatMap { it.tags }
            .groupBy { it }
            .map { it.key to it.value.count() }
            .sortedByDescending { it.second }
    }
}
